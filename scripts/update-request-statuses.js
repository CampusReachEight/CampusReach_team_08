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
const { Timestamp, FieldPath } = require('firebase-admin/firestore');

// Collection and batching configuration
const REQUESTS_COLLECTION_PATH = 'requests';
const MAX_BATCH_SIZE = 500; // Firestore limit per batch
const PAGE_SIZE = 1000; // pagination for reading
const PREVIEW_LIMIT = 10; // max transitions preview lines
const MAX_RETRIES = 3; // retry attempts for transient errors
const BACKOFF_BASE_MS = 1000; // base backoff delay in ms

// The credentials file the workflow writes to disk before execution
const SERVICE_ACCOUNT_FILE = 'service-account.json';

// Simple CLI args parsing for flags like --dry-run
const argv = process.argv.slice(2);
const DRY_RUN = argv.includes('--dry-run') || process.env.DRY_RUN === 'true';

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
    const usingEmulator = !!process.env.FIRESTORE_EMULATOR_HOST;
    if (usingEmulator) {
      const projectId = process.env.FIREBASE_PROJECT_ID || 'demo-project';
      admin.initializeApp({ projectId });
    } else {
      const sa = loadServiceAccount();
      const projectId = process.env.FIREBASE_PROJECT_ID || sa.project_id;
      admin.initializeApp({ credential: admin.credential.cert(sa), projectId });
    }
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

// Retry helper with exponential backoff and jitter
async function retryWithBackoff(fn, { maxRetries = MAX_RETRIES, baseMs = BACKOFF_BASE_MS } = {}) {
  let lastError;
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      if (attempt === maxRetries - 1) break;
      const jitter = Math.floor(Math.random() * baseMs);
      const delay = Math.pow(2, attempt) * baseMs + jitter;
      console.warn(`Operation failed (attempt ${attempt + 1}/${maxRetries}): ${error.message}. Retrying in ${delay}ms...`);
      await new Promise((resolve) => setTimeout(resolve, delay));
    }
  }
  throw lastError;
}

// Commits updates in batches of at most MAX_BATCH_SIZE operations with retry and
// improved error diagnostics. Returns an array containing the size of each committed batch.
async function batchUpdateDocuments(db, updates) {
  let batch = db.batch();
  let opsInBatch = 0;
  const commitSizes = [];
  let currentBatchStartIdx = 0;

  const commitCurrent = async () => {
    if (opsInBatch === 0) return;
    try {
      await retryWithBackoff(() => batch.commit());
      commitSizes.push(opsInBatch);
    } catch (error) {
      const failedIds = updates
        .slice(currentBatchStartIdx, currentBatchStartIdx + opsInBatch)
        .map((u) => u.id);
      console.error(`Batch commit failed for ${opsInBatch} ops. Failed document IDs:`, failedIds);
      throw error;
    } finally {
      batch = db.batch();
      opsInBatch = 0;
      currentBatchStartIdx += commitSizes[commitSizes.length - 1] || 0;
    }
  };

  for (const update of updates) {
    batch.update(update.ref, update.data);
    opsInBatch += 1;

    // Commit and reset the batch once we reach the write limit.
    if (opsInBatch === MAX_BATCH_SIZE) {
      await commitCurrent();
    }
  }

  // Commit any remaining writes.
  if (opsInBatch > 0) {
    await commitCurrent();
  }

  return commitSizes;
}

// Prints a concise summary including counts per transition and a small preview.
function printSummary({ total, updated, transitions, preview = [], dryRun = false, batchSizes = [] }) {
  console.log('\nUPDATE SUMMARY');
  console.log('----------------');
  console.log(`Total documents scanned: ${total}`);
  if (dryRun) {
    console.log(`Total documents that WOULD be updated (dry-run): ${updated}`);
  } else {
    console.log(`Total documents updated: ${updated}`);
  }
  Object.entries(transitions).forEach(([transition, count]) => {
    console.log(`  ${transition}: ${count}`);
  });
  if (batchSizes.length) {
    console.log(`Committed ${batchSizes.length} batches: [${batchSizes.join(', ')}]`);
  }
  if (preview.length) {
    console.log('\nPreview of transitions:');
    preview.forEach((item, idx) => {
      console.log(`  ${idx + 1}. ${item.id}: ${item.from} -> ${item.to}`);
    });
  }
}

// Async generator to iterate over a collection in pages to avoid loading everything in memory.
async function* fetchInBatches(collectionRef, pageSize = PAGE_SIZE) {
  let lastDoc = null;
  // Order by document ID for stable pagination
  while (true) {
    let query = collectionRef.orderBy(FieldPath.documentId()).limit(pageSize);
    if (lastDoc) query = query.startAfter(lastDoc);
    const snapshot = await query.get();
    if (snapshot.empty) break;
    yield snapshot;
    lastDoc = snapshot.docs[snapshot.docs.length - 1];
  }
}

// Orchestrates the full update flow with pagination and optional dry-run
async function updateRequestStatuses() {
  console.log(`[${new Date().toISOString()}] Starting Firebase Request Status Update${DRY_RUN ? ' (dry-run)' : ''}`);
  const db = initFirebase();

  const collectionRef = db.collection(REQUESTS_COLLECTION_PATH);

  let totalScanned = 0;
  let totalUpdated = 0;
  const transitions = {
    'CANCELLED->ARCHIVED': 0,
    'OPEN->IN_PROGRESS': 0,
    'IN_PROGRESS->ARCHIVED': 0,
  };
  const preview = [];
  const allBatchSizes = [];

  const nowMs = Date.now();

  for await (const snapshot of fetchInBatches(collectionRef)) {
    totalScanned += snapshot.size;

    const updatesBuffer = [];

    snapshot.forEach((doc) => {
      const data = doc.data();
      const newStatus = determineNewStatus(data, nowMs);
      if (!newStatus || newStatus === data.status) return;

      const transitionKey = `${data.status}->${newStatus}`;
      if (transitions[transitionKey] !== undefined) {
        transitions[transitionKey] += 1;
      } else {
        // Track unexpected transitions for visibility
        transitions[transitionKey] = 1;
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
      updatesBuffer.push(updateEntry);

      // Keep a small preview list for logs.
      if (preview.length < PREVIEW_LIMIT) {
        preview.push({ id: updateEntry.id, from: updateEntry.from, to: updateEntry.to });
      }
    });

    console.log(`Page scheduled ${updatesBuffer.length} documents for update`);

    if (!DRY_RUN && updatesBuffer.length > 0) {
      const sizes = await batchUpdateDocuments(db, updatesBuffer);
      sizes.forEach((s) => allBatchSizes.push(s));
    }

    totalUpdated += updatesBuffer.length;
  }

  // Log quick preview of what's going to change if there are more than preview limit
  if (totalUpdated > PREVIEW_LIMIT) {
    console.log(`  ...and ${totalUpdated - PREVIEW_LIMIT} more`);
  }

  printSummary({
    total: totalScanned,
    updated: totalUpdated,
    transitions,
    preview,
    dryRun: DRY_RUN,
    batchSizes: allBatchSizes,
  });
}

// Run the updater only when executed directly (not when imported by tests)
if (require.main === module) {
  updateRequestStatuses()
    .then(() => {
      console.log('Status update completed successfully');
      process.exit(0);
    })
    .catch((error) => {
      console.error('Status update failed:', error);
      process.exit(1);
    });
}

module.exports = {
  updateRequestStatuses,
  determineNewStatus,
  // Expose for tests if needed
  _internal: {
    initFirebase,
  },
};
