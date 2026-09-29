/**
 * Firebase Admin Migration Script: Populate 'phone_hash' in 'users' collection
 *
 * This script connects to Cloud Firestore via the Firebase Admin SDK,
 * normalizes all user phone numbers to E.164 format, computes a 16-character
 * truncated SHA-256 hash, and updates every user document with the 'phone_hash' field.
 *
 * Usage:
 *   1. Download your service account key JSON from Firebase Console:
 *      Project Settings > Service Accounts > Generate New Private Key
 *   2. Save the key as 'serviceAccountKey.json' in this directory.
 *   3. Run:
 *      npm install firebase-admin
 *      node migrate_phone_hash.js
 */

const { initializeApp, cert, applicationDefault } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

let serviceAccountPath = path.join(__dirname, 'serviceAccountKey.json');

if (!fs.existsSync(serviceAccountPath)) {
  const files = fs.readdirSync(__dirname);
  const found = files.find(f => (f.includes('firebase-adminsdk') || f.includes('service_account')) && f.endsWith('.json'));
  if (found) {
    serviceAccountPath = path.join(__dirname, found);
    console.log(`Auto-detected service account key: ${found}`);
  }
}

let app;
if (!fs.existsSync(serviceAccountPath)) {
  console.log('Using default Google Application Credentials...');
  app = initializeApp();
} else {
  const serviceAccount = require(serviceAccountPath);
  console.log(`Initializing Firebase Admin with service account project: ${serviceAccount.project_id}`);
  app = initializeApp({
    credential: cert(serviceAccount)
  });
}

const db = getFirestore(app);

/**
 * Normalizes phone number to E.164 standard (+ followed by country code and digits).
 * Defaulting to +91 for 10-digit national numbers.
 */
function normalizeToE164(rawPhone, defaultCountryCode = '+91') {
  if (!rawPhone) return '';
  const trimmed = String(rawPhone).trim();
  const digits = trimmed.replace(/\D/g, '');
  if (!digits) return '';

  const defaultCc = defaultCountryCode.replace(/\D/g, '') || '91';

  if (trimmed.startsWith('+')) {
    return `+${digits}`;
  } else if (digits.startsWith('00') && digits.length > 2) {
    return `+${digits.substring(2)}`;
  } else if (digits.length === 10) {
    return `+${defaultCc}${digits}`;
  } else if (digits.length === 11 && digits.startsWith('0')) {
    return `+${defaultCc}${digits.substring(1)}`;
  } else if (digits.length === 12 && digits.startsWith('91')) {
    return `+${digits}`;
  } else if (digits.length === 11 && digits.startsWith('1')) {
    return `+${digits}`;
  } else if (digits.length > 10) {
    return `+${digits}`;
  } else {
    return `+${defaultCc}${digits}`;
  }
}

/**
 * Generates a 16-character truncated SHA-256 hash of the E.164 normalized phone number.
 */
function generate16CharHash(rawPhone, defaultCountryCode = '+91') {
  const e164 = normalizeToE164(rawPhone, defaultCountryCode);
  if (!e164) return '';
  const hash = crypto.createHash('sha256').update(e164, 'utf8').digest('hex');
  return hash.substring(0, 16); // Exactly 16 characters
}

async function migrateCollection(collectionName) {
  console.log(`\n🚀 Starting Firestore "${collectionName}" collection phone_hash migration...`);
  const colRef = db.collection(collectionName);
  const snapshot = await colRef.get();

  if (snapshot.empty) {
    console.log(`⚠️ No documents found in "${collectionName}" collection.`);
    return;
  }

  console.log(`Found ${snapshot.size} document(s) in "${collectionName}". Processing...`);
  let updatedCount = 0;
  let alreadyValidCount = 0;

  const batchSize = 400;
  let batch = db.batch();
  let countInBatch = 0;

  for (const doc of snapshot.docs) {
    const data = doc.data();
    const currentHash = data.phone_hash || '';
    const phone = data.phoneNumber || data.phone || data.mobileNumber || data.verifiedMobileNumber || doc.id;

    const e164 = normalizeToE164(phone);
    const expected16Hash = generate16CharHash(e164);

    if (!expected16Hash) {
      console.log(`⚠️ Doc "${doc.id}" has invalid phone: "${phone}". Skipping.`);
      continue;
    }

    if (currentHash.length === 16 && currentHash === expected16Hash) {
      alreadyValidCount++;
      continue;
    }

    batch.set(
      doc.ref,
      {
        phone_hash: expected16Hash,
        phoneHash: expected16Hash,
        cleanPhone: expected16Hash,
        phoneNumberE164: e164,
        hashMigratedAt: Date.now()
      },
      { merge: true }
    );

    console.log(`✅ Queued update for doc "${doc.id}": phone_hash -> "${expected16Hash}" (E.164: "${e164}")`);
    updatedCount++;
    countInBatch++;

    if (countInBatch >= batchSize) {
      await batch.commit();
      console.log(`💾 Committed batch of ${countInBatch} documents.`);
      batch = db.batch();
      countInBatch = 0;
    }
  }

  if (countInBatch > 0) {
    await batch.commit();
    console.log(`💾 Committed final batch of ${countInBatch} documents.`);
  }

  console.log(`🎉 "${collectionName}" Migration Complete!`);
  console.log(`   Total Scanned: ${snapshot.size}`);
  console.log(`   Updated with 16-char phone_hash: ${updatedCount}`);
  console.log(`   Already Valid: ${alreadyValidCount}`);
}

async function migrateAllPhoneHashes() {
  for (const col of ['users', 'profiles']) {
    await migrateCollection(col);
  }
}

migrateAllPhoneHashes().catch(err => {
  if (err && (err.code === 8 || (err.message && err.message.includes('RESOURCE_EXHAUSTED')))) {
    console.error('\n⚠️ FIRESTORE DAILY QUOTA EXCEEDED (RESOURCE_EXHAUSTED):');
    console.error('The Firebase project "vibesync-chat-social-connect" has hit the Cloud Firestore Spark (Free) plan daily read/write limit.');
    console.error('Solutions:');
    console.error('  1. Wait for the daily quota to reset at midnight US Pacific Time (PST).');
    console.error('  2. Upgrade the Firebase project to the Blaze (Pay-as-you-go) plan in the Firebase Console:');
    console.error('     https://console.firebase.google.com/project/vibesync-chat-social-connect/usage');
    console.error('  3. Note: VibeSync already has Supabase PostgREST fully operational for zero-cost sync without Firestore quota limitations.\n');
  } else {
    console.error('❌ Migration failed:', err);
  }
  process.exit(1);
});
