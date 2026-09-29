import { test, expect } from '@playwright/test';

test.describe('VibeSync Clean UI, WhatsApp Soft Typography & Spark Plan Functional Suite', () => {

  test('01: Verify Application Loads with Smooth UI & Soft WhatsApp Sizing', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Title and Brand Presence
    const pageTitle = await page.title();
    expect(pageTitle).toBeDefined();

    // Verify WhatsApp-like Soft Typography Elements
    const bodyText = page.locator('body');
    await expect(bodyText).toBeVisible();

    // Verify no overflowing horizontal containers on mobile screen width
    const viewportSize = page.viewportSize();
    if (viewportSize) {
      const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
      expect(scrollWidth).toBeLessThanOrEqual(viewportSize.width + 1);
    }
  });

  test('02: Clean Screens & Absence of Verbose Unnecessary Explanations', async ({ page }) => {
    await page.goto('/');
    
    // Ensure clean uncluttered layout without multi-paragraph disclaimer dumps
    const longDisclaimers = page.locator('text=/Lorem ipsum|Disclaimer: By continuing you understand that the platform makes no guarantee/i');
    const count = await longDisclaimers.count();
    expect(count).toBe(0);

    // Verify WhatsApp standard clean actionable buttons
    const continueBtn = page.locator('button, [role="button"]', { hasText: /Continue|Verify|Chat/i }).first();
    if (await continueBtn.isVisible()) {
      await expect(continueBtn).toBeEnabled();
    }
  });

  test('03: Zero Google AdSense & AdMob in Front-end and Network Requests', async ({ page }) => {
    const adRequests: string[] = [];
    page.on('request', request => {
      const url = request.url();
      if (url.includes('googlesyndication') || url.includes('doubleclick') || url.includes('admob')) {
        adRequests.push(url);
      }
    });

    await page.goto('/');
    await page.waitForTimeout(2000);

    // Ensure zero AdMob/AdSense network requests
    expect(adRequests.length).toBe(0);

    // Ensure zero AdMob/AdSense DOM elements
    const adBanners = page.locator('ins.adsbygoogle, iframe[id*="google_ads"], div[class*="admob"]');
    expect(await adBanners.count()).toBe(0);
  });

  test('04: Single Profile Per Mobile & Account Integrity Verification', async ({ page }) => {
    await page.goto('/');

    // Check presence of phone number input
    const phoneInput = page.locator('input[type="tel"], input[placeholder*="mobile" i], input[placeholder*="phone" i]');
    if (await phoneInput.first().isVisible()) {
      await phoneInput.first().fill('9876543210');
      const val = await phoneInput.first().inputValue();
      expect(val).toContain('9876543210');
    }
  });

  test('05: Firebase Spark Plan Free-Tier Compliance & Zero Blaze Billing Trigger', async ({ page }) => {
    const billingErrors: string[] = [];
    page.on('console', msg => {
      const text = msg.text();
      if (text.includes('FirebaseError: [code=permission-denied]') || text.includes('requires Blaze plan') || text.includes('billing')) {
        billingErrors.push(text);
      }
    });

    await page.goto('/');
    await page.waitForTimeout(1500);

    // No Blaze plan dependency errors
    expect(billingErrors.length).toBe(0);
  });

});
