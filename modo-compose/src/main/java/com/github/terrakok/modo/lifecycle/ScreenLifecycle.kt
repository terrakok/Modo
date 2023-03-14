package com.github.terrakok.modo.lifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey

interface ScreenLifecycle :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {

    var parentLifecycleState: Lifecycle.State

    fun setApplication(app: Application?)

    fun updateLifecycleState(state: Lifecycle.State)

    fun performRestore(savedState: Bundle?)

    fun performSave(outState: Bundle)

    fun onDispose()
}

class ScreenLifecycleImpl(
    val screenKey: ScreenKey,
) : ScreenLifecycle {

    override var parentLifecycleState: Lifecycle.State = Lifecycle.State.CREATED
        set(state) {
            if (field == state) return
            field = state
            updateLifecycleState(state)
        }

    private val lifecycle = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    private var app: Application? = null

    private val defaultFactory by lazy { SavedStateViewModelFactory(app, this) }

    private var savedStateRegistryRestored = false

    override fun setApplication(app: Application?) {
        this.app = app
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    override fun getViewModelStore(): ViewModelStore = store

    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return defaultFactory
    }

    override fun updateLifecycleState(state: Lifecycle.State) {
        if (lifecycle.currentState == state) return
        lifecycle.currentState = state
    }

    override fun performRestore(savedState: Bundle?) {
        // Perform the restore just once and before we move up the Lifecycle
        if (!savedStateRegistryRestored) {
            enableSavedStateHandles()
            savedStateRegistryController.performRestore(savedState)
            savedStateRegistryRestored = true
        }
    }

    override fun performSave(outState: Bundle) {
        savedStateRegistryController.performSave(outState)
    }

    override fun onDispose() {
        store.clear()
        updateLifecycleState(Lifecycle.State.DESTROYED)
    }
}

private val startEvents =
    arrayOf(Lifecycle.Event.ON_CREATE, Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME)

private val stopEvents = arrayOf(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP)

@Composable
internal fun Screen.LifecycleHandler(parentLifecycle: Lifecycle) {
    val savedState = rememberSaveable(key = "${screenKey.value}:bundle") { Bundle() }
    val activity = LocalContext.current.findActivity()
    setApplication(activity?.application)
    performRestore(savedState)
    DisposableEffect(Unit) {
        startEvents.forEach { updateLifecycleState(it.targetState) }
        parentLifecycleState = parentLifecycle.currentState
        val observer = LifecycleEventObserver { source, event ->
            if (event == Lifecycle.Event.ON_DESTROY && activity?.isChangingConfigurations == true) {
                /**
                 * Instance of the screen isn't recreated during config changes so skip this event
                 * to avoid crash while accessing to ViewModel with SavedStateHandle, because after
                 * ON_DESTROY, [androidx.lifecycle.SavedStateHandleController] is marked as not
                 * attached and next call of [registerSavedStateProvider] after recreating Activity
                 * on the same instance causing the crash.
                 */
                return@LifecycleEventObserver
            }
            if (event == Lifecycle.Event.ON_STOP) {
                performSave(savedState)
            }
            parentLifecycleState = source.lifecycle.currentState
        }
        parentLifecycle.addObserver(observer)
        onDispose {
            performSave(savedState)
            stopEvents.forEach {
                // don't move up lifecycle state if the parent state is lower than event's one
                if (it.targetState < parentLifecycleState) {
                    updateLifecycleState(it.targetState)
                }
            }
            parentLifecycle.removeObserver(observer)
        }
    }
}

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
