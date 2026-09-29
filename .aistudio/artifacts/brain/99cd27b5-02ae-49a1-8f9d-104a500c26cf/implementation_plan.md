# Architecture & Scaling Plan: Zero-Cost Business Search (50k Venues & 500k Users)

## 1. Executive Summary & Problem Breakdown

When scaling VibeSync to **50,000 registered businesses** and **500,000 (5 lakh) active searching users**, traditional cloud database querying creates a massive **Read Explosion**:

* **Direct Cloud Query Math:**
  $$500,000\text{ active users} \times 1\text{ search/day} \times 10\text{ returned entities} = \mathbf{5,000,000\text{ reads/day}}$$
* **Firebase Free Tier Limit:** 50,000 reads/day.
* **Result:** A **100x overflow** that crashes the free tier within the first hour of morning traffic.

This plan outlines the architecture to decouple public user searches from cloud database reads, guaranteeing **$0 operational cost** while protecting both Firestore and the 500MB Supabase hash database.

---

## 2. High-Level System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MERCHANT REGISTRATION                           │
│  (Merchant pays ₹99/yr, fills details, signs with phone number)        │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ 1 Write per year
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│              FIREBASE FIRESTORE (`vibesync_venues`)                    │
│  - Strictly isolated for Merchant Writes & Reinstall Recovery          │
│  - Document ID: `biz_<id>` (ownerUserId = "+919876543210")             │
│  - Total Daily Reads: < 500 (only when merchants reinstall)            │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ Background Catalog Export (Every 24h)
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│           CLOUDFLARE R2 CDN (Zero-Cost Static Edge Storage)            │
│  - `venues_index_v1.json.gz` (50,000 venues compressed to ~1.2 MB)     │
│  - Cost: $0 (10 GB free storage, UNLIMITED FREE EGRESS BANDWIDTH)      │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ 1 Download per device every 24-48h
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    USER DEVICE (Android Client)                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                  ROOM SQLITE LOCAL SEARCH ENGINE                 │  │
│  │  - User searches "Cafe", "Indiranagar", "Coupons"                │  │
│  │  - Distance sorting by GPS (`distanceKm`)                        │  │
│  │  - 500,000 users run searches LOCALLY on-device                  │  │
│  │  - Cloud Database Reads Generated: 0                             │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Storage & Scaling Comparison

| Dimension | Direct Firestore Querying | VibeSync Local-First + R2 Architecture |
| :--- | :--- | :--- |
| **Search Engine Location** | Cloud server / Firestore indexes | Client phone (Room SQLite) |
| **Daily Cloud Reads (500k users)** | **5,000,000 reads** (100x over limit) | **0 reads** |
| **Bandwidth Cost** | Heavy database egress bills | **$0** (Cloudflare R2 free tier) |
| **Catalog Size (50,000 venues)** | Massive JSON transfers | **~1.2 MB** (compressed Gzip index) |
| **Offline Capability** | ❌ Fails without active internet | ✅ Instant offline search & bookmarking |
| **Supabase 500MB Impact** | 0 bytes (never touches Supabase) | **0 bytes** (Supabase stays 100% hash-only) |

---

## 4. Key Implementation Components

### A. Local Search & Filtering Engine (`BusinessDao`)
* Implement full-text and categorical indexing in Android's local Room database:
  - FTS (Full-Text Search) or optimized SQLite index on `name`, `category`, `city`, and `activeOfferSummary`.
  - Geo-distance calculation executed via local SQLite math queries.
  - Zero network latency: results return in under 5 milliseconds.

### B. Lightweight Edge Sync Worker (`BusinessCatalogSyncManager`)
* Periodically fetches the compressed static catalog (`venues_index.json.gz`) via OkHttp with HTTP `If-None-Match` (ETag caching).
* If no catalog changes occurred, the server returns `304 Not Modified` (0 bytes downloaded).
* Inserts/updates entries in Room batch transactions (`insertBusinesses(batch)`).

### C. Merchant Reinstall & Recovery Guard
* Merchants who paid ₹99/year are authenticated via their verified phone number.
* When they reinstall or clear storage, the app performs a direct single-document query:
  `collection("vibesync_venues").whereEqualTo("ownerUserId", userPhone)`
* Cost: Exactly **1 read** for the merchant, completely within the 50,000 daily limit.

---

## 5. Verification & Rollout Plan

1. **Verify Local Search Performance:** Benchmark local Room queries with 50,000 seeded entities to ensure sub-10ms UI response on Android.
2. **Verify Bandwidth & Compression:** Ensure gzip/brotli compression keeps catalog size under 1.5 MB.
3. **Verify Reinstall Recovery:** Validate that a merchant who uninstalls and logs back in with their phone number instantly restores their verified ₹99/year business profile.
