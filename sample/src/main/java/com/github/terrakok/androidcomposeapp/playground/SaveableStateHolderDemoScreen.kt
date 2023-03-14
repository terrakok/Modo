package com.github.terrakok.androidcomposeapp.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
class SaveableStateHolderDemoScreen : Screen(screenKey = generateScreenKey()) {

    @Composable
    override fun Content() {
        ScreenContent()
    }

}

@Composable
private fun ScreenContent() {
    val saveableStateHolder = rememberSaveableStateHolder()
    val screens1 = remember {
        listOf("11", "12", "13")
    }
    val screens2 = remember {
        listOf("21", "22", "23")
    }
    var selectedPos by rememberSaveable { mutableStateOf(0) }
    var showAllStacks by rememberSaveable {
        mutableStateOf(false)
    }
    Column {
        BrokenRememberSaveableContent(
            showAllStacks = showAllStacks,
            screens = screens1,
            saveableStateHolder = saveableStateHolder,
            selectedPos = selectedPos,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        // work working case you need to uncomment code below this and comment code above
//            WorkingRememberSaveableContent(
//                showAllStacks,
//                screens2,
//                saveableStateHolder,
//                selectedPos,
//                Modifier
//                    .weight(1f)
//                    .fillMaxWidth()
//            )
        BottomBar(
            screens1.size,
            selectedPos,
            changeDisplayTypeClick = { showAllStacks = !showAllStacks },
            onTabClick = { selectedPos = it })
    }

}

@Composable
private fun BrokenRememberSaveableContent(
    showAllStacks: Boolean,
    screens: List<String>,
    saveableStateHolder: SaveableStateHolder,
    selectedPos: Int,
    modifier: Modifier
) {
    Text(
        text = "Broken",
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Red)
    )
    if (showAllStacks) {
        Row(modifier) {
            for (screen in screens) {
                ScreenContent(
                    saveableStateHolder = saveableStateHolder,
                    id = screen,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    } else {
        ScreenContent(
            saveableStateHolder = saveableStateHolder,
            id = screens[selectedPos],
            modifier = modifier
        )
    }
}

@Composable
private fun WorkingRememberSaveableContent(
    showAllStacks: Boolean,
    screens: List<String>,
    saveableStateHolder: SaveableStateHolder,
    selectedPos: Int,
    modifier: Modifier
) {
    Text(
        text = "Working",
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Green)
    )
    Row(modifier) {
        for ((index, screen) in screens.withIndex()) {
            if (showAllStacks || index == selectedPos) {
                ScreenContent(
                    saveableStateHolder = saveableStateHolder,
                    id = screen,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun ScreenContent(
    saveableStateHolder: SaveableStateHolder,
    id: String,
    modifier: Modifier
) {
    saveableStateHolder.SaveableStateProvider(key = id) {
        val background = rememberSaveable(key = id) { Random.nextInt() }
        var counter by rememberSaveable { mutableStateOf(0) }
        LaunchedEffect(key1 = Unit) {
            while (isActive) {
                delay(1000)
                counter++
            }
        }
        Column(
            modifier = modifier.background(color = Color(background)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = background.toString())
            Text(text = counter.toString())
        }
    }
}

@Composable
private fun BottomBar(
    tabsCount: Int,
    selectedPos: Int,
    changeDisplayTypeClick: () -> Unit,
    onTabClick: (Int) -> Unit
) {
    Row {
        Text(
            modifier = Modifier
                .clickable { changeDisplayTypeClick() }
                .padding(16.dp),
            textAlign = TextAlign.Center,
            text = "🪄"
        )
        repeat(tabsCount) { tabPos ->
            Tab(
                modifier = Modifier.weight(1f),
                isSelected = selectedPos == tabPos,
                tabPos = tabPos,
                onTabClick = { onTabClick(tabPos) }
            )
        }
    }
}

@Composable
private fun Tab(
    isSelected: Boolean,
    tabPos: Int,
    modifier: Modifier = Modifier,
    onTabClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .clickable(onClick = onTabClick)
            .background(if (isSelected) Color.LightGray else Color.White)
            .padding(16.dp),
        textAlign = TextAlign.Center,
        fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
        color = if (isSelected) Color.Red else Color.Black,
        text = "Tab $tabPos"
    )
}