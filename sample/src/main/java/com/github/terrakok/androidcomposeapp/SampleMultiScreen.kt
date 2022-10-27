package com.github.terrakok.androidcomposeapp

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.terrakok.modo.MultiNavigation
import com.github.terrakok.modo.android.compose.MultiScreen
import com.github.terrakok.modo.android.compose.generateScreenKey
import com.github.terrakok.modo.selectContainer

class SampleMultiScreen : MultiScreen {

    constructor(
        initialState: MultiNavigation = MultiNavigation(
            containers = listOf(
                SampleStack(SampleScreen(1)),
                SampleStack(SampleScreen(2)),
                SampleStack(SampleScreen(3)),
            ),
            selected = 1
        )
    ) : super(initialState, generateScreenKey())

    constructor(parcel: Parcel) : super(parcel)

    @Composable
    override fun Content() {
        var showAllStacks by rememberSaveable {
            mutableStateOf(false)
        }
        Column {
            TopContent(showAllStacks, Modifier.weight(1f))
            Row {
                Text(
                    modifier = Modifier
                        .clickable { showAllStacks = !showAllStacks }
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    text = "🪄"
                )
                repeat(navigationState.containers.size) { tabPos ->
                    Tab(
                        modifier = Modifier.weight(1f),
                        isSelected = navigationState.selected == tabPos,
                        tabPos = tabPos,
                        onTabClick = { selectContainer(tabPos) }
                    )
                }
                Text(
                    modifier = Modifier
                        .clickable { dispatch(AddTab(navigationState.containers.size.toString(), SampleScreen(1))) }
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    text = "[+]"
                )
            }
        }
    }

    @Composable
    fun TopContent(showAllStacks: Boolean, modifier: Modifier) {
        Box(modifier = modifier) {
            Row {
                for ((pos, container) in navigationState.containers.withIndex()) {
                    if (showAllStacks || pos == navigationState.selected) {
                        Box(modifier = Modifier.weight(1f)) {
                            // внутри вызывается используется SaveableStateProvider с одинаковым ключом для экрана
                            Content(container)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Tab(
        isSelected: Boolean,
        tabPos: Int,
        modifier: Modifier = Modifier,
        onTabClick: () -> Unit,
    ) = Text(
        modifier = modifier
            .clickable(onClick = onTabClick)
            .background(if (isSelected) Color.LightGray else Color.White)
            .padding(16.dp),
        textAlign = TextAlign.Center,
        fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
        color = if (isSelected) Color.Red else Color.Black,
        text = "Tab $tabPos"
    )

    companion object CREATOR : Parcelable.Creator<SampleMultiScreen> {
        override fun createFromParcel(parcel: Parcel): SampleMultiScreen = SampleMultiScreen(parcel)

        override fun newArray(size: Int): Array<SampleMultiScreen?> = arrayOfNulls(size)
    }
}

@Preview
@Composable
fun PreviewSampleMultiScreen() {
    SampleMultiScreen().Content()
}