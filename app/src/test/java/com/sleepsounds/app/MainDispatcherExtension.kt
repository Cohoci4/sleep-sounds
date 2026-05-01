package com.sleepsounds.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Swap [Dispatchers.Main] for a [UnconfinedTestDispatcher] for the duration of
 * a test so `viewModelScope` (which dispatches on Main) executes coroutines
 * eagerly on the calling thread.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension : BeforeEachCallback, AfterEachCallback {
    private val dispatcher = UnconfinedTestDispatcher()

    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(dispatcher)
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
