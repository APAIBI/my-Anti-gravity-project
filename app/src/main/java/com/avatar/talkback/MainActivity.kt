package com.avatar.talkback

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.avatar.talkback.ui.TalkBackScreen
import com.avatar.talkback.ui.state.AvatarViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AvatarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TalkBackScreen(viewModel = viewModel)
        }
    }
}
