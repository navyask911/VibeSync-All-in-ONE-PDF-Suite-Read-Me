-- ====================================================================
-- VibeSync Production Database Schema Script (Version 31)
-- Engine: SQLite / Room Database
-- Purpose: Lightweight, zero-bloat schema definition for VibeSync
-- ====================================================================

-- 1. Profiles Table
CREATE TABLE IF NOT EXISTS `profiles` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `name` TEXT NOT NULL,
    `age` INTEGER NOT NULL,
    `occupation` TEXT NOT NULL,
    `city` TEXT NOT NULL,
    `distanceMiles` INTEGER NOT NULL,
    `bio` TEXT NOT NULL,
    `interests` TEXT NOT NULL,
    `relationshipGoal` TEXT NOT NULL,
    `promptQuestion` TEXT NOT NULL,
    `promptAnswer` TEXT NOT NULL,
    `gradientColorStart` INTEGER NOT NULL,
    `gradientColorEnd` INTEGER NOT NULL,
    `avatarEmoji` TEXT NOT NULL,
    `avatarUrl` TEXT NOT NULL DEFAULT '',
    `isVerified` INTEGER NOT NULL,
    `likedMe` INTEGER NOT NULL DEFAULT 0,
    `isSuperLikedMe` INTEGER NOT NULL DEFAULT 0,
    `trustScore` INTEGER NOT NULL DEFAULT 95,
    `isOpenForDating` INTEGER NOT NULL DEFAULT 1,
    `gender` TEXT NOT NULL DEFAULT 'Female',
    `maritalStatus` TEXT NOT NULL DEFAULT 'Single (Never Married)',
    `phoneNumber` TEXT NOT NULL DEFAULT '',
    `email` TEXT NOT NULL DEFAULT '',
    `country` TEXT NOT NULL DEFAULT 'India',
    `countryFlag` TEXT NOT NULL DEFAULT '🇮🇳',
    `place` TEXT NOT NULL DEFAULT 'Downtown Metro',
    `qualification` TEXT NOT NULL DEFAULT 'Bachelors'
);

-- 2. Matched Contacts Table (Zero-Knowledge SHA-256 Hashed Phonebook Matches)
CREATE TABLE IF NOT EXISTS `matched_contacts` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `contactName` TEXT NOT NULL DEFAULT '',
    `phoneNumber` TEXT NOT NULL DEFAULT '',
    `phoneHash` TEXT NOT NULL DEFAULT '',
    `vibeSyncUserId` TEXT NOT NULL DEFAULT '',
    `isOnVibeSync` INTEGER NOT NULL DEFAULT 0,
    `matchedAt` INTEGER NOT NULL DEFAULT 0,
    `avatarEmoji` TEXT NOT NULL DEFAULT '✨',
    `photoUrl` TEXT NOT NULL DEFAULT '',
    `statusTagline` TEXT NOT NULL DEFAULT ''
);

-- 3. Swipes Table
CREATE TABLE IF NOT EXISTS `swipes` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `targetProfileId` TEXT NOT NULL,
    `direction` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL
);

-- 4. Matches Table
CREATE TABLE IF NOT EXISTS `matches` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `matchedProfileId` TEXT NOT NULL,
    `matchedAt` INTEGER NOT NULL,
    `lastMessage` TEXT NOT NULL,
    `lastMessageTimestamp` INTEGER NOT NULL,
    `unreadCount` INTEGER NOT NULL
);

-- 5. Chat Messages Table
CREATE TABLE IF NOT EXISTS `chat_messages` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `matchId` TEXT NOT NULL,
    `senderId` TEXT NOT NULL,
    `text` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL,
    `isSentByMe` INTEGER NOT NULL,
    `deliveryStatus` TEXT NOT NULL DEFAULT 'SENT',
    `mediaUrl` TEXT NOT NULL DEFAULT '',
    `mediaType` TEXT NOT NULL DEFAULT 'NONE'
);

-- 6. Local Messages Table
CREATE TABLE IF NOT EXISTS `local_messages` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `senderUid` TEXT NOT NULL,
    `textContent` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL,
    `isSentByMe` INTEGER NOT NULL,
    `matchId` TEXT NOT NULL DEFAULT ''
);

