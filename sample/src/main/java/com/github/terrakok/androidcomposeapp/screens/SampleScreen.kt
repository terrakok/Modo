package com.github.terrakok.androidcomposeapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.terrakok.androidcomposeapp.ButtonsList
import com.github.terrakok.androidcomposeapp.ListScreen
import com.github.terrakok.androidcomposeapp.ModelSampleScreen
import com.github.terrakok.androidcomposeapp.randomBackground
import com.github.terrakok.androidcomposeapp.screens.containers.SampleMultiScreen
import com.github.terrakok.androidcomposeapp.screens.containers.SampleStackScreen
import com.github.terrakok.modo.LocalContainerScreen
import com.github.terrakok.modo.NavigationContainer
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import com.github.terrakok.modo.stack.StackScreen
import com.github.terrakok.modo.stack.StackState
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.backTo
import com.github.terrakok.modo.stack.forward
import com.github.terrakok.modo.stack.newStack
import com.github.terrakok.modo.stack.replace
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
class SampleScreen(
    private val i: Int,
) : Screen(screenKey = generateScreenKey()) {

    @Composable
    override fun Content() {
        val parent = LocalContainerScreen.current
        SampleContent(i, parent as StackScreen)
    }
}

@Composable
internal fun SampleContent(
    i: Int, navigator:
    NavigationContainer<StackState>,
    isDialog: Boolean = false
) {
    var counter by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(key1 = Unit) {
        while (isActive) {
            delay(100)
            counter++
        }
    }
    Column(
        modifier = Modifier
            .randomBackground()
            .padding(8.dp)
            .then(if (!isDialog) Modifier.fillMaxSize() else Modifier),
    ) {
        Text(
            text = counter.toString()
        )
        Text(
            text = "Screen $i",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(16.dp))
        ButtonsList(
            rememberButtons(navigator, i),
            Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun rememberButtons(
    navigator: NavigationContainer<StackState>,
    i: Int
): List<Pair<String, () -> Unit>> = remember {
    listOf(
        "Forward" to { navigator.forward(SampleScreen(i + 1)) },
        "Back" to { navigator.back() },
        "Replace" to { navigator.replace(SampleScreen(i + 1)) },
        "New stack" to {
            navigator.newStack(
                SampleScreen(i + 1),
                SampleScreen(i + 2),
                SampleScreen(i + 3)
            )
        },
        "Multi forward" to {
            navigator.forward(
                SampleScreen(i + 1),
                SampleScreen(i + 2),
                SampleScreen(i + 3)
            )
        },
        "New root" to { navigator.newStack(SampleScreen(i + 1)) },
        "Forward 3 sec delay" to {
            GlobalScope.launch {
                delay(3000)
                navigator.forward(SampleScreen(i + 1))
            }
        },
        "Back to '3'" to {
            navigator.navigationState.stack.getOrNull(2)?.let {
                navigator.backTo(it)
            }
        },
        "Container" to { navigator.forward(SampleStackScreen(i + 1)) },
        "Multiscreen" to { navigator.forward(SampleMultiScreen()) },
        "List/Details" to { navigator.forward(ListScreen()) },
        // Just experiments
//        "2 items screen" to { navigator.forward(TwoTopItemsStackScreen(i + 1)) },
//                "Demo" to { navigator.forward(SaveableStateHolderDemoScreen()) },
        "Dialog" to { navigator.forward(SampleDialog(i + 1)) },
        "Model" to { navigator.forward(ModelSampleScreen()) },
    )
}