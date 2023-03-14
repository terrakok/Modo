package com.github.terrakok.modo

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.terrakok.modo.stack.CompositeAction

val LocalContainerScreen = staticCompositionLocalOf<ContainerScreen<*>> { error("no screen provided") }

abstract class ContainerScreen<State : NavigationState>(
    private val navModel: NavModel<State>
) : Screen(navModel.screenKey), NavigationContainer<State> by navModel {

    abstract val reducer: NavigationReducer<State>

    internal val renderer: NavigationRenderer<State>?
        get() = navModel.renderer

    init {
        navModel.init(
            reducerProvider = { reducer },
            renderer = ComposeRenderer(this)
        )
    }

    @Composable
    protected fun InternalContent(
        screen: Screen,
        content: RendererContent<State> = defaultRendererContent
    ) {
        val composeRenderer = renderer as ComposeRenderer
        composeRenderer.Content(screen, content)
    }

    override fun toString(): String = screenKey.value

}

typealias ReducerProvider<State> = () -> NavigationReducer<State>

/**
 * Container for simple using [ContainerScreen] with [Parcelize]
 */
class NavModel<State : NavigationState>(
    initialState: State,
    val screenKey: ScreenKey = generateScreenKey()
) : NavigationContainer<State>, Parcelable {

    override var navigationState: State = initialState
        get() = (renderer as ComposeRenderer<State>).state ?: field
        set(value) {
            field = value
            renderer?.render(value)
        }

    private var reducerProvider: ReducerProvider<State>? = null
    internal var renderer: ComposeRenderer<State>? = null
        private set

    internal fun init(
        reducerProvider: ReducerProvider<State>,
        renderer: ComposeRenderer<State>
    ) {
        assert(this.reducerProvider == null && this.renderer == null) {
            "Trying to initialize navigation model again"
        }
        this.reducerProvider = reducerProvider
        this.renderer = renderer
    }

    override fun dispatch(action: NavigationAction) {
        val reducer = reducerProvider!!()
        navigationState = reduce(reducer, navigationState, action)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(navigationState, flags)
        parcel.writeString(screenKey.value)
    }

    private fun reduce(reducer: NavigationReducer<State>, state: State, action: NavigationAction): State =
        (action as? CompositeAction)
            ?.actions
            ?.fold(state) { state, action -> reduce(reducer, state, action) }
            ?: reducer.reduce(action, state)

    companion object CREATOR : Parcelable.Creator<NavModel<*>> {
        override fun createFromParcel(parcel: Parcel): NavModel<NavigationState> {
            val state = parcel.readParcelable<NavigationState>(NavModel::class.java.classLoader)!!
            val screenKey = parcel.readString()!!
            return NavModel(state, ScreenKey(screenKey))
        }

        override fun newArray(size: Int): Array<NavModel<*>?> = arrayOfNulls(size)
    }
}