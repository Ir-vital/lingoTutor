package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionType: String, // "VOICE" or "GROUP"
    val sender: String,      // "USER", "SARAH", "EMILY", "JEAN", "TUTOR"
    val text: String,
    val correctedText: String? = null,
    val pronunciationFeedback: String? = null,
    val feedback: String? = null,
    val frenchTranslation: String? = null,
    val educationalNote: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey val word: String,
    val definition: String,
    val translation: String,
    val exampleSentence: String,
    val isLearned: Boolean = false,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val title: String,
    val xpEarned: Int,
    val score: Int, // percentage 0-100
    val timestamp: Long = System.currentTimeMillis()
)
