package com.zleeper.sleepapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zleeper.sleepapp.feature.shell.ZleeperViewModel
import com.zleeper.sleepapp.ui.theme.ZleeperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ZleeperViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            ZleeperTheme(state.settings.theme) {
                App(viewModel)
            }
        }
    }
}
