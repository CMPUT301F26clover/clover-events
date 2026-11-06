package com.example.camp_serve.model

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Represents the role of the user on the device.
 */
enum class UserRole {
    ENTRANT,
    ADMIN,
    CAMPSITEOWNER;

    companion object {
        /**
         * Helper to check if a Firestore field value represents true / 1.
         */
        fun isFlagTrue(value: Any?): Boolean {
            return when (value) {
                is Boolean -> value
                is Number -> value.toLong() == 1L
                is String -> value.trim() == "1" || value.trim().equals("true", ignoreCase = true)
                else -> false
            }
        }

        /**
         * Maps a role string (lowercase/trimmed) to a [UserRole].
         * Defaults to [ENTRANT] for null or unrecognized strings.
         */
        fun fromRoleString(roleStr: String?): UserRole {
            return when (roleStr?.lowercase()?.trim()) {
                "admin" -> ADMIN
                "campsiteowner", "campsite_owner", "owner" -> CAMPSITEOWNER
                "entrant" -> ENTRANT
                else -> ENTRANT
            }
        }

        /**
         * Extracts all assigned [UserRole]s from a Firestore document snapshot based on
         * `isEntrant`, `isOwner`, and `isAdmin` flags (1 or true).
         */
        fun fromDocument(snapshot: DocumentSnapshot): Set<UserRole> {
            val roles = mutableSetOf<UserRole>()
            if (isFlagTrue(snapshot.get("isEntrant"))) roles.add(ENTRANT)
            if (isFlagTrue(snapshot.get("isOwner")) || isFlagTrue(snapshot.get("isCampsiteOwner"))) roles.add(CAMPSITEOWNER)
            if (isFlagTrue(snapshot.get("isAdmin"))) roles.add(ADMIN)

            // puts legacy format (without flags) into UserRole set
            if (roles.isEmpty()) {
                val roleStr = snapshot.getString("role")
                if (roleStr != null) {
                    roles.add(fromRoleString(roleStr))
                } else {
                    roles.add(ENTRANT)
                }
            }
            return roles
        }

        /**
         * Looks up the user document for [deviceId] using [loadUser] and maps it to roles.
         * Defaults to `setOf(ENTRANT)` if the document is missing or the lookup fails.
         *
         * @param deviceId ID of the user device
         * @param loadUser fetches the user's document; defaults to Firestore "users/{deviceId}"
         */
        suspend fun determineUserRoles(
            deviceId: String,
            loadUser: suspend (String) -> DocumentSnapshot? = { id ->
                FirebaseFirestore.getInstance().collection("users").document(id).get().await()
            }
        ): Set<UserRole> {
            return try {
                val snapshot = loadUser(deviceId)
                if (snapshot != null && snapshot.exists()) fromDocument(snapshot) else setOf(ENTRANT)
            } catch (_: Exception) {
                setOf(ENTRANT)
            }
        }
        suspend fun determineUserRoles(deviceId: String, db: FirebaseFirestore): Set<UserRole> =
            determineUserRoles(deviceId) { id -> db.collection("users").document(id).get().await() }
    }
}
