package com.example.data.model

data class BusinessCategory(
    val name: String,
    val iconEmoji: String,
    val subCategories: List<String>
)

object BusinessCategories {
    const val ALL = "All"

    const val RETAIL_AND_SHOPS = "Retail & Shops"
    const val FOOD_AND_HOSPITALITY = "Food, Beverage & Hospitality"
    const val HEALTH_AND_WELLNESS = "Health, Wellness & Personal Care"
    const val SERVICES_AND_UTILITIES = "Services & Utilities"
    const val PUBLIC_AND_COMMUNITY = "Public & Community Spaces"

    val PARENT_CATEGORIES = listOf(
        BusinessCategory(
            name = RETAIL_AND_SHOPS,
            iconEmoji = "🛍️",
            subCategories = listOf(
                "Convenience store (or bodega/minimart)",
                "Grocery store / Supermarket",
                "Bakery & Pastry shop",
                "Pharmacy / Drugstore",
                "Clothing boutique / Shoe store",
                "Bookstore & Stationery shop",
                "Flower shop / Florist",
                "Hardware & DIY store",
                "Pet supply store",
                "Electronics & Mobile shop"
            )
        ),
        BusinessCategory(
            name = FOOD_AND_HOSPITALITY,
            iconEmoji = "☕",
            subCategories = listOf(
                "Café / Coffee shop",
                "Restaurants (fast food, casual, fine dining)",
                "Hotel / Hostel / Bed & Breakfast",
                "Bar / Pub / Cocktail lounge",
                "Ice cream parlor / Juice bar"
            )
        ),
        BusinessCategory(
            name = HEALTH_AND_WELLNESS,
            iconEmoji = "🧘",
            subCategories = listOf(
                "Gym / Fitness center / Yoga studio",
                "Hair salon / Barbershop",
                "Nail salon / Day spa",
                "Dental clinic",
                "Medical walk-in clinic",
                "Optician / Eyewear shop"
            )
        ),
        BusinessCategory(
            name = SERVICES_AND_UTILITIES,
            iconEmoji = "🔧",
            subCategories = listOf(
                "Bank / ATM kiosk",
                "Laundromat / Dry cleaners",
                "Post office / Courier & shipping center",
                "Tailor / Alterations shop",
                "Cobbler / Shoe repair",
                "Locksmith",
                "Auto repair shop / Gas station"
            )
        ),
        BusinessCategory(
            name = PUBLIC_AND_COMMUNITY,
            iconEmoji = "🏛️",
            subCategories = listOf(
                "Library",
                "Community center",
                "Police substation",
                "Street vending carts / Kiosks"
            )
        )
    )

    fun getAllParentNames(): List<String> = PARENT_CATEGORIES.map { it.name }

    fun getSubcategories(parentName: String): List<String> {
        return PARENT_CATEGORIES.firstOrNull { it.name.equals(parentName, ignoreCase = true) }?.subCategories ?: emptyList()
    }

    fun findCategoryForParent(parentName: String): BusinessCategory? {
        return PARENT_CATEGORIES.firstOrNull { it.name.equals(parentName, ignoreCase = true) }
    }

