package com.github.terrakok.modo

import android.os.Parcelable
import androidx.compose.runtime.Composable
import com.github.terrakok.modo.lifecycle.ScreenLifecycle
import com.github.terrakok.modo.lifecycle.ScreenLifecycleImpl

abstract class Screen(
    val screenKey: ScreenKey,
) : Parcelable, ScreenLifecycle by ScreenLifecycleImpl(screenKey) {

    @Composable
    abstract fun Content()
}