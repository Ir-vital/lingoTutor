package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.database.ChatEntity
import com.example.data.database.VocabularyEntity
import com.example.data.model.Lesson
import com.example.data.model.LessonData
import com.example.data.repository.TutorRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ActiveTab {
    DASHBOARD, VOICE, GROUP_CHAT
}

class TutorViewModel(private val repository: TutorRepository) : ViewModel() {
    private val TAG = "TutorViewModel"

    // --- General States ---
    private val _activeTab = MutableStateFlow(ActiveTab.DASHBOARD)
    val activeTab: StateFlow<ActiveTab> = _activeTab.asStateFlow()

    private val _activeLesson = MutableStateFlow<Lesson?>(null)
    val activeLesson: StateFlow<Lesson?> = _activeLesson.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow(false)
    val apiKeyStatus: StateFlow<Boolean> = _apiKeyStatus.asStateFlow()

    // --- Dashboard & Vocabulary ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val vocabularyList: StateFlow<List<VocabularyEntity>> = repository.allVocabulary
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.word.contains(query, ignoreCase = true) ||
                    it.definition.contains(query, ignoreCase = true) ||
                    it.translation.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lessonProgress = repository.lessonProgress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val completedLessonIds = lessonProgress.map { progressList ->
        progressList.map { it.lessonId }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val totalXp = lessonProgress.map { progressList ->
        progressList.sumOf { it.xpEarned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streakDays = lessonProgress.map { progressList ->
        calculateStreak(progressList.map { it.timestamp })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Voice Assistant States ---
    val voiceMessages = repository.voiceMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _tutorIsThinking = MutableStateFlow(false)
    val tutorIsThinking: StateFlow<Boolean> = _tutorIsThinking.asStateFlow()

    // Event flow to play TTS audio in the UI
    private val _ttsPlayEvent = MutableSharedFlow<String>()
    val ttsPlayEvent = _ttsPlayEvent.asSharedFlow()

    // --- Group Chat States ---
    val groupMessages = repository.groupMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedTopic = MutableStateFlow("Making New Friends")
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    private val _groupIsThinking = MutableStateFlow(false)
    val groupIsThinking: StateFlow<Boolean> = _groupIsThinking.asStateFlow()

    private val _chatInputText = MutableStateFlow("")
    val chatInputText: StateFlow<String> = _chatInputText.asStateFlow()

    init {
        checkApiKey()
        viewModelScope.launch {
            repository.seedVocabularyIfNeeded()
        }
    }

    private fun checkApiKey() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        _apiKeyStatus.value = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"
    }

    fun setActiveTab(tab: ActiveTab) {
        _activeTab.value = tab
    }

    fun selectLesson(lesson: Lesson?) {
        _activeLesson.value = lesson
    }

    fun setVocabularySearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Voice Tutor Actions ---

    fun toggleRecording(currentlyRecording: Boolean) {
        _isRecording.value = currentlyRecording
    }

    fun handleUserVoiceSpeech(transcribedText: String) {
        if (transcribedText.isBlank()) return

        viewModelScope.launch {
            // 1. Save user's spoken message in database
            repository.saveMessage(
                ChatEntity(
                    sessionType = "VOICE",
                    sender = "USER",
                    text = transcribedText
                )
            )

            // 2. Set thinking state
            _tutorIsThinking.value = true

            // 3. Request Gemini voice response
            val result = repository.getVoiceTutorReply(transcribedText)
            _tutorIsThinking.value = false

            if (result != null) {
                // 4. Save tutor's response in DB
                repository.saveMessage(
                    ChatEntity(
                        sessionType = "VOICE",
                        sender = "TUTOR",
                        text = result.reply,
                        correctedText = result.correctedText,
                        pronunciationFeedback = result.pronunciationTips,
                        feedback = result.feedback
                    )
                )

                // 5. Trigger Text To Speech in UI
                _ttsPlayEvent.emit(result.reply)
            } else {
                repository.saveMessage(
                    ChatEntity(
                        sessionType = "VOICE",
                        sender = "TUTOR",
                        text = "I'm sorry, I'm having trouble connecting to my brain right now. Please check if your Gemini API key is configured!"
                    )
                )
            }
        }
    }

    fun clearVoiceChat() {
        viewModelScope.launch {
            repository.clearMessages("VOICE")
        }
    }

    // --- Group Chat Actions ---

    fun setGroupTopic(topic: String) {
        _selectedTopic.value = topic
        viewModelScope.launch {
            repository.clearMessages("GROUP")
            // Add a warm introduction greeting from Sarah to kickstart the chat
            repository.saveMessage(
                ChatEntity(
                    sessionType = "GROUP",
                    sender = "SARAH",
                    text = "Hey everyone! Stoked to talk about '$topic' today! Let's get this discussion started. What are your thoughts?"
                )
            )
        }
    }

    fun updateChatInput(text: String) {
        _chatInputText.value = text
    }

    fun sendGroupChatMessage() {
        val message = _chatInputText.value
        if (message.isBlank()) return

        _chatInputText.value = ""

        viewModelScope.launch {
            // 1. Save user message to group db
            repository.saveMessage(
                ChatEntity(
                    sessionType = "GROUP",
                    sender = "USER",
                    text = message
                )
            )

            // 2. Trigger thinking
            _groupIsThinking.value = true

            // 3. Get reply sequence from Gemini API
            val result = repository.getGroupChatReplies(_selectedTopic.value, message)
            _groupIsThinking.value = false

            if (result != null) {
                // Save each character's reply sequentially
                result.replies.forEach { reply ->
                    repository.saveMessage(
                        ChatEntity(
                            sessionType = "GROUP",
                            sender = reply.sender.uppercase(),
                            text = reply.text,
                            frenchTranslation = reply.frenchTranslation,
                            educationalNote = reply.educationalNote
                        )
                    )
                }
            } else {
                repository.saveMessage(
                    ChatEntity(
                        sessionType = "GROUP",
                        sender = "SARAH",
                        text = "Hmm, looks like our internet connection stuttered. Emily, Jean, can you hear me? Let's check the API configuration!"
                    )
                )
            }
        }
    }

    fun clearGroupChat() {
        viewModelScope.launch {
            repository.clearMessages("GROUP")
            setGroupTopic(_selectedTopic.value) // re-trigger greeting
        }
    }

    // --- Dictionary / Custom Vocabulary ---

    fun addCustomWord(word: String, def: String, trans: String, ex: String) {
        if (word.isBlank() || def.isBlank() || trans.isBlank()) return
        viewModelScope.launch {
            repository.addCustomVocabulary(
                VocabularyEntity(
                    word = word.trim(),
                    definition = def.trim(),
                    translation = trans.trim(),
                    exampleSentence = ex.trim(),
                    isLearned = false,
                    isCustom = true
                )
            )
        }
    }

    fun deleteVocabularyWord(word: String) {
        viewModelScope.launch {
            repository.deleteVocabulary(word)
        }
    }

    fun toggleVocabularyFavorite(word: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleVocabularyFavorite(word, isFavorite)
        }
    }

    fun toggleVocabularyLearned(word: String, isLearned: Boolean) {
        viewModelScope.launch {
            repository.markVocabularyLearned(word, isLearned)
        }
    }

    // --- Practice / Completing Lessons ---

    fun simulateCompleteLesson(lessonId: String, title: String, xp: Int, score: Int) {
        viewModelScope.launch {
            repository.completeLesson(lessonId, title, xp, score)
        }
    }

    // --- Helper calculations ---

    private fun calculateStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val sortedDates = timestamps.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        var currentStreak = 0
        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        var targetTime = todayCal.timeInMillis

        // If the user's latest completion wasn't today or yesterday, streak is broken
        if (sortedDates.first() != targetTime) {
            val yesterdayCal = Calendar.getInstance()
            yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
            yesterdayCal.set(Calendar.HOUR_OF_DAY, 0)
            yesterdayCal.set(Calendar.MINUTE, 0)
            yesterdayCal.set(Calendar.SECOND, 0)
            yesterdayCal.set(Calendar.MILLISECOND, 0)
            if (sortedDates.first() != yesterdayCal.timeInMillis) {
                return 0
            } else {
                targetTime = yesterdayCal.timeInMillis
            }
        }

        for (date in sortedDates) {
            if (date == targetTime) {
                currentStreak++
                // Set target to previous day
                val targetCal = Calendar.getInstance()
                targetCal.timeInMillis = targetTime
                targetCal.add(Calendar.DAY_OF_YEAR, -1)
                targetTime = targetCal.timeInMillis
            } else if (date < targetTime) {
                break
            }
        }
        return currentStreak
    }
}

class TutorViewModelFactory(private val repository: TutorRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TutorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TutorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
