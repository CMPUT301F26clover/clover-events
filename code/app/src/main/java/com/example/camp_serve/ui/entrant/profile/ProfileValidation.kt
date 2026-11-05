package com.example.camp_serve.ui.entrant.profile

/*
 * ProfileValidation.kt holds the checks for the entrant profile form
 * (name, email, phone) used by ProfileScreen (US 01.07.03).
 * Outstanding issues: none.
 */
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

// True if email looks like name@domain.tld. Email is required
fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

// True if phone is empty (phone is optional) or has 10 to 15 digits and only
// phone characters
fun isValidPhone(phone: String): Boolean {
    if (phone.isBlank()) return true
    val digits = phone.count { it.isDigit() }
    return digits in 10..15 && phone.all {it.isDigit() || it in "+-() "}
}