    /**
     * Checks if a business matches the selected category & optional sub-category
     */
    fun matchesCategory(
        bizCategory: String,
        bizName: String = "",
        bizTagline: String = "",
        selectedParentCategory: String,
        selectedSubCategory: String? = null
    ): Boolean {
        if (selectedParentCategory == ALL && selectedSubCategory.isNullOrBlank()) {
            return true
        }

        val bizCatClean = bizCategory.lowercase()
        val bizNameClean = bizName.lowercase()
        val bizTaglineClean = bizTagline.lowercase()
        val fullBizText = "$bizCatClean $bizNameClean $bizTaglineClean"

        // If specific sub-category filter is active
        if (!selectedSubCategory.isNullOrBlank()) {
            val subClean = selectedSubCategory.lowercase()

            if (fullBizText.contains(subClean)) return true

            // Match by subcategory key words (split words like "Bakery & Pastry shop" -> "bakery", "pastry")
            val subKeywords = subClean.split("/", ",", "(", ")", "&", " ")
                .map { it.trim().lowercase() }
                .filter { it.length > 2 && it !in setOf("or", "and", "the", "shop", "center", "store", "fast", "food") }

            for (kw in subKeywords) {
                if (fullBizText.contains(kw)) return true
            }

            // Keyword specific aliases
            if (subClean.contains("convenience") || subClean.contains("bodega") || subClean.contains("minimart")) {
                if (fullBizText.contains("convenience") || fullBizText.contains("bodega") || fullBizText.contains("mart")) return true
            }
            if (subClean.contains("café") || subClean.contains("coffee")) {
                if (fullBizText.contains("cafe") || fullBizText.contains("coffee") || fullBizText.contains("roasters") || fullBizText.contains("brew")) return true
            }
            if (subClean.contains("restaurant") || subClean.contains("dining")) {
                if (fullBizText.contains("restaurant") || fullBizText.contains("dining") || fullBizText.contains("bistro") || fullBizText.contains("pizza") || fullBizText.contains("burger")) return true
            }
            if (subClean.contains("bar") || subClean.contains("pub") || subClean.contains("lounge")) {
                if (fullBizText.contains("bar") || fullBizText.contains("pub") || fullBizText.contains("lounge") || fullBizText.contains("nightlife") || fullBizText.contains("cocktail") || fullBizText.contains("brewery")) return true
            }
            if (subClean.contains("hotel") || subClean.contains("hostel") || subClean.contains("bed & breakfast")) {
                if (fullBizText.contains("hotel") || fullBizText.contains("hostel") || fullBizText.contains("resort") || fullBizText.contains("inn") || fullBizText.contains("palace")) return true
            }
            if (subClean.contains("ice cream") || subClean.contains("juice")) {
                if (fullBizText.contains("ice cream") || fullBizText.contains("gelato") || fullBizText.contains("juice") || fullBizText.contains("dessert")) return true
            }
            if (subClean.contains("gym") || subClean.contains("fitness") || subClean.contains("yoga")) {
                if (fullBizText.contains("gym") || fullBizText.contains("fitness") || fullBizText.contains("yoga") || fullBizText.contains("crossfit") || fullBizText.contains("workout")) return true
            }
            if (subClean.contains("hair") || subClean.contains("barber")) {
                if (fullBizText.contains("hair") || fullBizText.contains("barber") || fullBizText.contains("salon")) return true
            }
            if (subClean.contains("nail") || subClean.contains("spa")) {
                if (fullBizText.contains("nail") || fullBizText.contains("spa") || fullBizText.contains("massage")) return true
            }
            if (subClean.contains("dental") || subClean.contains("medical") || subClean.contains("clinic")) {
                if (fullBizText.contains("dental") || fullBizText.contains("clinic") || fullBizText.contains("doctor") || fullBizText.contains("hospital")) return true
            }
            if (subClean.contains("optician") || subClean.contains("eyewear")) {
                if (fullBizText.contains("optician") || fullBizText.contains("eyewear") || fullBizText.contains("lens") || fullBizText.contains("glasses")) return true
            }
            if (subClean.contains("bank") || subClean.contains("atm")) {
                if (fullBizText.contains("bank") || fullBizText.contains("atm")) return true
            }
            if (subClean.contains("laundromat") || subClean.contains("dry cleaner")) {
                if (fullBizText.contains("laundry") || fullBizText.contains("laundromat") || fullBizText.contains("dry clean")) return true
            }
            if (subClean.contains("post") || subClean.contains("courier") || subClean.contains("shipping")) {
                if (fullBizText.contains("post") || fullBizText.contains("courier") || fullBizText.contains("dart") || fullBizText.contains("fedex") || fullBizText.contains("shipping")) return true
            }
            if (subClean.contains("tailor") || subClean.contains("alteration")) {
                if (fullBizText.contains("tailor") || fullBizText.contains("alteration") || fullBizText.contains("suit")) return true
            }
            if (subClean.contains("auto") || subClean.contains("gas station") || fullBizText.contains("petrol")) {
                if (fullBizText.contains("auto") || fullBizText.contains("car") || fullBizText.contains("garage") || fullBizText.contains("gas") || fullBizText.contains("fuel")) return true
            }
            if (subClean.contains("library")) {
                if (fullBizText.contains("library") || fullBizText.contains("reading")) return true
            }
            if (subClean.contains("community")) {
                if (fullBizText.contains("community") || fullBizText.contains("cultural") || fullBizText.contains("trek")) return true
            }
            if (subClean.contains("police")) {
                if (fullBizText.contains("police") || fullBizText.contains("security")) return true
            }
            if (subClean.contains("vending") || subClean.contains("kiosk")) {
                if (fullBizText.contains("vending") || fullBizText.contains("kiosk") || fullBizText.contains("street food") || fullBizText.contains("cart")) return true
            }
            return false
        }

        // Parent Category Filtering
        val parent = PARENT_CATEGORIES.firstOrNull { it.name.equals(selectedParentCategory, ignoreCase = true) }
        if (parent != null) {
            // Direct parent match
            if (bizCatClean.contains(parent.name.lowercase())) return true

            // Match any subcategory of this parent
            for (sub in parent.subCategories) {
                if (matchesCategory(bizCategory, bizName, bizTagline, parent.name, sub)) {
                    return true
                }
            }

            // Fallback keywords for parent category
            when (parent.name) {
                RETAIL_AND_SHOPS -> {
                    val retailWords = listOf("retail", "shop", "store", "boutique", "book", "supermarket", "grocery", "flower", "florist", "pharmacy", "hardware", "pet")
                    if (retailWords.any { fullBizText.contains(it) }) return true
                }
                FOOD_AND_HOSPITALITY -> {
                    val foodWords = listOf("food", "beverage", "cafe", "coffee", "restaurant", "dining", "hotel", "hostel", "bar", "pub", "lounge", "brewery", "ice cream", "juice", "pizza", "bistro", "nightlife")
                    if (foodWords.any { fullBizText.contains(it) }) return true
                }
                HEALTH_AND_WELLNESS -> {
                    val healthWords = listOf("health", "wellness", "fitness", "gym", "yoga", "hair", "salon", "barber", "nail", "spa", "dental", "clinic", "optician", "eyewear", "crossfit")
                    if (healthWords.any { fullBizText.contains(it) }) return true
                }
                SERVICES_AND_UTILITIES -> {
                    val serviceWords = listOf("service", "utility", "bank", "atm", "laundry", "dry cleaner", "post", "courier", "tailor", "shoe repair", "locksmith", "auto repair", "gas station", "garage")
                    if (serviceWords.any { fullBizText.contains(it) }) return true
                }
                PUBLIC_AND_COMMUNITY -> {
                    val publicWords = listOf("public", "community", "space", "library", "police", "vending", "kiosk", "park", "trekkers", "treks", "adventure")
                    if (publicWords.any { fullBizText.contains(it) }) return true
                }
            }
            return false
        }

        return true
    }
}
