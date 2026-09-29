# VibeSync Ad Injection, Programmatic CPM Bidding & Ethical Ad Policy Specification

---

## 1. Executive Overview

The **VibeSync Ad Platform** is an ethical, high-performance programmatic monetization and local business discovery engine built natively into the VibeSync mobile ecosystem. It balances high advertiser ROI with an enjoyable, user-first dating and social experience.

### Core Guarantees:
* **Zero Compulsion / No Screen Struck**: Users are never locked into watching ads. Every ad card and story can be skipped or swiped away instantly.
* **Non-Intrusive Native Placements**: Cleanly interleaved within the swipe deck and status stories feed (1 ad per 5 items).
* **Transparent Programmatic CPM Engine**: Advertisers pay exclusively for verified visible impressions (Cost Per Mille / 1,000 impressions).
* **Unified Business Wallet Integration**: Direct payment via credit/debit card, UPI, or seamless deduction from VibeSync Wallet points (1 Point = ₹0.05).

---

## 2. Ad Placement Formats & Injection Frequency Rules

| Placement Channel | Injection Frequency | Supported Media Formats | Max Duration | Interactive CTA Targets |
| :--- | :--- | :--- | :--- | :--- |
| **Swipe Cards Deck** | Exactly **1 ad card every 5 profile swipes** | High-Res Visual (1080x1920) / Short Video | Up to 30 seconds | • Open Business Profile<br>• Visit Website URL<br>• VibeSync Direct Chat |
| **Status Stories Feed** | Exactly **1 ad story every 5 user stories viewed** | Full-Screen Image / Vertical Video Clip | Up to 30 seconds | • Open Business Profile<br>• Visit External Link<br>• Claim In-App Voucher |

### Responsive Ad Features:
1. **Dynamic Media Rendering**: Smooth hardware-accelerated image caching and short video autoplay with mute/unmute and seek controls.
2. **Sponsored Badge**: Clearly labeled header badge with advertiser branding and direct business profile shortcut.
3. **Instant Dismissal**: Prominent top-right `✕` skip button and native left/right swipe dismissal without lag.

---

## 3. Programmatic CPM Monetization Engine & Pricing Tiers

Advertisers can choose campaign targets, geo-radius filters, and delivery speeds. All billing is computed on **Cost Per Mille (CPM)**.

### CPM Pricing Matrix

| Campaign Tier | Format / Target | Base CPM (per 1,000 Views) | Effective Cost / View | Ideal Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **Starter Standard** | High-Res Static Creative | **₹30.00** | ₹0.030 | Brand awareness, menu launches, regional events |
| **Prime Video HD** | Short Video (15s–30s) | **₹50.00** | ₹0.050 | Nightlife teasers, fashion showcases, date night spots |
| **Hyper-Local Targeted** | Geo-fenced (< 10 km radius) | **₹75.00** | ₹0.075 | Cafes, local boutiques, dining promotions |
| **Blitz Express Surge** | Active Burst (< 60 minutes) | **₹200.00** | ₹0.200 | Flash discounts, happy hours, live dating mixers |

---

## 4. Impression Packages & Wallet Conversion

Business owners can launch campaigns by paying via **Cards / UPI / NetBanking (Single Gateway Checkout)** or deducting **VibeSync Wallet Points**.

### Points Valuation:
$$\mathbf{1\text{ VibeSync Point} = ₹0.05\text{ (5 paise)}} \iff \mathbf{100\text{ Points} = ₹5.00}$$

### Standard Ad Packages

| Package | Guaranteed Impressions | Price (INR) | Wallet Points Equivalent | Bonus Features |
| :--- | :--- | :--- | :--- | :--- |
| **Micro Test** | 1,000 impressions | ₹30 – ₹200 | 600 – 4,000 pts | Live analytics dashboard |
| **Growth Pack** | 5,000 impressions | ₹140 – ₹900 | 2,800 – 18,000 pts | 5% bulk impression bonus |
| **Pro Business** | 10,000 impressions | ₹250 – ₹1,600 | 5,000 – 32,000 pts | Priority placement in swipe queue |
| **Enterprise Mega** | 50,000 impressions | ₹1,100 – ₹7,000 | 22,000 – 140,000 pts | Dedicated VIP account manager + verified Gold badge |

---

## 5. Business Wallet & Follower Broadcast Integration

1. **Persistent Top Bar Wallet**: Visible in the Business Hub navigation header showing live point balance, INR equivalent, instant "+ Top Up", and "📢 Broadcast" shortcuts.
2. **Follower Broadcasts**: Costs 1 point per follower. Real-time double-tick ($✓✓$) delivery tracking ensures that points for unread or undelivered recipients are **automatically refunded** back to the business wallet.
3. **Direct Cloud Messenger API**: Configure custom business helpline numbers, VBA IDs, API tokens, webhook endpoints, and run live ping latency diagnostics.

---

## 6. VibeSync Ethical Ad Policy

VibeSync enforces strict user-centric ad standards to prevent common mobile advertising pitfalls:

```
+-------------------------------------------------------------------------+
|                  VIBESYNC USER-FIRST AD PRINCIPLES                      |
+-------------------------------------------------------------------------+
| [1] ZERO COMPULSION: No mandatory watch timers, unskippable countdowns  |
| [2] NO SCREEN STRUCK: Full touch control; immediate swipe/skip at 0s    |
| [3] ACCURATE LOGGING: Impressions count only after entering viewport    |
| [4] PRIVACY BY DESIGN: No device ID reselling; fully anonymized tags    |
| [5] NO DECEPTIVE CTAS: Direct link previews and authentic sponsor cards |
+-------------------------------------------------------------------------+
```

### Policy Rules:
1. **Instant User Control**: No ad shall lock, freeze, or disable navigation controls. The user retains 100% autonomy to dismiss ads at the exact moment they appear.
2. **Honest Viewability Threshold**: Impression metrics are logged dynamically only when the ad is rendered into the active viewport for at least 1.0 second.
3. **Zero 3rd-Party Data Leaks**: All advertising delivery and user interest matching are handled securely within the VibeSync private engine without distributing user logs to third-party ad brokers.
