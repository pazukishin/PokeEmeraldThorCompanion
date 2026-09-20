package com.thorcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.thorcompanion.ui.CompanionScreen
import com.thorcompanion.ui.ThorTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<CompanionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ThorTheme { CompanionScreen(viewModel, Modifier.fillMaxSize()) } }
    }
}