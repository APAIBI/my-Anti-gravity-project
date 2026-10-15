package com.avatar.talkback

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avatar.talkback.ui.TalkBackScreen
import com.avatar.talkback.ui.state.AvatarViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: AvatarViewModel = viewModel()
            TalkBackScreen(viewModel = viewModel)
        }
    }
}
