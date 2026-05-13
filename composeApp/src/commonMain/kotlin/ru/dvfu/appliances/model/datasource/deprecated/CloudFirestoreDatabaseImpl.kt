package ru.dvfu.appliances.model.datasource.deprecated

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import ru.dvfu.appliances.model.repository.Repository
import ru.dvfu.appliances.model.utils.FirestoreCollections

class CloudFirestoreDatabaseImpl(
    private val collections: FirestoreCollections,
) : Repository {

    private val realtimeDatabase by lazy {
        Firebase.database("https://schedule-4c151-default-rtdb.europe-west1.firebasedatabase.app/")
    }
}
