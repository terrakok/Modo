package com.github.terrakok.modo.android.compose

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.github.terrakok.modo.MultiNavigation
import com.github.terrakok.modo.NavigationRenderer
import com.github.terrakok.modo.NavigationState
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.StackNavigation

class ComposeRendererScope(
    val screen: ComposeScreen,
    val transitionType: ScreenTransitionType
)

typealias RendererContent = @Composable ComposeRendererScope.() -> Unit
val defaultRendererContent: RendererContent = { screen.SaveableContent() }

private val LocalSaveableStateHolder = staticCompositionLocalOf<SaveableStateHolder?> { null }

private val <T> ProvidableCompositionLocal<T?>.currentOrThrow: T
    @Composable
    get() = current ?: error("CompositionLocal is null")

@Composable
fun ComposeScreen.SaveableContent() {
    LocalSaveableStateHolder.currentOrThrow.SaveableStateProvider(key = id) {
        Content()
    }
}

fun Activity.ComposeRenderer(
    getTransitionType: (oldState: NavigationState?, newState: NavigationState?) -> ScreenTransitionType = ::defaultCalculateTransitionType,
    content: RendererContent = defaultRendererContent
) = ComposeRenderer(
    exitAction = { finish() },
    getTransitionType,
    content
)

/**
 * Renderer responsibilities:
 *  1. Rendering - wrapping state to composable state and delegating rendering to screens
 *  2. Storing and clearing composable states inside [SaveableStateHolder]
 *  3. Animations
 */
class ComposeRenderer(
    private val exitAction: () -> Unit = {},
    private val getTransitionType: (oldState: NavigationState?, newState: NavigationState?) -> ScreenTransitionType = ::defaultCalculateTransitionType,
    private val content: RendererContent = defaultRendererContent
) : NavigationRenderer {

    private val renderState: MutableState<NavigationState?> = mutableStateOf(null)
    private val lastStackEvent: MutableState<ScreenTransitionType> = mutableStateOf(ScreenTransitionType.Idle)

    // TODO: share removed screen for whole structure
    private val removedScreens = mutableSetOf<Screen>()

    override fun render(state: NavigationState) {
        if (state is StackNavigation && state.stack.isEmpty()) {
            exitAction()
        } else {
            lastStackEvent.value = getTransitionType(renderState.value, state)
            renderState.value?.let { currentState ->
                removedScreens.addAll(calculateRemovedScreens(currentState, state))
            }
            renderState.value = state
        }
    }

    @Composable
    fun Content() {
        // use single state holder for whole hierarchy, store it on top of it
        val stateHolder: SaveableStateHolder = LocalSaveableStateHolder.current ?: rememberSaveableStateHolder()
        DisposableEffect(key1 = renderState) {
            onDispose { clearStateHolder(stateHolder) }
        }
        CompositionLocalProvider(
            LocalSaveableStateHolder providesDefault stateHolder
        ) {
            renderState.value?.getActiveScreen()?.let { screen ->
                require(screen is ComposeScreen) {
                    "ComposeRender works with ComposeScreen only! Received $screen"
                }
                ComposeRendererScope(screen, lastStackEvent.value).content()
            }
        }
    }

    /**
     * Clear states of removed screens from given [stateHolder].
     * @param stateHolder - SaveableStateHolder that contains screen states
     * @param clearAll - forces to remove all screen states that renderer holds (removed and "displayed")
     */
    private fun clearStateHolder(stateHolder: SaveableStateHolder, clearAll: Boolean = false) {
        if (clearAll) {
            renderState.value?.getAllScreens()?.clearStates(stateHolder)
        }
        removedScreens.clearStates(stateHolder)
        if (removedScreens.isNotEmpty()) {
            removedScreens.clear()
        }
    }

    private fun Iterable<Screen>.clearStates(stateHolder: SaveableStateHolder) = forEach { screen ->
        require(screen is ComposeScreen)
        stateHolder.removeState(screen.id)
        // clear nested screens using recursion
        ((screen as? ComposeContainerScreen<*>)?.renderer as? ComposeRenderer)?.clearStateHolder(stateHolder, clearAll = true)
    }

    private fun calculateRemovedScreens(oldState: NavigationState, newState: NavigationState): List<Screen> {
        val newChainSet = newState.getAllScreens()
        return oldState.getAllScreens().filter { it !in newChainSet }
    }
}