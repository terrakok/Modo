package com.github.terrakok.modo

import android.os.Parcel
import androidx.compose.runtime.Composable
import kotlinx.parcelize.Parcelize

fun MockScreen(key: ScreenKey): Screen = object : Screen(key) {

    @Composable
    override fun Content() = Unit

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = Unit
}