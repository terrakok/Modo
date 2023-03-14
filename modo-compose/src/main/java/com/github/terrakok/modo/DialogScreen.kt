package com.github.terrakok.modo

import androidx.compose.ui.window.DialogProperties

@ExperimentalModoApi
abstract class DialogScreen(screenKey: ScreenKey) : Screen(screenKey) {

    fun provideDialogProperties(): DialogProperties = DialogProperties()
}