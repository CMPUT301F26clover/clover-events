package com.example.camp_serve.model

/**
 * One entry in the admin message log (US 03.12.01): a message a campsite owner
 * sent to an entrant who reserved their campsite.
 * Every field has a default value so Firestore can turn it back into a log.
 *
 * @property id unique id of the log entry
 * @property ownerId id of the campsite owner who sent the message
 * @property ownerName name of the owner, saved so the admin list needs no lookup
 * @property entrantId id of the entrant who received the message
 * @property entrantName name of the entrant, saved so the admin list needs no lookup
 * @property campsite name of the campsite the message is about
 * @property content text of the message
 * @property sentAt when it was sent, in epoch milliseconds
 */
data class MessageLog(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val entrantId: String = "",
    val entrantName: String = "",
    val campsite: String = "",
    val content: String = "",
    val sentAt: Long = 0L
)