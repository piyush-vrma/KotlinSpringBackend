package com.v.industries.KotlinSpringBackend.database.repository

import com.v.industries.KotlinSpringBackend.database.model.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, ObjectId> {
    fun findByEmail(email: String): User?
}