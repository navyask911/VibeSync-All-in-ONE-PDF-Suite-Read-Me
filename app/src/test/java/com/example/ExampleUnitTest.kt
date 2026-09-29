package com.example

import com.example.data.model.ProfileEntity
import com.example.data.model.UserPreferencesEntity
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun profile_interestParsing_returnsCleanList() {
    val profile = ProfileEntity(
      id = "test_1",
      name = "Sam",
      age = 25,
      occupation = "Designer",
      city = "Metro",
      distanceMiles = 5,
      bio = "Hello",
      interests = "Coffee, Photography , Hiking",
      relationshipGoal = "Long-term relationship",
      promptQuestion = "A goal of mine",
      promptAnswer = "Run a marathon",
      gradientColorStart = 0xFF000000,
      gradientColorEnd = 0xFFFFFFFF,
      avatarEmoji = "☕"
    )

    val interests = profile.getInterestList()
    assertEquals(3, interests.size)
    assertEquals("Coffee", interests[0])
    assertEquals("Photography", interests[1])
    assertEquals("Hiking", interests[2])
  }

  @Test
  fun userPreferences_defaultFreePlan_isActive() {
    val prefs = UserPreferencesEntity()
    assertTrue(prefs.freePlanActive)
    assertEquals(18, prefs.minAge)
    assertEquals(99, prefs.maxAge)
    assertFalse(prefs.isLoggedIn)
    assertTrue(prefs.isFaceVerified)
  }

  @Test
  fun profile_antiSpamAndVerification_defaultsAreSecure() {
    val profile = ProfileEntity(
      id = "test_spam",
      name = "Bot Account",
      age = 30,
      occupation = "Crypto Trader",
      city = "Unknown",
      distanceMiles = 100,
      bio = "Message me on crypto cashapp",
      interests = "Crypto, Money",
      relationshipGoal = "Casual",
      promptQuestion = "Ask me about",
      promptAnswer = "Fast money",
      gradientColorStart = 0xFF000000,
      gradientColorEnd = 0xFFFFFFFF,
      avatarEmoji = "🤖",
      isBanned = true,
      isFlaggedSpam = true,
      trustScore = 30,
      isRealFaceVerified = false
    )

    assertTrue(profile.isBanned)
    assertTrue(profile.isFlaggedSpam)
    assertFalse(profile.isRealFaceVerified)
    assertEquals(30, profile.trustScore)
  }

  @Test
  fun countryCodeList_containsMajorCountriesWithDialCodesAndFlags() {
    val countries = com.example.data.model.CountryCodeList.allCountries
    assertTrue(countries.isNotEmpty())
    assertTrue(countries.size >= 40)

    val us = countries.find { it.isoCode == "US" }
    assertNotNull(us)
    assertEquals("+1", us?.dialCode)
    assertEquals("🇺🇸", us?.flagEmoji)

    val india = countries.find { it.isoCode == "IN" }
    assertNotNull(india)
    assertEquals("+91", india?.dialCode)
    assertEquals("🇮🇳", india?.flagEmoji)

    val uk = countries.find { it.isoCode == "GB" }
    assertNotNull(uk)
    assertEquals("+44", uk?.dialCode)
    assertEquals("🇬🇧", uk?.flagEmoji)

    val uae = countries.find { it.isoCode == "AE" }
    assertNotNull(uae)
    assertEquals("+971", uae?.dialCode)
    assertEquals("🇦🇪", uae?.flagEmoji)
  }
}

