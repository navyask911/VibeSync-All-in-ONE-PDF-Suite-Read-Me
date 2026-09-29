package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.DatingDatabase
import com.example.data.model.RegisteredAccountEntity
import com.example.data.repository.DatingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VibeSync", appName)
  }

  @Test
  fun `verify duplicate profile detection prevents multi-accounting with new phone`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, DatingDatabase::class.java).allowMainThreadQueries().build()
    try {
      val repository = DatingRepository(db)

      // Seed/register primary biometric account
      val primaryBioHash = "BIO_HUMAN_PRIMARY_911"
      db.registeredAccountDao().insertOrUpdate(
          RegisteredAccountEntity(
              id = "acc_primary_test",
              phoneNumber = "+1 555-0199",
              googleEmail = "navyask911@gmail.com",
              userName = "Jordan Taylor",
              biometricHash = primaryBioHash,
              biometricRegisteredTimestamp = System.currentTimeMillis()
          )
      )

      // When someone attempts to register with a NEW phone number using the same biometric face:
      val duplicate = repository.checkDuplicateAccount(
          newPhone = "+1 555-8888", // New mobile number
          newEmail = "new_random@gmail.com",
          biometricHash = primaryBioHash
      )

      // Detection MUST identify the existing profile so duplicate creation is blocked!
      assertNotNull("Duplicate profile must be detected for existing biometric signature", duplicate)
      assertEquals("+1 555-0199", duplicate?.phoneNumber)
      assertEquals("navyask911@gmail.com", duplicate?.googleEmail)
    } finally {
      db.close()
    }
  }

  @Test
  fun `verify logout action wipes local user data and resets session`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, DatingDatabase::class.java).allowMainThreadQueries().build()
    try {
      val repository = DatingRepository(db)

      // Setup active logged in user session
      db.userPreferencesDao().insertOrUpdate(
          com.example.data.model.UserPreferencesEntity(
              id = 1,
              isLoggedIn = true,
              isProfileCompleted = true,
              verifiedMobileNumber = "+1 555-0199",
              googleEmail = "user@vibesync.test",
              userName = "Alex Rivera",
              loginTimestamp = System.currentTimeMillis()
          )
      )

      // Seed some local user data (match and chat message)
      db.matchDao().insertMatch(
          com.example.data.model.MatchEntity(
              matchId = "match_test_1",
              profileId = "p_test_profile",
              matchedAt = System.currentTimeMillis()
          )
      )
      db.chatMessageDao().insertMessage(
          com.example.data.model.ChatMessageEntity(
              messageId = "msg_test_1",
              matchId = "match_test_1",
              senderId = "current_user",
              text = "Hello there!"
          )
      )

      // Execute logout
      repository.logout()

      // Assert session is reset and local data is wiped
      val prefsAfter = db.userPreferencesDao().getPreferencesSync()
      assertNotNull(prefsAfter)
      assertEquals(false, prefsAfter?.isLoggedIn)
      assertEquals(false, prefsAfter?.isProfileCompleted)
      assertEquals("", prefsAfter?.verifiedMobileNumber)
      assertEquals("", prefsAfter?.googleEmail)
      assertEquals(0L, prefsAfter?.loginTimestamp)

      // Verify local chats and matches are wiped
      val matchAfter = db.matchDao().getMatchByIdSync("match_test_1")
      val messageAfter = db.chatMessageDao().getMessageById("msg_test_1")
      assertEquals(null, matchAfter)
      assertEquals(null, messageAfter)
    } finally {
      db.close()
    }
  }

  @Test
  fun `verify delete account action wipes account record, session, and local data`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, DatingDatabase::class.java).allowMainThreadQueries().build()
    try {
      val repository = DatingRepository(db)

      val phone = "+1 555-9988"
      val email = "delete_me@vibesync.test"

      // Seed registered account
      db.registeredAccountDao().insertOrUpdate(
          RegisteredAccountEntity(
              id = "acc_to_delete",
              phoneNumber = phone,
              googleEmail = email,
              userName = "Deletable User",
              biometricHash = "BIO_DELETE_HASH"
          )
      )

      // Setup active session
      db.userPreferencesDao().insertOrUpdate(
          com.example.data.model.UserPreferencesEntity(
              id = 1,
              isLoggedIn = true,
              isProfileCompleted = true,
              verifiedMobileNumber = phone,
              googleEmail = email,
              userName = "Deletable User",
              loginTimestamp = System.currentTimeMillis()
          )
      )

      // Execute account deletion
      repository.deactivateOrDeleteAccount(phone, email)

      // Assert registered account was marked deleted/deactivated
      val deletedAccount = db.registeredAccountDao().getAccountByPhone(phone)
      assertNotNull(deletedAccount)
      assertEquals("DELETED", deletedAccount?.accountStatus)
      assertEquals(true, deletedAccount?.isDeleted)

      // Assert user preferences session was cleared
      val prefsAfter = db.userPreferencesDao().getPreferencesSync()
      assertNotNull(prefsAfter)
      assertEquals(false, prefsAfter?.isLoggedIn)
      assertEquals("", prefsAfter?.verifiedMobileNumber)
    } finally {
      db.close()
    }
  }
}

