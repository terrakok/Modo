package com.github.terrakok.modo.model

import androidx.compose.runtime.DisallowComposableCalls
import com.github.terrakok.modo.ModoDevOptions
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private typealias ScreenModelKey = String

internal typealias DependencyKey = String
private typealias DependencyInstance = Any
private typealias DependencyOnDispose = (Any) -> Unit
private typealias Dependency = Pair<DependencyInstance, DependencyOnDispose>

/**
 * Class that stores remove priority for a dependency with a dependency it-selves.
 * We need the remove priority to be able to customise behavior f.e. for correct work of lifecycle. Android lifecycle should be removed first.
 * Maybe this is just temporary solution, and we should store a list of dependencies per a screen.
 */
@PublishedApi
internal data class DependencyWithRemoveOrder(
    val removePriority: Long = 0L,
    val dependency: Dependency
)

object ScreenModelStore {

    @PublishedApi
    internal val screenModels: MutableMap<ScreenModelKey, ScreenModel> = ConcurrentHashMap()

    @PublishedApi
    internal val dependencies: MutableMap<DependencyKey, DependencyWithRemoveOrder> = ConcurrentHashMap()

    @PublishedApi
    internal val lastScreenModelKey: MutableStateFlow<ScreenModelKey?> = MutableStateFlow(null)

    /**
     * Counter that uses to save the order of adding dependencies.
     */
    @PublishedApi
    internal val dependencyCounter = AtomicLong(0L)

    /**
     * For debugging, contains keys of screen that was removed.
     */
    private val removedScreenKeys = ConcurrentHashMap<ScreenKey, Unit>()

    private val Screen.isRemoved: Boolean get() = screenKey.isRemoved
    private val ScreenKey.isRemoved: Boolean get() = removedScreenKeys[this] != null

    @PublishedApi
    internal inline fun <reified T : ScreenModel> getKey(screen: Screen, tag: String?): ScreenModelKey =
        "${screen.screenKey.value}:${T::class.qualifiedName}:${tag ?: "default"}"

    @PublishedApi
    internal fun getDependencyKey(screenModel: ScreenModel, name: String): DependencyKey =
        screenModels
            .firstNotNullOfOrNull {
                if (it.value == screenModel) it.key
                else null
            }
            ?: lastScreenModelKey.value
                ?.let { "$it:$name" }
            ?: "standalone:$name"

    @PublishedApi
    internal inline fun <reified T : ScreenModel> getOrPut(
        screen: Screen,
        tag: String?,
        factory: @DisallowComposableCalls () -> T
    ): T {
        val key = getKey<T>(screen, tag)
        lastScreenModelKey.value = key
        assertGetOrPutScreenModelsCorrect(screen, screenModels[key])
        return screenModels.getOrPut(key, factory) as T
    }

    inline fun <reified T : Any> getOrPutDependency(
        screenModel: ScreenModel,
        name: String,
        noinline onDispose: @DisallowComposableCalls (T) -> Unit = {},
        noinline factory: @DisallowComposableCalls (DependencyKey) -> T
    ): T {
        val key = getDependencyKey(screenModel, name)
        return getOrPutDependency<T>(key, factory, onDispose)
    }

    inline fun <reified T : Any> getOrPutDependency(
        key: DependencyKey,
        factory: @DisallowComposableCalls (DependencyKey) -> T,
        noinline onDispose: @DisallowComposableCalls (T) -> Unit
    ): T {
        assertGetOrPutDependencyCorrect(key, dependencies[key])
        return dependencies
            .getOrPut(key) { DependencyWithRemoveOrder(dependencyCounter.getAndIncrement(), (factory(key) to onDispose) as Dependency) }
            .dependency
            .first as T
    }

    inline fun <reified T : Any> getOrPutDependency(
        screen: Screen,
        name: String,
        tag: String? = null,
        noinline onDispose: @DisallowComposableCalls (T) -> Unit = {},
        noinline factory: @DisallowComposableCalls (DependencyKey) -> T
    ): T {
        val key = getDependencyKey(screen, name, tag)
        return getOrPutDependency(key, factory, onDispose)
    }

    fun getDependencyKey(screen: Screen, name: String, tag: String? = null) =
        "${screen.screenKey.value}:$name${if (tag != null) ":$tag" else ""}"

    fun remove(screen: Screen) {
        if (removedScreenKeys[screen.screenKey] != null) {
            ModoDevOptions.onIllegalScreenModelStoreAccess.validationFailed(
                IllegalStateException("Trying to remove screen $screen the second time.")
            )
        }
        screenModels.onEach(screen) { key ->
            screenModels[key]?.onDispose()
            screenModels -= key
        }

        val screenDependencies = dependencies
            .screenDependencies(screen)
            .toList()
        screenDependencies.sortedBy { it.value.removePriority }.forEach { (key, value) ->
            val (instance, onDispose) = value.dependency
            onDispose(instance)
            dependencies -= key
        }
        removedScreenKeys[screen.screenKey] = Unit
    }

    @PublishedApi
    internal fun assertGetOrPutScreenModelsCorrect(screen: Screen, valueInMap: Any?) {
        assertGetOrPutCorrect(screen.isRemoved, valueInMap != null)
    }

    @PublishedApi
    internal fun assertGetOrPutDependencyCorrect(dependencyKey: DependencyKey, valueInMap: Any?) {
        val screenKey: ScreenKey = ScreenKey(dependencyKey.substringBefore(":"))
        assertGetOrPutCorrect(screenKey.isRemoved, valueInMap != null)
    }

    /**
     * If screen is already removed, and we don't have value in map means,
     * that we are going initialize value for removed screen and the error must be reported.
     */
    private fun assertGetOrPutCorrect(isScreenRemoved: Boolean, hasScreenAssociatedValue: Boolean) {
        if (isScreenRemoved && !hasScreenAssociatedValue) {
            val text = """
                    Trying to initialize dependency after screen is gone.
                    If you straggled with this issue during implementation your custom ScreenContainer,
                    please take a look to the examples of usage com.github.terrakok.modo.LocalTransitionCompleteChannel at
                    com.github.terrakok.modo.animation.ScreenTransition or com.github.terrakok.modo.defaultRendererContent.
                    Otherwise please report an issue at ${ModoDevOptions.REPORT_ISSUE_URL}
                    """.trimIndent()
            ModoDevOptions.onIllegalScreenModelStoreAccess.validationFailed(
                IllegalStateException(text),
            )
        }
    }

    private fun <T> Map<String, T>.screenDependencies(screen: Screen): Sequence<Map.Entry<String, T>> =
        asSequence().filter { it.key.startsWith(screen.screenKey.value) }

    private fun <T> Map<String, T>.onEach(screen: Screen, block: (String) -> Unit) =
        asSequence()
            .filter { it.key.startsWith(screen.screenKey.value) }
            .map { it.key }
            .forEach(block)
}
