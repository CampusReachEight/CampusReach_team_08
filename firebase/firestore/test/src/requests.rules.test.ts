import { describe, test, afterAll, beforeAll } from 'vitest';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, getDocs, setDoc, updateDoc } from 'firebase/firestore';
import { setup } from './setup.js';

// Tests for the 'requests' collection based on firestore.rules

describe('Firestore rules tests - requests', () => {
  let env: Awaited<ReturnType<typeof setup>>;
  const creatorId = 'creator-1';
  const otherUserId = 'user-2';

  const collectionName = 'requests';
  const seedId = 'req-1';

  beforeAll(async () => {
    env = await setup();
    await env.clearFirestore();
    await env.withSecurityRulesDisabled(async (ctx) => {
      await ctx.firestore().collection(collectionName).doc(seedId).set({
        creatorId,
        people: [],
        status: 'OPEN',
        startTimeStamp: new Date(),
        expirationTime: new Date(Date.now() + 3600_000),
      });
    });
  });

  afterAll(async () => {
    await env.clearFirestore();
    await env.cleanup();
  });

  test('Anyone can read requests collection and specific doc', async () => {
    const me = env.authenticatedContext(creatorId);
    const other = env.authenticatedContext(otherUserId);

    await assertSucceeds(getDocs(me.firestore().collection(collectionName)));
    await assertSucceeds(getDoc(doc(me.firestore(), `${collectionName}/${seedId}`)));
    await assertSucceeds(getDocs(other.firestore().collection(collectionName)));
    await assertSucceeds(getDoc(doc(other.firestore(), `${collectionName}/${seedId}`)));
  });

  test('Creator can create a request (creatorId must match auth uid)', async () => {
    const me = env.authenticatedContext(creatorId);

    const newId = 'req-2';
    await assertSucceeds(
      setDoc(doc(me.firestore(), `${collectionName}/${newId}`), {
        creatorId,
        people: [],
        status: 'OPEN',
      })
    );

    // Other user cannot set creatorId to someone else
    const other = env.authenticatedContext(otherUserId);
    const otherId = 'req-3';
    await assertFails(
      setDoc(doc(other.firestore(), `${collectionName}/${otherId}`), {
        creatorId,
        people: [],
        status: 'OPEN',
      })
    );
  });

  test('Creator can update any field; other users can only add/remove their UID in people', async () => {
    const me = env.authenticatedContext(creatorId);
    const myDb = me.firestore();

    // Creator updates freely
    await assertSucceeds(updateDoc(doc(myDb, `${collectionName}/${seedId}`), { status: 'IN_PROGRESS' }));

    // Other user can add themselves to people by setting full array (matches repository behavior)
    const other = env.authenticatedContext(otherUserId);
    await assertSucceeds(updateDoc(doc(other.firestore(), `${collectionName}/${seedId}`), { people: [otherUserId] }));

    // Other user cannot update unrelated fields
    await assertFails(updateDoc(doc(other.firestore(), `${collectionName}/${seedId}`), { status: 'ARCHIVED' }));
  });

  test('Delete: only creator can delete', async () => {
    const me = env.authenticatedContext(creatorId);
    const other = env.authenticatedContext(otherUserId);

    const docRefCreator = doc(me.firestore(), `${collectionName}/${seedId}`);
    const docRefOther = doc(other.firestore(), `${collectionName}/${seedId}`);

    await assertFails(deleteDoc(docRefOther));
    await assertSucceeds(deleteDoc(docRefCreator));
  });
});
