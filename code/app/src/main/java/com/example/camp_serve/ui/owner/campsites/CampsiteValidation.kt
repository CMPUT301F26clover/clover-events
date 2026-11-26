package com.example.camp_serve.ui.owner.campsites

/*
 * Simple checks for the register/edit campsite form.
 */

// name and location can't be empty
fun isValidText(text: String): Boolean {
    return text.trim() != ""
}

/** Returns whether the input is a latitude in decimal degrees. */
fun isValidLatitude(latitude: String): Boolean {
    val number = latitude.trim().toDoubleOrNull() ?: return false
    return number.isFinite() && number in -90.0..90.0
}

/** Returns whether the input is a longitude in decimal degrees. */
fun isValidLongitude(longitude: String): Boolean {
    val number = longitude.trim().toDoubleOrNull() ?: return false
    return number.isFinite() && number in -180.0..180.0
}

// max occupancy has to be a whole number that's at least 1
fun isValidOccupancy(occupancy: String): Boolean {
    val number = occupancy.trim().toIntOrNull()
    if (number == null) {
        return false
    }
    return number >= 1
}

// price has to be a number that's 0 or more
fun isValidPrice(price: String): Boolean {
    val number = price.trim().toDoubleOrNull()
    if (number == null) {
        return false
    }
    return number >= 0
}