# VibeSync Business Wallet, Points Broadcast & Direct Messaging API Architecture

## 1. System Overview
The **VibeSync Business Suite** provides local businesses, cafes, date spots, and brands with high-conversion communication and engagement tools. The ecosystem is completely first-party, powered exclusively by native **VibeSync** infrastructure without reliance on third-party branded portals.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       VibeSync Business Hub Architecture                    │
├───────────────────┬─────────────────────────────────────────────────────────┤
│ Core Module       │ Capabilities & Mechanisms                               │
├───────────────────┼─────────────────────────────────────────────────────────┤
│ 💳 Business Wallet│ • Live Balance in Points & ₹ INR (1 Point = ₹0.05)      │
│                   │ • Auto-deduction for Broadcasts & CPM Ads               │
│                   │ • Multi-tier top-up reload packages                     │
├───────────────────┼─────────────────────────────────────────────────────────┤
│ 📣 Broadcast Engine│ • 1 Point / Recipient Follower (100 followers = ₹5.00) │
│                   │ • High-engagement date vouchers & event announcements    │
│                   │ • Target audience filtering & image banner support      │
├───────────────────┼─────────────────────────────────────────────────────────┤
│ ✓✓ Delivery Refund│ • Real-time double-tick (✓✓) delivery tracking          │
│                   │ • Automatic instant refund for undelivered recipients   │
│                   │ • Immutable transaction ledger audit trail              │
├───────────────────┼─────────────────────────────────────────────────────────┤
│ 💬 Business API   │ • VibeSync Cloud Business Messaging Engine              │
│                   │ • Direct verified partner chat with automated bots      │
│                   │ • Webhook endpoints & real-time test ping handshake     │
└───────────────────┴─────────────────────────────────────────────────────────┘
```

---

## 2. Wallet & Conversion Rate Model

### Valuation Rate:
$$\text{1 VibeSync Point} = \text{₹0.05 INR (5 Paise)} \quad \Longleftrightarrow \quad \text{₹1.00 INR} = \text{20 Points}$$

### Wallet Top-Up Packages

| Package Tier | Top-Up Price (INR) | Wallet Points Credited | Bonus Perk |
| :--- | :--- | :--- | :--- |
| **Starter Reload** | ₹100.00 | **2,000 Points** | Base conversion |
| **Growth Pack** | ₹250.00 | **5,250 Points** | +5% Bonus Points (250 pts free) |
| **Pro Business** | ₹500.00 | **11,000 Points** | +10% Bonus Points (1,000 pts free) |
| **Enterprise Surge** | ₹1,000.00 | **23,000 Points** | +15% Bonus Points (3,000 pts free) |

---

## 3. Follower Broadcast & Double-Tick (✓✓) Auto-Refund Engine

### Broadcast Cost Model:
* **Cost per recipient follower:** **1 Point** (₹0.05 per follower).
* *Example:* Broadcasting to 100 followers debits **100 Points (₹5.00)** from the venue wallet.

### Automatic Refund Flow:
1. When a broadcast is dispatched, the full recipient point amount is placed on escrow hold.
2. The VibeSync real-time messaging pipeline transmits messages to active follower inboxes.
3. Every delivered message records a **`DELIVERED ✓✓`** acknowledgment.
4. Any messages that remain unread, bounced, or undelivered upon timeout are flagged, and the corresponding points are **automatically credited back into the Business Wallet** with an auto-refund ledger entry.

---

## 4. VibeSync Cloud Business Messaging API

Businesses can enable automated direct communication with customers and daters via the native **VibeSync Business Cloud Messenger API**:
* **VibeSync Business Account ID (VBA ID):** Unique enterprise identity token.
* **Direct Partner Chat:** Instant 1-on-1 verified customer conversations within VibeSync.
* **Webhook Endpoints:** `https://api.vibesync.app/v1/biz/{businessId}/messenger/webhook` for CRM / POS automation.
* **Live Test Handshake Tool:** Integrated ping console to verify API token integrity.

---

## 5. UI Integration Map

* **Discover Screen ➔ Business Hub (Top Bar Store Icon):**
  * **Tab 0 ("Nearest"):** Tap any venue ➔ Owner Action Toolbar:
    * `[ 📣 Broadcast ]` `[ 💳 Points Wallet ]` `[ 💬 VibeSync Business API ]` `[ ⭐ Upgrade Badge ]`
  * **Tab 2 ("My Venues"):** Every registered venue card displays the live **Wallet Balance Banner (e.g. 500 pts = ₹25.00)** with quick 1-tap reload and broadcast controls.
  * **Tab 3 ("Analytics & Dashboard"):** Complete performance breakdown of voucher redemptions and follower impressions.
* **"Add with Us" Campaign Wizard:** Allows financing CPM Ad placements directly using **VibeSync Business Wallet Points**.

---

*VibeSync Business Systems Architecture © 2026. 100% First-Party VibeSync Infrastructure.*
