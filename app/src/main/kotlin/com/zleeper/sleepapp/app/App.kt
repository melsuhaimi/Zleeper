package com.zleeper.sleepapp.app

import androidx.compose.runtime.Composable
import com.zleeper.sleepapp.navigation.AppNavigation
import com.zleeper.sleepapp.feature.shell.ZleeperViewModel

@Composable
fun App(viewModel: ZleeperViewModel) {
    AppNavigation(viewModel = viewModel)
}
