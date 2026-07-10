package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// --- Specific Structured Output Models for our App's Responses ---

@JsonClass(generateAdapter = true)
data class VoiceTutorResponse(
    @Json(name = "feedback") val feedback: String,
    @Json(name = "correctedText") val correctedText: String?,
    @Json(name = "reply") val reply: String,
    @Json(name = "pronunciationTips") val pronunciationTips: String?
)

@JsonClass(generateAdapter = true)
data class GroupChatPersonaReply(
    @Json(name = "sender") val sender: String, // "Sarah", "Emily", "Jean"
    @Json(name = "text") val text: String,
    @Json(name = "frenchTranslation") val frenchTranslation: String?,
    @Json(name = "educationalNote") val educationalNote: String?
)

@JsonClass(generateAdapter = true)
data class GroupChatResponse(
    @Json(name = "replies") val replies: List<GroupChatPersonaReply>
)
