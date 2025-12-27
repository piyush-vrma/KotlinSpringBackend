package com.v.industries.KotlinSpringBackend.controllers

import com.v.industries.KotlinSpringBackend.controllers.NoteController.NoteResponse
import com.v.industries.KotlinSpringBackend.database.model.Note
import com.v.industries.KotlinSpringBackend.database.repository.NoteRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/notes")
class NoteController(
    private val noteRepository: NoteRepository
) {
    data class NoteRequest(
        val id: String?,
        @field:NotBlank(message = "Title can't be blank!")
        val title: String,
        val content: String,
        val color: Long,
    )

    data class NoteResponse(
        val id: String,
        val title: String,
        val content: String,
        val color: Long,
        val createdAt: Instant
    )

    // POST http://localhost:8080/notes

    // Upsert method: create new or update existing Note
    @PostMapping
    fun save(
        @Valid @RequestBody body: NoteRequest
    ): NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val note = noteRepository.save(
            Note(
                id = body.id?.let { ObjectId(it) } ?: ObjectId.get(),
                title = body.title,
                content = body.content,
                color = body.color,
                createdAt = Instant.now(),
                ownerId = ObjectId(ownerId)
            )
        )
        return note.toResponse()
    }

    // GET http://localhost:8080/notes

    @GetMapping
    fun findByOwnerId(): List<NoteResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        return noteRepository.findByOwnerId(ObjectId(ownerId)).map { note ->
            note.toResponse()
        }.toList()
    }

    // DELETE http://localhost:8080/notes/123

    @DeleteMapping(path = ["/{id}"])
    fun deleteById(@PathVariable id: String) {
        val note = noteRepository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Note not found")
        }
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        if (note.ownerId.toHexString() == ownerId) {
            noteRepository.deleteById(ObjectId(id))
        }
    }
}

private fun Note.toResponse(): NoteResponse {
    return NoteResponse(
        id = id.toHexString(),
        title = title,
        content = content,
        color = color,
        createdAt = createdAt
    )
}