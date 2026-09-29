package com.example.data.model

data class CountryCode(
    val name: String,
    val dialCode: String,
    val flagEmoji: String,
    val isoCode: String
)

object CountryCodeList {
    val allCountries: List<CountryCode> = listOf(
        CountryCode("United States", "+1", "🇺🇸", "US"),
        CountryCode("India", "+91", "🇮🇳", "IN"),
        CountryCode("United Kingdom", "+44", "🇬🇧", "GB"),
        CountryCode("Canada", "+1", "🇨🇦", "CA"),
        CountryCode("Australia", "+61", "🇦🇺", "AU"),
        CountryCode("Germany", "+49", "🇩🇪", "DE"),
        CountryCode("France", "+33", "🇫🇷", "FR"),
        CountryCode("Brazil", "+55", "🇧🇷", "BR"),
        CountryCode("Mexico", "+52", "🇲🇽", "MX"),
        CountryCode("United Arab Emirates", "+971", "🇦🇪", "AE"),
        CountryCode("Saudi Arabia", "+966", "🇸🇦", "SA"),
        CountryCode("Singapore", "+65", "🇸🇬", "SG"),
        CountryCode("Japan", "+81", "🇯🇵", "JP"),
        CountryCode("South Korea", "+82", "🇰🇷", "KR"),
        CountryCode("Indonesia", "+62", "🇮🇩", "ID"),
        CountryCode("Philippines", "+63", "🇵🇭", "PH"),
        CountryCode("Nigeria", "+234", "🇳🇬", "NG"),
        CountryCode("South Africa", "+27", "🇿🇦", "ZA"),
        CountryCode("Kenya", "+254", "🇰🇪", "KE"),
        CountryCode("Spain", "+34", "🇪🇸", "ES"),
        CountryCode("Italy", "+39", "🇮🇹", "IT"),
        CountryCode("Netherlands", "+31", "🇳🇱", "NL"),
        CountryCode("Switzerland", "+41", "🇨🇭", "CH"),
        CountryCode("Sweden", "+46", "🇸🇪", "SE"),
        CountryCode("Norway", "+47", "🇳🇴", "NO"),
        CountryCode("Denmark", "+45", "🇩🇰", "DK"),
        CountryCode("Finland", "+358", "🇫🇮", "FI"),
        CountryCode("Ireland", "+353", "🇮🇪", "IE"),
        CountryCode("New Zealand", "+64", "🇳🇿", "NZ"),
        CountryCode("Turkey", "+90", "🇹🇷", "TR"),
        CountryCode("Egypt", "+20", "🇪🇬", "EG"),
        CountryCode("Pakistan", "+92", "🇵🇰", "PK"),
        CountryCode("Bangladesh", "+880", "🇧🇩", "BD"),
        CountryCode("Nepal", "+977", "🇳🇵", "NP"),
        CountryCode("Sri Lanka", "+94", "🇱🇰", "LK"),
        CountryCode("Malaysia", "+60", "🇲🇾", "MY"),
        CountryCode("Thailand", "+66", "🇹🇭", "TH"),
        CountryCode("Vietnam", "+84", "🇻🇳", "VN"),
        CountryCode("Argentina", "+54", "🇦🇷", "AR"),
        CountryCode("Colombia", "+57", "🇨🇴", "CO"),
        CountryCode("Chile", "+56", "🇨🇱", "CL"),
        CountryCode("Peru", "+51", "🇵🇪", "PE"),
        CountryCode("Poland", "+48", "🇵🇱", "PL"),
        CountryCode("Portugal", "+351", "🇵🇹", "PT"),
        CountryCode("Austria", "+43", "🇦🇹", "AT"),
        CountryCode("Belgium", "+32", "🇧🇪", "BE"),
        CountryCode("Greece", "+30", "🇬🇷", "GR"),
        CountryCode("Israel", "+972", "🇮🇱", "IL"),
        CountryCode("Qatar", "+974", "🇶🇦", "QA"),
        CountryCode("Kuwait", "+965", "🇰🇼", "KW"),
        CountryCode("Oman", "+968", "🇴🇲", "OM"),
        CountryCode("Bahrain", "+973", "🇧🇭", "BH"),
        CountryCode("Ghana", "+233", "🇬🇭", "GH"),
        CountryCode("Morocco", "+212", "🇲🇦", "MA"),
        CountryCode("Ukraine", "+380", "🇺🇦", "UA"),
        CountryCode("Romania", "+40", "🇷🇴", "RO"),
        CountryCode("Czech Republic", "+420", "🇨🇿", "CZ"),
        CountryCode("Hungary", "+36", "🇭🇺", "HU")
    )

    val popularCountries: List<CountryCode> = listOf(
        allCountries[0], // US
        allCountries[1], // IN
        allCountries[2], // GB
        allCountries[3], // CA
        allCountries[4], // AU
        allCountries[5], // DE
        allCountries[9], // AE
        allCountries[7]  // BR
    )
}
