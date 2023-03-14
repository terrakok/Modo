package com.github.terrakok.modo

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.github.terrakok.modo.lifecycle.LifecycleHandler
import com.github.terrakok.modo.model.ScreenModelStore
import com.github.terrakok.modo.util.currentOrThrow
import kotlinx.coroutines.channels.Channel

typealias RendererContent<State> = @Composable ComposeRendererScope<State>.() -> Unit

val defaultRendererContent: (@Composable ComposeRendererScope<*>.() -> Unit) = {
    screen.SaveableContent()
    val channel = LocalTransitionCompleteChannel.current
    // There's no animation so we can instantly mark the transition as completed
    DisposableEffect(screen.screenKey) {
        onDispose {
            channel.trySend(Unit)
        }
    }
}

internal val LocalSaveableStateHolder = staticCompositionLocalOf<SaveableStateHolder?> { null }

internal val LocalTransitionCompleteChannel =
    staticCompositionLocalOf<Channel<Unit>> { error("no channel provided") }

@Composable
fun Screen.SaveableContent() {
    // The current visible screen should sync with parent's lifecycle (Activity, Fragment or
    // ContainerScreen)
    val parentLifecycle = LocalLifecycleOwner.current.lifecycle
    CompositionLocalProvider(
        LocalLifecycleOwner provides this,
        LocalViewModelStoreOwner provides this,
        LocalSavedStateRegistryOwner provides this,
    ) {
        LocalSaveableStateHolder.currentOrThrow.SaveableStateProvider(key = screenKey) {
            LifecycleHandler(parentLifecycle)
            Content()
        }
    }
}

class ComposeRendererScope<State : NavigationState>(
    val oldState: State?,
    val newState: State?,
    val screen: Screen,
)

/**
 * Renderer responsibilities:
 * 1. Rendering - wrapping state to composable state and delegating rendering to screens
 * 2. Storing and clearing composable states inside [SaveableStateHolder]
 */
internal class ComposeRenderer<State : NavigationState>(
    private val containerScreen: ContainerScreen<*>,
) : NavigationRenderer<State> {

    private var lastState: State? = null
    var state: State? by mutableStateOf(null, neverEqualPolicy())
        private set

    // TODO: share removed screen for whole structure
    private val removedScreens = mutableSetOf<Screen>()

    /**
     * A channel that is used to notify about completing of screen transition, so we can dispose
     * screen that is removed out of the backstack properly.
     */
    private val transitionCompleteChannel: Channel<Unit> = Channel(Channel.CONFLATED)

    override fun render(state: State) {
        this.state?.let { currentState ->
            removedScreens.addAll(calculateRemovedScreens(currentState, state))
        }
        lastState = this.state
        this.state = state
    }

    @Composable
    fun Content(screen: Screen, content: RendererContent<State> = defaultRendererContent) {
        // use single state holder for whole hierarchy, store it on top of it
        val stateHolder: SaveableStateHolder =
            LocalSaveableStateHolder.current ?: rememberSaveableStateHolder()

        LaunchedEffect(
            key1 = screen.screenKey,
        ) {
            for (event in transitionCompleteChannel) {
                clearScreens(stateHolder)
            }
        }

        CompositionLocalProvider(
            LocalSaveableStateHolder providesDefault stateHolder,
            LocalContainerScreen provides containerScreen,
            LocalTransitionCompleteChannel provides transitionCompleteChannel,
        ) {
            ComposeRendererScope(lastState, state, screen).content()
        }
    }

    /**
     * Clear states of removed screens from given [stateHolder].
     * @param stateHolder
     * - SaveableStateHolder that contains screen states
     * @param clearAll
     * - forces to remove all screen states that renderer holds (removed and "displayed")
     */
    private fun clearScreens(stateHolder: SaveableStateHolder, clearAll: Boolean = false) {
        if (clearAll) {
            state?.getChildScreens()?.clearStates(stateHolder)
        }
        removedScreens.clearStates(stateHolder)
        if (removedScreens.isNotEmpty()) {
            removedScreens.clear()
        }
    }

    private fun Iterable<Screen>.clearStates(
        stateHolder: SaveableStateHolder,
    ) = forEach { screen ->
        ScreenModelStore.remove(screen)
        stateHolder.removeState(screen.screenKey)
        screen.onDispose()
        // clear nested screens using recursion
        ((screen as? ContainerScreen<*>)?.renderer as? ComposeRenderer<*>)?.clearScreens(
            stateHolder = stateHolder,
            clearAll = true
        )
    }

    private fun calculateRemovedScreens(
        oldState: NavigationState,
        newState: NavigationState
    ): List<Screen> {
        val newChainSet = newState.getChildScreens()
        return oldState.getChildScreens().filter { it !in newChainSet }
    }
}