-- 7. User Preferences Table
CREATE TABLE IF NOT EXISTS `user_preferences` (
    `id` INTEGER PRIMARY KEY NOT NULL,
    `userName` TEXT NOT NULL DEFAULT '',
    `userAge` INTEGER NOT NULL DEFAULT 24,
    `userCity` TEXT NOT NULL DEFAULT '',
    `userBio` TEXT NOT NULL DEFAULT '',
    `userInterests` TEXT NOT NULL DEFAULT '',
    `verifiedMobileNumber` TEXT NOT NULL DEFAULT '',
    `googleEmail` TEXT NOT NULL DEFAULT '',
    `isProfileCompleted` INTEGER NOT NULL DEFAULT 0,
    `soundAlertsEnabled` INTEGER NOT NULL DEFAULT 1,
    `vibrationAlertsEnabled` INTEGER NOT NULL DEFAULT 1,
    `messageSoundTone` TEXT NOT NULL DEFAULT 'DEFAULT',
    `matchSoundTone` TEXT NOT NULL DEFAULT 'CELEBRATION',
    `callRingtone` TEXT NOT NULL DEFAULT 'STANDARD_RING'
);

-- 8. Registered Accounts Table
CREATE TABLE IF NOT EXISTS `registered_accounts` (
    `phoneNumber` TEXT NOT NULL PRIMARY KEY,
    `googleEmail` TEXT NOT NULL DEFAULT '',
    `registeredAt` INTEGER NOT NULL DEFAULT 0,
    `lastLoginAt` INTEGER NOT NULL DEFAULT 0
);

-- 9. Status Stories Table
CREATE TABLE IF NOT EXISTS `status_stories` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `userId` TEXT NOT NULL,
    `userName` TEXT NOT NULL,
    `userEmoji` TEXT NOT NULL,
    `mediaUrl` TEXT NOT NULL DEFAULT '',
    `captionText` TEXT NOT NULL DEFAULT '',
    `createdAt` INTEGER NOT NULL,
    `expiresAt` INTEGER NOT NULL
);

-- 10. Friendship Requests Table
CREATE TABLE IF NOT EXISTS `friendship_requests` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `senderId` TEXT NOT NULL,
    `receiverId` TEXT NOT NULL,
    `status` TEXT NOT NULL DEFAULT 'PENDING',
    `createdAt` INTEGER NOT NULL
);

-- 11. User Contacts Table
CREATE TABLE IF NOT EXISTS `user_contacts` (
    `phoneNumber` TEXT NOT NULL PRIMARY KEY,
    `name` TEXT NOT NULL,
    `isOnApp` INTEGER NOT NULL DEFAULT 0
);

-- 12. Businesses Table
CREATE TABLE IF NOT EXISTS `businesses` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `name` TEXT NOT NULL,
    `category` TEXT NOT NULL,
    `rating` REAL NOT NULL,
    `address` TEXT NOT NULL
);

-- 13. Business Posts Table
CREATE TABLE IF NOT EXISTS `business_posts` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `businessId` TEXT NOT NULL,
    `content` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL
);

-- 14. Channels Table
CREATE TABLE IF NOT EXISTS `channels` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `name` TEXT NOT NULL,
    `description` TEXT NOT NULL,
    `memberCount` INTEGER NOT NULL DEFAULT 1
);

-- 15. Channel Broadcasts Table
CREATE TABLE IF NOT EXISTS `channel_broadcasts` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `channelId` TEXT NOT NULL,
    `messageText` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL
);

-- 16. Blocks Table
CREATE TABLE IF NOT EXISTS `blocks` (
    `blockedUserId` TEXT NOT NULL PRIMARY KEY,
    `blockedAt` INTEGER NOT NULL
);

-- 17. Reports Table
CREATE TABLE IF NOT EXISTS `reports` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `reportedUserId` TEXT NOT NULL,
    `reason` TEXT NOT NULL,
    `reportedAt` INTEGER NOT NULL
);

-- ====================================================================
-- Database Maintenance & Reclaim Disk Space Script
-- Execute to erase unrequired / stale temporary registration cache
-- ====================================================================
DELETE FROM profiles WHERE id LIKE 'demo_%' OR id LIKE 'seed_%' OR id LIKE 'temp_%' OR id LIKE 'test_%';
DELETE FROM status_stories WHERE expiresAt < strftime('%s', 'now') * 1000;
VACUUM;
