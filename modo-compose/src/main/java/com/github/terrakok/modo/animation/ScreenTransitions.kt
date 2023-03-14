package com.github.terrakok.modo.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.ComposeRendererScope
import com.github.terrakok.modo.LocalTransitionCompleteChannel
import com.github.terrakok.modo.SaveableContent
import com.github.terrakok.modo.Screen

typealias ScreenTransitionContent = @Composable AnimatedVisibilityScope.(Screen) -> Unit

@ExperimentalAnimationApi
@Composable
fun ComposeRendererScope<*>.ScreenTransition(
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentScope<Screen>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) with
                fadeOut(animationSpec = tween(90))
    },
    content: ScreenTransitionContent = { it.SaveableContent() },
) {
    val transition = updateTransition(targetState = screen, label = "ScreenTransition")
    transition.AnimatedContent(
        transitionSpec = transitionSpec,
        modifier = modifier,
        contentKey = { it.screenKey },
        content = content,
    )
    if (transition.currentState == transition.targetState) {
        val channel = LocalTransitionCompleteChannel.current
        LaunchedEffect(Unit) {
            channel.trySend(Unit)
        }
    }
}