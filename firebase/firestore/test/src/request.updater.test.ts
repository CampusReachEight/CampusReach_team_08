import { describe, test, beforeAll, afterAll, expect } from 'vitest';
import { setup } from './setup.js';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { unlink, readFile, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { spawn } from 'node:child_process';
import { doc, setDoc, getDoc } from 'firebase/firestore';
import { sampleRequestDocs } from './test-data-samples.js';

// This test simulates the CI-scheduled job by:
// - Invoking the updater script as a CLI process with FIRESTORE_EMULATOR_HOST
// - Verifying the expected status transitions in the emulator DB

describe('Scheduled updater script (CLI) against emulator', () => {
  let env: Awaited<ReturnType<typeof setup>>;

  // Resolve repo root from this test file location
  const here = path.dirname(fileURLToPath(new URL(import.meta.url)));
  const repoRoot = path.resolve(here, '../../../../');
  const serviceAccountDst = path.join(repoRoot, 'service-account.json');
  const updaterScript = path.join(repoRoot, 'scripts', 'update-request-statuses.js');

  const collection = 'requests';

  const runUpdaterCli = () =>
    new Promise<void>((resolve, reject) => {
      const child = spawn('node', [updaterScript], {
        cwd: repoRoot,
        env: {
          ...process.env,
          FIRESTORE_EMULATOR_HOST: '127.0.0.1:8080',
          FIREBASE_PROJECT_ID: 'demo-unit-tests',
          // Optional: uncomment to preview without writes
          // DRY_RUN: 'true',
        },
        stdio: 'inherit',
      });
      child.on('error', reject);
      child.on('exit', (code) => {
        if (code === 0) resolve();
        else reject(new Error(`Updater exited with code ${code}`));
      });
    });

  beforeAll(async () => {
    env = await setup();

    // Ensure we start clean
    await env.clearFirestore();

    // No need to copy credentials: CI creates service-account.json at repo root when needed.

    // Seed diverse data: include stable samples + targeted transition docs
    await env.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();

      // Seed the sample request docs using doc IDs equal to their requestId
      await Promise.all(
        sampleRequestDocs.map((r) => setDoc(doc(db, collection, String(r.requestId)), {
          requestId: r.requestId,
          title: r.title ?? 'n/a',
          description: r.description ?? '',
          requestType: Array.isArray(r.requestType) ? r.requestType : [],
          location: r.location ?? { latitude: 0, longitude: 0, name: '' },
          locationName: r.locationName ?? (r.location?.name || ''),
          status: r.status ?? 'OPEN',
          startTimeStamp: r.startTimeStamp instanceof Date ? r.startTimeStamp : new Date(),
          expirationTime: r.expirationTime instanceof Date ? r.expirationTime : new Date(Date.now() + 3600000),
          people: Array.isArray(r.people) ? r.people : [],
          tags: Array.isArray(r.tags) ? r.tags : [],
          creatorId: r.creatorId ?? 'seed',
        }))
      );

      const now = Date.now();

      // OPEN -> IN_PROGRESS: window contains now
      await setDoc(doc(db, collection, 'open-window'), {
        status: 'OPEN',
        startTimeStamp: new Date(now - 60_000),
        expirationTime: new Date(now + 60_000),
      });

      // IN_PROGRESS -> ARCHIVED: expiration in the past
      await setDoc(doc(db, collection, 'inprog-expired'), {
        status: 'IN_PROGRESS',
        startTimeStamp: new Date(now - 120_000),
        expirationTime: new Date(now - 10_000),
      });

      // CANCELLED -> ARCHIVED: immediate
      await setDoc(doc(db, collection, 'cancelled-any'), {
        status: 'CANCELLED',
        startTimeStamp: new Date(now - 120_000),
        expirationTime: new Date(now + 120_000),
      });

      // Control: COMPLETED should remain unchanged
      await setDoc(doc(db, collection, 'completed-stays'), {
        status: 'COMPLETED',
        startTimeStamp: new Date(now - 3600_000),
        expirationTime: new Date(now - 1800_000),
      });
    });
  });

  afterAll(async () => {
    await env.clearFirestore();
    await env.cleanup();
    // Do not remove service-account.json in CI; workflow handles cleanup.
    if (!process.env.CI && existsSync(serviceAccountDst)) {
      await unlink(serviceAccountDst).catch(() => void 0);
    }
  });

  test('applies expected transitions via CLI invocation', async () => {
    await runUpdaterCli();
    // small delay to ensure emulator has applied writes
    await new Promise((r) => setTimeout(r, 100));

    // Verify transitions
    const db = env.authenticatedContext('verifier').firestore();

    const getStatus = async (id: string): Promise<string | undefined> => {
      const ref = doc(db, `${collection}/${id}`);
      for (let i = 0; i < 5; i++) {
        const snap = await getDoc(ref);
        const s = snap.data()?.status as string | undefined;
        if (s) return s;
        await new Promise((r) => setTimeout(r, 100));
      }
      const final = await getDoc(ref);
      return final.data()?.status as string | undefined;
    };

    await expect(getStatus('open-window')).resolves.toBe('IN_PROGRESS');
    await expect(getStatus('inprog-expired')).resolves.toBe('ARCHIVED');
    await expect(getStatus('cancelled-any')).resolves.toBe('ARCHIVED');
    await expect(getStatus('completed-stays')).resolves.toBe('COMPLETED');
  });
});
