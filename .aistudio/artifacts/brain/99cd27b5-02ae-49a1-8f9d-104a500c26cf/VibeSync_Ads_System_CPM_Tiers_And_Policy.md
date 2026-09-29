# VibeSync Programmatic Ad System, CPM Tiers & User Freedom Policy

## Executive Overview
VibeSync introduces a next-generation, non-intrusive ad injection network built specifically for modern social, dating, and discovery platforms. The monetization engine couples standard programmatic **CPM (Cost Per Mille / Cost per 1,000 Impressions)** bidding with strict **User-First UX guarantees**—ensuring businesses achieve high-conversion local reach without ever degrading the dating and connection experience for end users.

---

## 1. Ad Formats & Delivery Mechanics

### A. In-Feed Swipe Deck Ad Injection (1 per 5 Swipes)
* **Injection Frequency:** Automatically inserts **1 responsive ad card after every 5 profile swipes** (`swipeCounter % 5 == 0`).
* **Display Formats:**
  * **High-Resolution Image Ads:** Crisp, edge-to-edge branded visuals with clear typography.
  * **HD Video Ads (≤ 30s Max):** Short, engaging video previews with play/pause and sound controls.
* **Layout Structure:**
  * Header: Verified `📢 SPONSORED` badge + Advertiser Trade Name + Instant Skip (X) button.
  * Media Viewport: Centered 16:9 or vertical image/video container.
  * Body: Headline and promotional description (e.g., "Buy 1 Get 1 Free First Date Cocktails 🍸").
  * Action Deck: Primary Redirect Action Button + "Skip Ad & Resume Swiping" secondary action.

### B. In-Story Interstitial Ad Injection (1 per 5 Stories)
* **Injection Frequency:** Automatically inserts **1 sponsored ad story after every 5 user status stories viewed**.
* **Display Formats:**
  * Full-screen immersive 9:16 vertical image or short video story (< 30 seconds).
  * Segmented top progress bar with distinctive brand tint.
  * Advertiser avatar emoji, verified sponsor tag, and time indicator.
* **Direct Interactive CTA Bar:** Floating bottom banner allowing users to claim discounts, view menu vouchers, or message the business directly with one tap.

---

## 2. Standard Programmatic CPM Monetization & Pricing Matrix

All advertising campaigns on VibeSync operate on transparent, industry-standard **CPM (Cost Per 1,000 Views)**.

### CPM Rate Structure (India & Global Standard)

| Placement / Tier | Media Format | Active Duration | CPM Rate (INR) | CPM Rate (USD) | Wallet Points Equivalent |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Standard Feed Deck** | High-Res Image | 24 Hours / Scheduled | **₹150** / 1,000 views | $1.80 CPM | 3,000 Points / 1k |
| **Standard Story Ad** | High-Res Image | 24 Hours / Scheduled | **₹150** / 1,000 views | $1.80 CPM | 3,000 Points / 1k |
| **HD Video Interstitial** | Video (≤30s) | 24 Hours / Scheduled | **₹200** / 1,000 views | $2.40 CPM | 4,000 Points / 1k |
| **Prime Blitz Surge (<60m)** | Image / Video | 15m, 30m, 45m, 60m | **₹250** / 1,000 views | $3.00 CPM | 5,000 Points / 1k |
| **Hyperlocal Target (≤15km)**| Any | Custom Radius | **+₹25** surcharge | +$0.30 | +500 Points |

---

### Packaged Campaign Tiers

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       VibeSync CPM Impression Packs                         │
├───────────────────┬──────────────┬───────────────┬──────────────────────────┤
│ Tier Name         │ Impressions  │ Package Price │ Business Benefits        │
├───────────────────┼──────────────┼───────────────┼──────────────────────────┤
│ 🟢 Starter Pack   │ 1,000 Views  │ ₹150 INR      │ Ideal for single night   │
│ 🔵 Growth Pack    │ 5,000 Views  │ ₹650 INR      │ Save 13% • Weekend date  │
│ 🟣 Pro Business   │ 10,000 Views │ ₹1,200 INR    │ Save 20% • Multi-area    │
│ 🟡 Domination     │ 25,000 Views │ ₹2,750 INR    │ Save 27% • Metro reach   │
│ 👑 Enterprise     │ 50,000 Views │ ₹5,000 INR    │ Top feed priority        │
│ ⚡ 60-Min Blitz   │ 2,500 Views  │ ₹399 INR      │ Instant Happy Hour Rush  │
└───────────────────┴──────────────┴───────────────┴──────────────────────────┘
```

---

## 3. Targeting & Redirect Options

Advertisers can configure granular targeting and direct conversion channels:

1. **Geographic Targeting:**
   * City-level filtering (e.g., Bangalore, Mumbai, Delhi, New York, London, or All Cities).
   * Hyperlocal Radius Slider (5 km to 100 km).
2. **Action Redirect Destinations:**
   * **`BUSINESS_PROFILE`**: Direct in-app route to the venture's official VibeSync Profile (view vouchers, directions, reviews).
   * **`WEBSITE_URL`**: Deep link to external ordering portal, reservation engine, or website.
   * **`DIRECT_CHAT`**: Starts a direct 1-on-1 verified partner conversation with the business team.

---

## 4. Multi-Gateway Payment Options & Razorpay Integration

Business owners can finance campaigns through flexible payment rails:
* **UPI Instant:** Google Pay, PhonePe, Paytm, BHIM with instant zero-fee QR and app intent.
* **Credit/Debit Cards:** Visa, MasterCard, RuPay, Maestro with 3D-Secure authentication.
* **VibeSync Business Wallet:** 1 INR = 20 Points (Points deducted directly from store earnings/check-ins).
* **Razorpay Single-Window Integration:** Built-in merchant modal supporting live production and sandbox API keys (`key_id`, `key_secret`, webhook dispatch).

---

## 5. User-Centric Zero-Compulsion Ad Policy

> **Core Philosophy:** "Users come to VibeSync to meet people and discover great experiences. Ads should inform, never coerce."

### Strict Policy Commitments:

1. **Zero Screen Struck / Zero Screen Lock:**
   * Under **no circumstances** will an ad freeze, lock, or disable user input.
   * Swiping up, down, left, or right immediately dismisses the card.
2. **Zero Compulsory Watch Delay:**
   * **No mandatory countdown timers** (0 seconds forced wait).
   * Users can skip image and video ads at **millisecond 0**.
3. **No Deceptive Click Traps:**
   * No invisible hitboxes, misleading close buttons, or disguised elements.
   * Clear, high-contrast `SPONSORED` disclosure on all ad units.
4. **Fair Viewport Impression Logging:**
   * An impression is only deducted from an advertiser's CPM balance when the ad actually enters the active visible viewport.
5. **Auditory & Visual Courtesy:**
   * Video ads are strictly capped at 30 seconds max.
   * Volume is muted by default with an intuitive sound toggle.

---

*VibeSync Business Media Engine © 2026. All rights reserved.*
