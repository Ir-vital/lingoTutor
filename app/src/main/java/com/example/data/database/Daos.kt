package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE sessionType = :sessionType ORDER BY timestamp ASC")
    fun getMessagesBySessionType(sessionType: String): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatEntity)

    @Query("DELETE FROM chat_messages WHERE sessionType = :sessionType")
    suspend fun clearMessagesBySessionType(sessionType: String)
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary ORDER BY word ASC")
    fun getAllVocabularyFlow(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary ORDER BY word ASC")
    suspend fun getAllVocabulary(): List<VocabularyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(vocab: VocabularyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVocabulary(vocabList: List<VocabularyEntity>)

    @Update
    suspend fun updateVocabulary(vocab: VocabularyEntity)

    @Query("DELETE FROM vocabulary WHERE word = :word")
    suspend fun deleteVocabularyByWord(word: String)
}

@Dao
interface LessonProgressDao {
    @Query("SELECT * FROM lesson_progress ORDER BY timestamp DESC")
    fun getAllProgressFlow(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress ORDER BY timestamp DESC")
    suspend fun getAllProgress(): List<LessonProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: LessonProgressEntity)
}
