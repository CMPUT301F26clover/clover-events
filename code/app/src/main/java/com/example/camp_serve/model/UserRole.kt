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
         * Queries the "users" collection in Cloud Firestore by deviceId,
         * reads role flags (`isEntrant`, `isOwner`, `isAdmin`), and maps them to a set of [UserRole]s.
         * Defaults to `setOf(ENTRANT)` if the document or role fields fail to load.
         *
         * @param deviceId ID of the user device
         * @param db Firestore instance to use
         * @return set of assigned [UserRole]s
         */
        suspend fun determineUserRoles(
            deviceId: String,
            db: FirebaseFirestore = FirebaseFirestore.getInstance()
        ): Set<UserRole> {
            return try {
                val snapshot = db.collection("users").document(deviceId).get().await()
                if (snapshot.exists()) {
                    fromDocument(snapshot)
                } else {
                    setOf(ENTRANT)
                }
            } catch (_: Exception) {
                // Default to ENTRANT if error occurs or document doesn't exist
                setOf(ENTRANT)
            }
        }
    }
}
