package ru.dvfu.appliances.model.utils

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.firestore

class FirestoreCollections {

    private val firestore get() = Firebase.firestore

    fun users(): CollectionReference = firestore.collection(USERS)
    fun appliances(): CollectionReference = firestore.collection(APPLIANCES)
    fun events(): CollectionReference = firestore.collection(EVENTS)
    fun booking(): CollectionReference = firestore.collection(BOOKING)

    companion object {
        private const val USERS = "users"
        private const val APPLIANCES = "appliances"
        private const val BOOKING = "booking"
        private const val EVENTS = "events"
    }
}
