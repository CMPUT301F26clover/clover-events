package com.example.camp_serve.model

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/*
 * UserRole.kt works out which roles (entrant, campsite owner, admin) a device has.
 * Every new device is saved as an entrant and campsite owner. Admin is only given
 * by setting isAdmin in the device's users document in Firestore.
 * Outstanding issues: none.
 */

/**
 * Represents the role of the user on the device.
 */
enum class UserRole {
    ENTRANT,
    ADMIN,
    CAMPSITEOWNER;

    companion object {

        /** Roles every new device gets. Admin is only added by hand in Firestore. */
        val DEFAULT_ROLES = setOf(ENTRANT, CAMPSITEOWNER)

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
         * Looks up the user document for [deviceId] and maps it to roles.
         * If the device has no document yet, one is created with entrant and
         * campsite owner roles, and [DEFAULT_ROLES] is returned.
         * If the lookup fails, [DEFAULT_ROLES] is returned.
         *
         * @param deviceId ID of the user device
         * @param loadUser fetches the user's document; defaults to Firestore "users/{deviceId}"
         * @param createUser saves a new user document; defaults to Firestore "users/{deviceId}"
         * @return the roles this device has
         */
        suspend fun determineUserRoles(
            deviceId: String,
            loadUser: suspend (String) -> DocumentSnapshot? = { id ->
                FirebaseFirestore.getInstance().collection("users").document(id).get().await()
            },
            createUser: suspend (String) -> Unit = { id ->
                val newUser = hashMapOf("isEntrant" to true, "isOwner" to true)
                FirebaseFirestore.getInstance().collection("users").document(id).set(newUser).await()
            }
        ): Set<UserRole> {
            return try {
                val snapshot = loadUser(deviceId)
                if (snapshot != null && snapshot.exists()) {
                    // user already exists, so use the roles saved in Firestore
                    fromDocument(snapshot)
                } else {
                    // first time this device opens the app, so save it as entrant + owner
                    createUser(deviceId)
                    DEFAULT_ROLES
                }
            } catch (_: Exception) {
                // if Firestore fails (e.g. no internet), still let them use both views
                DEFAULT_ROLES
            }
        }

        /**
         * Same as above, but uses the given Firestore instance.
         *
         * @param deviceId ID of the user device
         * @param db the Firestore instance to use
         * @return the roles this device has
         */
        suspend fun determineUserRoles(deviceId: String, db: FirebaseFirestore): Set<UserRole> =
            determineUserRoles(
                deviceId,
                loadUser = { id -> db.collection("users").document(id).get().await() },
                createUser = { id ->
                    val newUser = hashMapOf("isEntrant" to true, "isOwner" to true)
                    db.collection("users").document(id).set(newUser).await()
                }
            )
    }
}