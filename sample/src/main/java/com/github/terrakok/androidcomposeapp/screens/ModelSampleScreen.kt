package com.github.terrakok.androidcomposeapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import com.github.terrakok.modo.model.ScreenModel
import com.github.terrakok.modo.model.coroutineScope
import com.github.terrakok.modo.model.rememberScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
class ModelSampleScreen : Screen(screenKey = generateScreenKey()) {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel {
            SampleScreenModel()
        }
        Column(Modifier.fillMaxSize()) {
            Text(text = screenModel.state.value.toString())
        }
    }

}

private class SampleScreenModel : ScreenModel {

    val state = mutableStateOf(0)

    init {
        coroutineScope.launch {
            while (isActive) {
                delay(1000)
                state.value++
            }
        }
    }

}