package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class MessengerProfile(
    val uid: String,
    val username: String,
    val displayName: String,
    val email: String
)

data class MessengerConversation(
    val id: String,
    val otherUid: String,
    val otherUsername: String,
    val otherDisplayName: String,
    val lastMessage: String,
    val lastMessageAt: Long
)

data class MessengerMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val status: String
)

class FirebaseMessengerRepository(context: Context) {
    private val app = FirebaseApp.getApps(context).firstOrNull()
    val isConfigured: Boolean get() = app != null

    private val auth: FirebaseAuth? = app?.let { FirebaseAuth.getInstance(it) }
    private val db: FirebaseFirestore? = app?.let { FirebaseFirestore.getInstance(it) }

    fun authState(): Flow<MessengerProfile?> = callbackFlow {
        val a = auth
        if (a == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        var registration: ListenerRegistration? = null
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                trySend(null)
            } else {
                registration?.remove()
                registration = db?.collection("users")?.document(uid)?.addSnapshotListener { snap, _ ->
                    val data = snap?.data
                    trySend(
                        if (data == null) null else MessengerProfile(
                            uid = uid,
                            username = data["username"] as? String ?: "",
                            displayName = data["displayName"] as? String ?: "",
                            email = data["email"] as? String ?: ""
                        )
                    )
                }
            }
        }
        a.addAuthStateListener(listener)
        awaitClose {
            registration?.remove()
            a.removeAuthStateListener(listener)
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String, username: String): Result<Unit> = runCatching {
        require(isConfigured) { "Firebase is not connected. Add google-services.json first." }
        val cleanUsername = normalizeUsername(username)
        require(cleanUsername.length in 4..24) { "Username must be 4-24 characters." }
        require(cleanUsername.all { it.isLetterOrDigit() || it == '_' || it == '.' }) { "Username can use letters, numbers, _ and . only." }
        val user = auth!!.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Account creation failed")
        val usernameRef = db!!.collection("usernames").document(cleanUsername)
        val userRef = db.collection("users").document(user.uid)
        try {
            db.runTransaction { tx ->
                if (tx.get(usernameRef).exists()) {
                    throw IllegalStateException("Username is already taken.")
                }
                tx.set(usernameRef, mapOf("uid" to user.uid))
                tx.set(userRef, mapOf(
                    "uid" to user.uid,
                    "username" to cleanUsername,
                    "displayName" to displayName.trim().ifBlank { cleanUsername },
                    "email" to email.trim(),
                    "createdAt" to System.currentTimeMillis()
                ))
                null
            }.await()
            saveFcmToken()
        } catch (e: Exception) {
            runCatching { user.delete().await() }
            throw e
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        require(isConfigured) { "Firebase is not connected. Add google-services.json first." }
        auth!!.signInWithEmailAndPassword(email.trim(), password).await()
        saveFcmToken()
    }

    fun signOut() {
        auth?.signOut()
    }

    suspend fun saveFcmToken() {
        val uid = auth?.currentUser?.uid ?: return
        val token = runCatching { FirebaseMessaging.getInstance(app!!).token.await() }.getOrNull() ?: return
        db?.collection("users")?.document(uid)?.update("fcmToken", token)?.await()
    }

    suspend fun findUserByUsername(input: String): Result<MessengerProfile?> = runCatching {
        require(isConfigured) { "Firebase is not connected." }
        val username = normalizeUsername(input)
        val usernameSnap = db!!.collection("usernames").document(username).get().await()
        val uid = usernameSnap.getString("uid") ?: return@runCatching null
        val snap = db.collection("users").document(uid).get().await()
        if (!snap.exists()) return@runCatching null
        MessengerProfile(
            uid = uid,
            username = snap.getString("username") ?: username,
            displayName = snap.getString("displayName") ?: username,
            email = snap.getString("email") ?: ""
        )
    }

    suspend fun openConversation(other: MessengerProfile): Result<String> = runCatching {
        val me = auth?.currentUser?.uid ?: error("Sign in required")
        require(me != other.uid) { "You cannot message yourself." }
        val id = conversationId(me, other.uid)
        val ref = db!!.collection("conversations").document(id)
        val existing = ref.get().await()
        if (!existing.exists()) {
            ref.set(mapOf(
                "participants" to listOf(me, other.uid),
                "participantProfiles" to mapOf(
                    me to mapOf("username" to (auth?.currentUser?.email ?: me), "displayName" to "Me"),
                    other.uid to mapOf("username" to other.username, "displayName" to other.displayName)
                ),
                "lastMessage" to "",
                "lastMessageAt" to 0L,
                "createdAt" to System.currentTimeMillis()
            )).await()
        }
        id
    }

    fun conversations(): Flow<List<MessengerConversation>> = callbackFlow {
        val uid = auth?.currentUser?.uid
        val firestore = db
        if (uid == null || firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = firestore.collection("conversations")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val participants = doc.get("participants") as? List<*> ?: return@mapNotNull null
                    val otherUid = participants.filterIsInstance<String>().firstOrNull { it != uid } ?: return@mapNotNull null
                    val profiles = doc.get("participantProfiles") as? Map<*, *>
                    val other = profiles?.get(otherUid) as? Map<*, *>
                    MessengerConversation(
                        id = doc.id,
                        otherUid = otherUid,
                        otherUsername = other?.get("username") as? String ?: otherUid,
                        otherDisplayName = other?.get("displayName") as? String ?: "Contact",
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageAt = doc.getLong("lastMessageAt") ?: 0L
                    )
                }.sortedByDescending { it.lastMessageAt }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun messages(conversationId: String): Flow<List<MessengerMessage>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    MessengerMessage(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: return@mapNotNull null,
                        senderName = doc.getString("senderName") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        status = doc.getString("status") ?: "SENT"
                    )
                }
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(conversationId: String, text: String): Result<Unit> = runCatching {
        val me = auth?.currentUser ?: error("Sign in required")
        val clean = text.trim()
        require(clean.isNotEmpty()) { "Message cannot be empty." }
        val messageRef = db!!.collection("conversations").document(conversationId).collection("messages").document()
        val conversationRef = db.collection("conversations").document(conversationId)
        db.runBatch { batch ->
            batch.set(messageRef, mapOf(
                "senderId" to me.uid,
                "senderName" to (me.displayName ?: me.email ?: "Me"),
                "text" to clean,
                "timestamp" to System.currentTimeMillis(),
                "status" to "SENT"
            ))
            batch.update(conversationRef, mapOf(
                "lastMessage" to clean,
                "lastMessageAt" to System.currentTimeMillis()
            ))
        }.await()
    }

    private fun normalizeUsername(value: String): String =
        value.trim().removePrefix("@").lowercase()

    private fun conversationId(a: String, b: String): String =
        listOf(a, b).sorted().joinToString("_")
}
