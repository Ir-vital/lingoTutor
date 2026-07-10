package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.TutorRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GroupChatScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ActiveTab
import com.example.ui.viewmodel.TutorViewModel
import com.example.ui.viewmodel.TutorViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TutorRepository(
            chatDao = database.chatDao(),
            vocabularyDao = database.vocabularyDao(),
            lessonProgressDao = database.lessonProgressDao()
        )

        // 2. Initialize Text-to-Speech engine
        tts = TextToSpeech(this, this)

        setContent {
            MyApplicationTheme {
                // 3. Inject ViewModel using factory
                val tutorViewModel: TutorViewModel = viewModel(
                    factory = TutorViewModelFactory(repository)
                )

                // 4. Collect TTS playback events from ViewModel
                LaunchedEffect(Unit) {
                    lifecycleScope.launch {
                        tutorViewModel.ttsPlayEvent.collectLatest { textToSpeak ->
                            speakText(textToSpeak)
                        }
                    }
                }

                val activeTab by tutorViewModel.activeTab.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("app_bottom_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = activeTab == ActiveTab.DASHBOARD,
                                onClick = { tutorViewModel.setActiveTab(ActiveTab.DASHBOARD) },
                                modifier = Modifier.testTag("nav_item_dashboard"),
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == ActiveTab.DASHBOARD) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                        contentDescription = "Dashboard"
                                    )
                                },
                                label = { Text("Dashboard") }
                            )

                            NavigationBarItem(
                                selected = activeTab == ActiveTab.VOICE,
                                onClick = { tutorViewModel.setActiveTab(ActiveTab.VOICE) },
                                modifier = Modifier.testTag("nav_item_voice"),
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == ActiveTab.VOICE) Icons.Filled.Mic else Icons.Outlined.Mic,
                                        contentDescription = "Voice Tutor"
                                    )
                                },
                                label = { Text("Voice Tutor") }
                            )

                            NavigationBarItem(
                                selected = activeTab == ActiveTab.GROUP_CHAT,
                                onClick = { tutorViewModel.setActiveTab(ActiveTab.GROUP_CHAT) },
                                modifier = Modifier.testTag("nav_item_group_chat"),
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == ActiveTab.GROUP_CHAT) Icons.Filled.Forum else Icons.Outlined.Forum,
                                        contentDescription = "Group Chat"
                                    )
                                },
                                label = { Text("Group Chat") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (activeTab) {
                            ActiveTab.DASHBOARD -> DashboardScreen(
                                viewModel = tutorViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            ActiveTab.VOICE -> VoiceAssistantScreen(
                                viewModel = tutorViewModel,
                                tts = tts,
                                modifier = Modifier.fillMaxSize()
                            )
                            ActiveTab.GROUP_CHAT -> GroupChatScreen(
                                viewModel = tutorViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // --- TextToSpeech.OnInitListener implementation ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "US English language is not supported or missing speech data.")
            } else {
                Log.d(TAG, "TextToSpeech successfully initialized in US English.")
            }
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech engine.")
        }
    }

    private fun speakText(text: String) {
        if (tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d(TAG, "Speaking: $text")
        } else {
            Log.e(TAG, "TextToSpeech engine is not initialized!")
        }
    }

    override fun onDestroy() {
        // Prevent memory leaks by shutting down Speech engine
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
