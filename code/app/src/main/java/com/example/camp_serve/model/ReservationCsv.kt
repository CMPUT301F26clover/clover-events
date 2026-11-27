package com.example.camp_serve.model

/*
 * ReservationCsv.kt
 * Builds a downloadable CSV confirmation for a reservation (US 01.05.02)
 * or a cancellation (US 01.07.02).
 * Outstanding issues: none.
 */

/**
 * Checks if a reservation was cancelled by the entrant or rejected by the owner.
 *
 * @param reservation the reservation to check
 * @return true if its status is "cancelled" or "rejected"
 */
fun isCancellation(reservation: Reservation): Boolean {
    return reservation.status == "cancelled" || reservation.status == "rejected"
}

/**
 * Makes one value safe for a CSV file. If it has a comma, quote or new line,
 * it gets wrapped in quotes (and any quotes inside are doubled), so it stays
 * in one column.
 *
 * @param value the text to put in the CSV
 * @return the safe version of the text
 */
fun csvValue(value: String): String {
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
    return value
}

/**
 * Builds the CSV text for a reservation: a header line, then one line of values.
 * Cancelled or rejected reservations are labelled as a cancellation confirmation.
 *
 * @param reservation the reservation to put in the CSV
 * @return the full CSV text
 */
fun reservationToCsv(reservation: Reservation): String {
    var type = "Reservation confirmation"
    if (isCancellation(reservation)) {
        type = "Cancellation confirmation"
    }

    val header = "Type,Confirmation number,Campsite,Check-in,Check-out,Booked by,Status"
    val row = csvValue(type) + "," +
            csvValue(reservation.id) + "," +
            csvValue(reservation.campsiteName) + "," +
            csvValue(reservation.startDate) + "," +
            csvValue(reservation.endDate) + "," +
            csvValue(reservation.entrantName) + "," +
            csvValue(reservation.status)

    return header + "\n" + row + "\n"
}

/**
 * Picks a file name for the CSV, e.g. "reservation_res-01.csv" or "cancellation_res-03.csv".
 *
 * @param reservation the reservation the file is for
 * @return the file name
 */
fun csvFileName(reservation: Reservation): String {
    if (isCancellation(reservation)) {
        return "cancellation_" + reservation.id + ".csv"
    }
    return "reservation_" + reservation.id + ".csv"
}