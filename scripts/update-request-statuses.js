#!/usr/bin/env node
const fs = require('fs');
const admin = require('firebase-admin');
const { Timestamp } = require('firebase-admin/firestore');

const REQUESTS_COLLECTION_PATH = 'requests';
const MAX_BATCH_SIZE = 500;
const SERVICE_ACCOUNT_FILE = 'service-account.json';

function loadServiceAccount() {
  try {
    const jsonString = fs.readFileSync(SERVICE_ACCOUNT_FILE, 'utf8');
    return JSON.parse(jsonString);
  } catch (err) {
    throw new Error(`Failed to load service account from ${SERVICE_ACCOUNT_FILE}: ${err.message}`);
  }
}

function initFirebase() {
  if (admin.apps.length === 0) {
    admin.initializeApp({ credential: admin.credential.cert(loadServiceAccount()) });
  }
  return admin.firestore();
}

function timestampToMillis(value) {
  if (!value) return null;
  if (typeof value === 'number') return value;
  if (value instanceof Date) return value.getTime();
  if (value instanceof Timestamp) return value.toMillis();
  if (value && value._seconds !== undefined && value._nanoseconds !== undefined) {
    return value._seconds * 1000 + Math.floor(value._nanoseconds / 1e6);
  }
  return null;
}

function determineNewStatus(docData, nowMs) {
  const status = docData.status;
  const startMs = timestampToMillis(docData.startTimeStamp);
  const expirationMs = timestampToMillis(docData.expirationTime);

  if (status === 'CANCELLED') return 'ARCHIVED';

  if (status === 'OPEN') {
    if (startMs && expirationMs && nowMs >= startMs && nowMs < expirationMs) {
      return 'IN_PROGRESS';
    }
    return null;
  }

  if (status === 'IN_PROGRESS') {
    if (expirationMs && nowMs >= expirationMs) {
      return 'ARCHIVED';
    }
    return null;
  }

  return null;
}

async function batchUpdateDocuments(db, updates) {
  let batch = db.batch();
  let opsInBatch = 0;
  const commitSizes = [];

  for (const update of updates) {
    batch.update(update.ref, update.data);
    opsInBatch += 1;

    if (opsInBatch === MAX_BATCH_SIZE) {
      await batch.commit();
      commitSizes.push(opsInBatch);
      batch = db.batch();
      opsInBatch = 0;
    }
  }

  if (opsInBatch > 0) {
    await batch.commit();
    commitSizes.push(opsInBatch);
  }

  return commitSizes;
}

function printSummary({ total, updated, transitions, preview = [] }) {
  console.log('\nUPDATE SUMMARY');
  console.log('----------------');
  console.log(`Total documents scanned: ${total}`);
  console.log(`Total documents updated: ${updated}`);
  Object.entries(transitions).forEach(([transition, count]) => {
    console.log(`  ${transition}: ${count}`);
  });
  if (preview.length) {
    console.log('\nPreview of transitions:');
    preview.forEach((item, idx) => {
      console.log(`  ${idx + 1}. ${item.id}: ${item.from} -> ${item.to}`);
    });
  }
}

async function updateRequestStatuses() {
  console.log(`[${new Date().toISOString()}] Starting Firebase Request Status Update`);
  const db = initFirebase();
  const snapshot = await db.collection(REQUESTS_COLLECTION_PATH).get();
  console.log(`Found ${snapshot.size} documents to process`);

  const nowMs = Date.now();
  const updates = [];
  const transitions = {
    'CANCELLED->ARCHIVED': 0,
    'OPEN->IN_PROGRESS': 0,
    'IN_PROGRESS->ARCHIVED': 0,
  };
  const preview = [];

  snapshot.forEach((doc) => {
    const data = doc.data();
    const newStatus = determineNewStatus(data, nowMs);
    if (!newStatus || newStatus === data.status) return;

    const transitionKey = `${data.status}->${newStatus}`;
    if (transitions[transitionKey] !== undefined) {
      transitions[transitionKey] += 1;
    }

    const updateEntry = {
      ref: doc.ref,
      data: {
        status: newStatus,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      id: doc.id,
      from: data.status,
      to: newStatus,
    };
    updates.push(updateEntry);
    if (preview.length < 10) {
      preview.push({ id: updateEntry.id, from: updateEntry.from, to: updateEntry.to });
    }
  });

  console.log(`Scheduled ${updates.length} documents for update`);
  updates.slice(0, 10).forEach((update, idx) => {
    console.log(`  [${idx + 1}] ${update.id}: ${update.from} -> ${update.to}`);
  });
  if (updates.length > 10) {
    console.log(`  ...and ${updates.length - 10} more`);
  }

  if (updates.length === 0) {
    printSummary({ total: snapshot.size, updated: 0, transitions, preview: [] });
    return;
  }

  const batchSizes = await batchUpdateDocuments(db, updates);
  batchSizes.forEach((size, index) => {
    console.log(`Batch ${index + 1} committed: ${size} documents updated`);
  });

  printSummary({ total: snapshot.size, updated: updates.length, transitions, preview });
}

updateRequestStatuses()
  .then(() => {
    console.log('Status update completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('Status update failed:', error);
    process.exit(1);
  });
