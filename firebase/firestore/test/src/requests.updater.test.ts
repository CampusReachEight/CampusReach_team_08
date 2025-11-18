import { describe, test, beforeAll, afterAll, expect } from 'vitest';
import * as admin from 'firebase-admin';
import updater from '../../../../index.js';
import { setup } from './setup.js';

const { updateRequestStatuses } = updater as { updateRequestStatuses: () => Promise<void> };

// Integration test of the updater script against the emulator

describe('Requests updater (emulator)', () => {
  let env: Awaited<ReturnType<typeof setup>>;
  const projectId = 'demo-unit-tests';
  const collection = 'requests';

  const initAdmin = () => {
    if (admin.apps.length === 0) {
      admin.initializeApp({ projectId });
    }
    return admin.firestore();
  };

  const db = initAdmin();

  beforeAll(async () => {
    env = await setup();
    await env.clearFirestore();
    await env.withSecurityRulesDisabled(async (ctx) => {
      const colRef = ctx.firestore().collection(collection);
      // OPEN should become IN_PROGRESS
      await colRef.doc('open-1').set({
        status: 'OPEN',
        startTimeStamp: admin.firestore.Timestamp.fromMillis(Date.now() - 60_000),
        expirationTime: admin.firestore.Timestamp.fromMillis(Date.now() + 60_000),
      });
      // IN_PROGRESS should become ARCHIVED
      await colRef.doc('prog-1').set({
        status: 'IN_PROGRESS',
        startTimeStamp: admin.firestore.Timestamp.fromMillis(Date.now() - 120_000),
        expirationTime: admin.firestore.Timestamp.fromMillis(Date.now() - 10_000),
      });
      // CANCELLED should become ARCHIVED
      await colRef.doc('cancel-1').set({
        status: 'CANCELLED',
        startTimeStamp: admin.firestore.Timestamp.fromMillis(Date.now() - 120_000),
        expirationTime: admin.firestore.Timestamp.fromMillis(Date.now() + 120_000),
      });
    });
  });

  afterAll(async () => {
    await env.clearFirestore();
    await env.cleanup();
    await admin.app().delete().catch(() => void 0);
  });

  test('applies expected transitions', async () => {
    await updateRequestStatuses();

    const [openDoc, inProgDoc, cancelDoc] = await Promise.all([
      db.collection(collection).doc('open-1').get(),
      db.collection(collection).doc('prog-1').get(),
      db.collection(collection).doc('cancel-1').get(),
    ]);

    expect(openDoc.data()?.status).toBe('IN_PROGRESS');
    expect(inProgDoc.data()?.status).toBe('ARCHIVED');
    expect(cancelDoc.data()?.status).toBe('ARCHIVED');
  });
});

