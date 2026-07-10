package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.database.*
import com.example.data.model.LessonData
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class TutorRepository(
    private val chatDao: ChatDao,
    private val vocabularyDao: VocabularyDao,
    private val lessonProgressDao: LessonProgressDao,
    private val moshi: Moshi = RetrofitClient.genericMoshi
) {
    private val TAG = "TutorRepository"

    val voiceMessages: Flow<List<ChatEntity>> = chatDao.getMessagesBySessionType("VOICE")
    val groupMessages: Flow<List<ChatEntity>> = chatDao.getMessagesBySessionType("GROUP")
    val allVocabulary: Flow<List<VocabularyEntity>> = vocabularyDao.getAllVocabularyFlow()
    val lessonProgress: Flow<List<LessonProgressEntity>> = lessonProgressDao.getAllProgressFlow()

    // Seeds the database with default lesson vocabulary if empty
    suspend fun seedVocabularyIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val existing = vocabularyDao.getAllVocabulary()
            if (existing.isEmpty()) {
                val entities = LessonData.lessons.flatMap { lesson ->
                    lesson.vocabulary.map { item ->
                        VocabularyEntity(
                            word = item.word,
                            definition = item.definition,
                            translation = item.translation,
                            exampleSentence = item.example,
                            isLearned = false,
                            isCustom = false,
                            isFavorite = false
                        )
                    }
                }
                vocabularyDao.insertAllVocabulary(entities)
                Log.d(TAG, "Successfully seeded ${entities.size} vocabulary items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed vocabulary", e)
        }
    }

    suspend fun saveMessage(message: ChatEntity) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun clearMessages(sessionType: String) = withContext(Dispatchers.IO) {
        chatDao.clearMessagesBySessionType(sessionType)
    }

    suspend fun addCustomVocabulary(vocab: VocabularyEntity) = withContext(Dispatchers.IO) {
        vocabularyDao.insertVocabulary(vocab)
    }

    suspend fun deleteVocabulary(word: String) = withContext(Dispatchers.IO) {
        vocabularyDao.deleteVocabularyByWord(word)
    }

    suspend fun toggleVocabularyFavorite(word: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val existing = vocabularyDao.getAllVocabulary().firstOrNull { it.word == word }
        if (existing != null) {
            vocabularyDao.updateVocabulary(existing.copy(isFavorite = isFavorite))
        }
    }

    suspend fun markVocabularyLearned(word: String, isLearned: Boolean) = withContext(Dispatchers.IO) {
        val existing = vocabularyDao.getAllVocabulary().firstOrNull { it.word == word }
        if (existing != null) {
            vocabularyDao.updateVocabulary(existing.copy(isLearned = isLearned))
        }
    }

    suspend fun completeLesson(lessonId: String, title: String, xp: Int, score: Int) = withContext(Dispatchers.IO) {
        lessonProgressDao.insertProgress(
            LessonProgressEntity(
                lessonId = lessonId,
                title = title,
                xpEarned = xp,
                score = score
            )
        )
        // Auto-mark vocabulary from this lesson as "Learned"
        val lesson = LessonData.lessons.find { it.id == lessonId }
        lesson?.vocabulary?.forEach { item ->
            val existing = vocabularyDao.getAllVocabulary().find { it.word.lowercase() == item.word.lowercase() }
            if (existing != null) {
                vocabularyDao.updateVocabulary(existing.copy(isLearned = true))
            }
        }
    }

    // --- Gemini Voice Tutor API Integration ---
    suspend fun getVoiceTutorReply(userPrompt: String): VoiceTutorResponse? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "Gemini API Key is missing or default placeholder! Invoking smart fallback tutor engine.")
            return@withContext getMockVoiceTutorReply(userPrompt)
        }

        // Get past voice messages for short context
        val pastEntities = voiceMessages.firstOrNull() ?: emptyList()
        val recentHistory = pastEntities.takeLast(10)

        val contents = mutableListOf<Content>()
        recentHistory.forEach { msg ->
            val roleName = if (msg.sender == "USER") "user" else "model"
            // For voice history, we supply the actual spoken response or text
            contents.add(Content(parts = listOf(Part(text = msg.text))))
        }
        // Add the current prompt
        contents.add(Content(parts = listOf(Part(text = userPrompt))))

        val systemPrompt = """
            You are a highly realistic, patient, and encouraging English-learning tutor named 'LingoTutor AI'.
            Your target audience is French speakers wanting to master English.
            
            CRITICAL TUTORING RULES:
            1. Analyze the user's English input carefully. Check for spelling, grammar, syntax, and word choice errors.
            2. Provide helpful, kind corrections and explanations in a natural, pedagogical mixture of French and English (bilingual code-switching).
            3. In your response: Explain complex English concepts in French when necessary, but keep the flow fluid.
            4. Suggest pronunciation pitfalls based on common mistakes French speakers make with those words (e.g., mixing 'H' sounds, pronouncing 'th' as 'z', or silent letters).
            5. Always end your 'reply' with an engaging, simple, open-ended English question to keep the conversation going.
            
            Your response MUST be formatted strictly as a single JSON object with the following fields:
            {
              "feedback": "A warm, helpful explanation of grammar corrections in bilingual French/English. Keep it concise (1-2 sentences).",
              "correctedText": "The fully corrected, pristine English version of what the user said. If they made no errors, set this to null.",
              "reply": "Your actual warm spoken tutor reply to them, written in bilingual code-switching. (e.g., 'C\'est une bonne idée! But wait, we say \"on Monday\" instead of \"in Monday\". So, tell me, what are you doing on Monday?'). Keep this under 3 sentences for Text-To-Speech.",
              "pronunciationTips": "1-2 pronunciation tips based on the words they used, focused on phonetic challenges for French natives (or null)."
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Voice Tutor JSON Response: $jsonText")
                val adapter = moshi.adapter(VoiceTutorResponse::class.java)
                adapter.fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Voice Tutor response from Gemini, falling back to mock", e)
            getMockVoiceTutorReply(userPrompt)
        }
    }

    private fun getMockVoiceTutorReply(userPrompt: String): VoiceTutorResponse {
        val promptLower = userPrompt.lowercase().trim()
        val correctedText: String?
        val feedback: String?
        val reply: String
        val pronunciationTips: String?

        when {
            promptLower.contains("i am agree") || promptLower.contains("i'm agree") -> {
                correctedText = "I agree"
                feedback = "En anglais, on n'utilise pas l'auxiliaire 'be' avec le verbe 'agree'. On dit directement 'I agree' !"
                reply = "I totally understand! C'est une erreur très commune pour les francophones. So, tell me, what topic do you want to talk about today?"
                pronunciationTips = "Attention au mot 'agree' : l'accent tonique est sur la deuxième syllabe (a-GREE)."
            }
            promptLower.contains("i have") && (promptLower.contains("years") || promptLower.contains("year old")) -> {
                correctedText = "I am " + userPrompt.replace("have", "am", ignoreCase = true)
                feedback = "Pour exprimer l'âge en anglais, on utilise le verbe 'to be' (I am...) plutôt que 'to have'."
                reply = "Ah, nice to meet you! Age is just a number. Quel est votre objectif principal pour l'apprentissage de l'anglais ?"
                pronunciationTips = "Prononcez bien le 'H' aspiré dans 'have'. N'oubliez pas d'expirer doucement !"
            }
            promptLower.contains("more better") -> {
                correctedText = userPrompt.replace("more better", "much better", ignoreCase = true)
                feedback = "'Better' est déjà un comparatif de supériorité. Dire 'more better' est redondant. On dit 'much better' ou juste 'better'."
                reply = "Indeed! We want to make your English better and better! What is your favorite hobby?"
                pronunciationTips = "Dans 'better', les 't' se prononcent souvent comme un léger 'd' en anglais américain (tap T)."
            }
            promptLower.contains("hello") || promptLower.contains("hi") || promptLower.contains("hey") || promptLower.isEmpty() -> {
                correctedText = null
                feedback = "Votre salutation est impeccable ! Rien à corriger."
                reply = "Hello there! Enchanté ! Je suis ravi de parler avec vous. Tell me, how has your day been so far?"
                pronunciationTips = "Pensez à bien faire entendre le 'H' dans 'Hello' (souffle expiré de la gorge)."
            }
            else -> {
                correctedText = null
                feedback = "Excellent! Votre phrase est claire et compréhensible."
                reply = "That's very interesting! Merci de partager ça avec moi. Can you tell me more about it in English?"
                pronunciationTips = "N'oubliez pas de prononcer le 'th' correctement en plaçant le bout de la langue sous les dents du haut."
            }
        }

        return VoiceTutorResponse(
            feedback = feedback,
            correctedText = correctedText,
            reply = reply,
            pronunciationTips = pronunciationTips
        )
    }

    // --- Gemini Group Chat API Integration ---
    suspend fun getGroupChatReplies(topic: String, userPrompt: String): GroupChatResponse? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "Gemini API Key is missing or default placeholder! Invoking smart fallback group chat engine.")
            return@withContext getMockGroupReplies(topic, userPrompt)
        }

        val pastEntities = groupMessages.firstOrNull() ?: emptyList()
        val recentHistory = pastEntities.takeLast(12)

        val contents = mutableListOf<Content>()
        recentHistory.forEach { msg ->
            val prefix = "[${msg.sender}]: "
            contents.add(Content(parts = listOf(Part(text = "$prefix${msg.text}"))))
        }
        contents.add(Content(parts = listOf(Part(text = "[USER]: $userPrompt"))))

        val systemPrompt = """
            You are simulating a lively, interactive 4-person English learning discussion group.
            The current topic of discussion is: "$topic".
            
            The participants are:
            1. USER (The student learning English)
            2. Sarah (AI persona): A highly fluent, enthusiastic, and supportive female native speaker from California. She uses friendly slang like 'awesome', 'stoked', 'totally'.
            3. Emily (AI persona): A fluent, intellectual, and academic female English speaker from London. She speaks elegantly, gently points out advanced vocabulary alternatives, and is very polite.
            4. Jean (AI persona): A beginner English speaker (male) from France. He makes common French-native mistakes (e.g. using 'agree with' incorrectly, omitting 'H' sounds, or saying 'I do a mistake'). He is friendly, eager to learn, and sometimes asks Sarah or Emily to explain hard English idioms in French.
            
            DIRECTIONS FOR GENERATING THE CHAT FLOW:
            - React naturally and directly to the USER's input.
            - Generate exactly 2 or 3 responses from the other participants in a logical, lively conversational sequence. (e.g., Emily replies to USER, then Jean chimes in, or Sarah replies to USER and Jean asks Emily a question).
            - Do NOT speak as USER.
            - Ensure Jean's English contains some gentle errors, while Sarah's is natural/fluent, and Emily's is academic and rich.
            - Include helpful explanations or French translations for slang/idioms in the metadata.
            
            Your response MUST be formatted strictly as a single JSON object with the following fields:
            {
              "replies": [
                {
                  "sender": "Sarah" (or "Emily" or "Jean"),
                  "text": "The actual spoken message in the conversation.",
                  "frenchTranslation": "If they used slang, advanced idioms, or if Jean spoke a bit of French to ask a question, provide the translation (otherwise null).",
                  "educationalNote": "A brief educational tip or correction (e.g. 'Sarah used \"hit the sack\" which is an idiom meaning to go to bed') (otherwise null)."
                }
              ]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.8f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d(TAG, "Group Chat JSON Response: $jsonText")
                val adapter = moshi.adapter(GroupChatResponse::class.java)
                adapter.fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Group Chat responses from Gemini, falling back to mock", e)
            getMockGroupReplies(topic, userPrompt)
        }
    }

    private fun getMockGroupReplies(topic: String, userPrompt: String): GroupChatResponse {
        val cleanTopic = topic.lowercase()
        val replies = when {
            cleanTopic.contains("food") || cleanTopic.contains("coffee") -> {
                listOf(
                    GroupChatPersonaReply(
                        sender = "SARAH",
                        text = "Oh, I totally love black coffee with a splash of oat milk! What about you? Do you prefer sweet pastries or savory breakfast?",
                        frenchTranslation = "splash of oat milk = un soupçon de lait d'avoine",
                        educationalNote = "Sarah uses 'totally love' to show enthusiastic agreement, common in casual conversation."
                    ),
                    GroupChatPersonaReply(
                        sender = "JEAN",
                        text = "For me, I agree with Sarah! But in France, we do very good croissants. I want to eat a croissant with my espresso.",
                        frenchTranslation = "I want to eat = Je veux manger",
                        educationalNote = "Jean says 'I agree with Sarah' which is correct, but he also says 'we do very good croissants' instead of 'we make very good croissants'."
                    ),
                    GroupChatPersonaReply(
                        sender = "EMILY",
                        text = "A freshly baked croissant is indeed exquisite! In London, we have lovely tea shops as well. Do you fancy a spot of afternoon tea?",
                        frenchTranslation = "Do you fancy = Est-ce que ça te dit / aimerais-tu",
                        educationalNote = "Emily uses 'fancy a spot of afternoon tea', a classic British idiom for asking if someone wants some tea."
                    )
                )
            }
            cleanTopic.contains("healthy") || cleanTopic.contains("habits") -> {
                listOf(
                    GroupChatPersonaReply(
                        sender = "SARAH",
                        text = "I try to hit the gym three times a week! It keeps me energized. How do you stay active?",
                        frenchTranslation = "hit the gym = aller à la salle de sport",
                        educationalNote = "'Hit the gym' is a very common casual idiom for exercising at a fitness center."
                    ),
                    GroupChatPersonaReply(
                        sender = "JEAN",
                        text = "I do running sometimes, but it is very difficult. I prefer sleep 8 hours every night!",
                        frenchTranslation = "I do running = Je fais de la course",
                        educationalNote = "Jean says 'I do running' instead of 'I go running', and 'I prefer sleep' instead of 'I prefer to sleep'."
                    ),
                    GroupChatPersonaReply(
                        sender = "EMILY",
                        text = "Adequate rest is crucial for cognitive performance. I personally practice mindfulness meditation each morning. It really helps with focus.",
                        frenchTranslation = "Adequate rest = Un repos adéquat",
                        educationalNote = "Emily uses formal vocabulary like 'crucial' and 'cognitive performance' to elevate the dialogue."
                    )
                )
            }
            cleanTopic.contains("intelligence") -> {
                listOf(
                    GroupChatPersonaReply(
                        sender = "SARAH",
                        text = "AI is absolutely blowing my mind right now! It's changing everything so fast. Are you using any AI tools for work or studying?",
                        frenchTranslation = "blowing my mind = m'impressionne énormément",
                        educationalNote = "'Blowing my mind' is an idiom meaning to amaze or shock someone completely."
                    ),
                    GroupChatPersonaReply(
                        sender = "JEAN",
                        text = "Yes, AI help me write English emails. But sometimes I am afraid that robots will take my job!",
                        frenchTranslation = "robots will take my job = les robots vont prendre mon travail",
                        educationalNote = "Jean says 'AI help me' instead of 'AI helps me' (subject-verb agreement error)."
                    ),
                    GroupChatPersonaReply(
                        sender = "EMILY",
                        text = "An understandable concern, Jean. However, automation often shifts human labor toward creative, high-level analysis rather than outright replacement.",
                        frenchTranslation = "outright replacement = remplacement pur et simple",
                        educationalNote = "Emily uses sophisticated academic conjunctions and vocabulary to present a balanced perspective."
                    )
                )
            }
            else -> {
                listOf(
                    GroupChatPersonaReply(
                        sender = "SARAH",
                        text = "That is such an awesome point! I completely agree that talking about '$topic' is super exciting.",
                        frenchTranslation = "awesome point = point super intéressant",
                        educationalNote = "Sarah uses 'super exciting' to add high energy to the conversation."
                    ),
                    GroupChatPersonaReply(
                        sender = "JEAN",
                        text = "I agree too! I want to practice more English vocabulary about this topic because it is very useful.",
                        frenchTranslation = "I agree too = Je suis d'accord aussi",
                        educationalNote = "Jean says 'I agree too' instead of 'I am agree too' (which is a common error he successfully avoids here!)."
                    ),
                    GroupChatPersonaReply(
                        sender = "EMILY",
                        text = "Indeed. Exploring diverse subjects like '$topic' enables us to broaden our lexical range. What aspect of this interests you most?",
                        frenchTranslation = "broaden our lexical range = élargir notre vocabulaire",
                        educationalNote = "Emily uses 'lexical range' to refer to vocabulary scope."
                    )
                )
            }
        }
        return GroupChatResponse(replies = replies)
    }
}
