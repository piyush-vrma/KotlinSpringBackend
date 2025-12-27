package com.v.industries.KotlinSpringBackend.database.repository

import com.v.industries.KotlinSpringBackend.database.model.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface NoteRepository : MongoRepository<Note, ObjectId> {
    fun findByOwnerId(ownerId: ObjectId): List<Note>
}