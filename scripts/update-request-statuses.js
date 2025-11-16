#!/usr/bin/env node
// -----------------------------------------------------------------------------
// Firebase Firestore Request Status Updater
// -----------------------------------------------------------------------------
// What this script does
// - Connects to Firestore using a service account JSON file named
//   "service-account.json" located at the repository root.
// - Reads all documents in the "requests" collection.
// - Applies time-based status transitions:
//     1) CANCELLED -> ARCHIVED (immediate)
//     2) OPEN -> IN_PROGRESS (when now is in [startTimeStamp, expirationTime))
//     3) IN_PROGRESS -> ARCHIVED (when now >= expirationTime)
// - Updates documents in batches of up to 500 writes for efficiency.
// - Logs a concise summary and exits with 0 on success or 1 on failure.
//
// How credentials are provided
// - In CI, GitHub Actions decodes the Base64 secret FIREBASE_SERVICE_ACCOUNT
//   into a file named "service-account.json" before running this script.
// - Locally, you can place a valid Firebase service account JSON at the repo
//   root with the same filename to run the script.
//
// Data assumptions
// - requests.status is one of: OPEN, IN_PROGRESS, ARCHIVED, COMPLETED, CANCELLED
// - requests.startTimeStamp and requests.expirationTime are Firestore Timestamps
//   (or equivalent timestamp-like objects); null/undefined are tolerated.
// - The script sets an "updatedAt" field using serverTimestamp when updating.
// -----------------------------------------------------------------------------

const fs = require('fs');
const admin = require('firebase-admin');
const { Timestamp } = require('firebase-admin/firestore');

// Collection and batching configuration
const REQUESTS_COLLECTION_PATH = 'requests';
const MAX_BATCH_SIZE = 500; // Firestore limit per batch

// The credentials file the workflow writes to disk before execution
const SERVICE_ACCOUNT_FILE = 'service-account.json';

// Loads and parses the service account JSON from disk. Throws on failure.
function loadServiceAccount() {
  try {
    const jsonString = fs.readFileSync(SERVICE_ACCOUNT_FILE, 'utf8');
    return JSON.parse(jsonString);
  } catch (err) {
    throw new Error(`Failed to load service account from ${SERVICE_ACCOUNT_FILE}: ${err.message}`);
  }
}

// Initializes Firebase Admin SDK once and returns a Firestore instance.
function initFirebase() {
  if (admin.apps.length === 0) {
    admin.initializeApp({ credential: admin.credential.cert(loadServiceAccount()) });
  }
  return admin.firestore();
}

// Normalizes various timestamp representations into milliseconds since epoch.
// Supports numbers, JS Date, Firestore Timestamp instances, and plain objects
// that look like Firestore proto timestamps ({ _seconds, _nanoseconds }).
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

// Applies the status transition rules for a single document. Returns a new
// status string or null if no change is needed.
function determineNewStatus(docData, nowMs) {
  const status = docData.status;
  const startMs = timestampToMillis(docData.startTimeStamp);
  const expirationMs = timestampToMillis(docData.expirationTime);

  // Rule 1: CANCELLED -> ARCHIVED (immediate)
  if (status === 'CANCELLED') return 'ARCHIVED';

  // Rule 2: OPEN -> IN_PROGRESS when now in [start, expiration)
  if (status === 'OPEN') {
    if (startMs && expirationMs && nowMs >= startMs && nowMs < expirationMs) {
      return 'IN_PROGRESS';
    }
    return null;
  }

  // Rule 3: IN_PROGRESS -> ARCHIVED when now >= expiration
  if (status === 'IN_PROGRESS') {
    if (expirationMs && nowMs >= expirationMs) {
      return 'ARCHIVED';
    }
    return null;
  }

  // No transition for other statuses (e.g., COMPLETED, ARCHIVED)
  return null;
}

// Commits updates in batches of at most MAX_BATCH_SIZE operations.
// Returns an array containing the size of each committed batch.
async function batchUpdateDocuments(db, updates) {
  let batch = db.batch();
  let opsInBatch = 0;
  const commitSizes = [];

  for (const update of updates) {
    batch.update(update.ref, update.data);
    opsInBatch += 1;

    // Commit and reset the batch once we reach the write limit.
    if (opsInBatch === MAX_BATCH_SIZE) {
      await batch.commit();
      commitSizes.push(opsInBatch);
      batch = db.batch();
      opsInBatch = 0;
    }
  }

  // Commit any remaining writes.
  if (opsInBatch > 0) {
    await batch.commit();
    commitSizes.push(opsInBatch);
  }

  return commitSizes;
}

// Prints a concise summary including counts per transition and a small preview.
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

// Orchestrates the full update flow:
// 1) Initialize Firestore.
// 2) Read all request documents.
// 3) Compute desired transitions relative to now.
// 4) Batch update changed documents with server timestamps.
// 5) Log batch commits and a final summary.
async function updateRequestStatuses() {
  console.log(`[${new Date().toISOString()}] Starting Firebase Request Status Update`);
  const db = initFirebase();

  // Fetch all documents from the target collection.
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

  // Determine which docs need updates and prepare their new values.
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
        // Use server-assigned timestamp to reflect the update time consistently.
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      id: doc.id,
      from: data.status,
      to: newStatus,
    };
    updates.push(updateEntry);

    // Keep a small preview list for logs.
    if (preview.length < 10) {
      preview.push({ id: updateEntry.id, from: updateEntry.from, to: updateEntry.to });
    }
  });

  // Log a quick preview of what's going to change.
  console.log(`Scheduled ${updates.length} documents for update`);
  updates.slice(0, 10).forEach((update, idx) => {
    console.log(`  [${idx + 1}] ${update.id}: ${update.from} -> ${update.to}`);
  });
  if (updates.length > 10) {
    console.log(`  ...and ${updates.length - 10} more`);
  }

  // If nothing to update, print summary and exit early.
  if (updates.length === 0) {
    printSummary({ total: snapshot.size, updated: 0, transitions, preview: [] });
    return;
  }

  // Execute batched updates and log commit sizes.
  const batchSizes = await batchUpdateDocuments(db, updates);
  batchSizes.forEach((size, index) => {
    console.log(`Batch ${index + 1} committed: ${size} documents updated`);
  });

  // Final rollup of what happened during this run.
  printSummary({ total: snapshot.size, updated: updates.length, transitions, preview });
}

// Run the updater and set a process exit code for CI consumption.
updateRequestStatuses()
  .then(() => {
    console.log('Status update completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('Status update failed:', error);
    process.exit(1);
  });
