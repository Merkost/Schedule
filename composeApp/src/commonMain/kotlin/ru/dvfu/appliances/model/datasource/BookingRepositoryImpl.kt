package ru.dvfu.appliances.model.datasource

import ru.dvfu.appliances.model.repository.BookingRepository
import ru.dvfu.appliances.model.utils.FirestoreCollections

class BookingRepositoryImpl(
    private val collections: FirestoreCollections,
) : BookingRepository
