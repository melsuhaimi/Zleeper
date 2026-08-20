package com.zleeper.sleepapp.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.zleeper.sleepapp.navigation.AppNavigation
import com.zleeper.sleepapp.ui.theme.ZleeperTheme

@Composable
fun App() {
    AppNavigation()
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    ZleeperTheme {
        App()
    }
}
