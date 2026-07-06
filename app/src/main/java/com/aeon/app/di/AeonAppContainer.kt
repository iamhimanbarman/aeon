package com.aeon.app.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aeon.app.data.local.database.AeonDatabase
import com.aeon.app.data.auth.AuthRepository
import com.aeon.app.data.repository.AeonRepositories
import com.aeon.app.data.seed.AeonSeedData
import com.aeon.app.data.task.AndroidTaskReminderScheduler
import com.aeon.app.data.focus.AndroidFocusRoutineReminderScheduler
import com.aeon.app.data.ai.AiPreferences
import com.aeon.app.data.ai.NetworkStatusProvider
import com.aeon.app.data.news.RssNewsSourceClient
import com.aeon.app.data.repository.NewsRepositoryImpl
import com.aeon.app.domain.ai.NewsRepository
import com.aeon.app.domain.usecase.AeonUseCases
import com.aeon.app.presentation.viewmodel.AeonViewModelFactory

/*
 * AEON APP CONTAINER
 *
 * Purpose:
 * Manual dependency injection container for Aeon.
 *
 * Senior architecture:
 * - Keeps app dependencies centralized
 * - Avoids passing database/repositories manually through screens
 * - Works without Hilt/Koin
 * - Testable because the interface can be replaced
 * - Simple enough for an offline-first MVP
 *
 * Dependency graph:
 * Context
 *   -> AeonDatabase
 *   -> AeonRepositories
 *   -> AeonUseCases
 *   -> AeonViewModelFactory
 *
 * Recommended later:
 * If the app becomes very large, migrate this contract to Hilt.
 */


// ----------------------------------------------------
// Contract
// ----------------------------------------------------

interface AeonAppContainer {

    val database: AeonDatabase

    val repositories: AeonRepositories

    val authRepository: AuthRepository

    val useCases: AeonUseCases

    val newsRepository: NewsRepository

    val viewModelFactory: AeonViewModelFactory

    suspend fun initializeAppDefaults()

    fun warmUpNavigationDependencies()

    fun close()
}


// ----------------------------------------------------
// Default Implementation
// ----------------------------------------------------

class DefaultAeonAppContainer(
    context: Context
) : AeonAppContainer {

    private val appContext: Context = context.applicationContext
    private val aiPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AiPreferences(appContext) }
    private val aiNetwork by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { NetworkStatusProvider(appContext) }

    override val database: AeonDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AeonDatabase.getInstance(appContext)
    }

    override val repositories: AeonRepositories by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AeonRepositories(database)
    }

    override val authRepository: AuthRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AuthRepository(appContext)
    }

    override val useCases: AeonUseCases by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AeonUseCases(
            repositories = repositories,
            taskReminderScheduler = AndroidTaskReminderScheduler(
                context = appContext,
                taskDao = database.taskDao()
            ),
            focusRoutineReminderScheduler = AndroidFocusRoutineReminderScheduler(
                context = appContext,
                dao = database.focusRoutineDao()
            )
        )
    }

    override val newsRepository: NewsRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        NewsRepositoryImpl(
            dao = database.newsDao(),
            sourceClient = RssNewsSourceClient(),
            network = aiNetwork,
            preferences = aiPreferences
        )
    }

    override val viewModelFactory: AeonViewModelFactory by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AeonViewModelFactory(
            useCasesProvider = { useCases },
            repositoriesProvider = { repositories },
            newsRepositoryProvider = { newsRepository }
        )
    }

    override suspend fun initializeAppDefaults() {
        useCases.initializeDefaults()
        repositories.finance.ensureDefaultCategories()
        AeonSeedData.removeDemoData(database)
        AeonSeedData.seedProductionDefaultsIfNeeded(database)
    }

    override fun warmUpNavigationDependencies() {
        authRepository
        database
        repositories
        useCases
        newsRepository
        viewModelFactory
    }

    override fun close() {
        database.close()
        AeonDatabase.clearInstance()
    }
}


// ----------------------------------------------------
// Preview / Test Container
// ----------------------------------------------------

class PreviewAeonAppContainer(
    context: Context
) : AeonAppContainer {

    private val appContext: Context = context.applicationContext
    private val aiPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AiPreferences(appContext) }
    private val aiNetwork by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { NetworkStatusProvider(appContext) }

    override val database: AeonDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AeonDatabase.buildInMemoryDatabase(appContext)
    }

    override val repositories: AeonRepositories by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AeonRepositories(database)
    }

    override val authRepository: AuthRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AuthRepository(appContext)
    }

    override val useCases: AeonUseCases by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AeonUseCases(repositories)
    }

    override val newsRepository: NewsRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        NewsRepositoryImpl(
            database.newsDao(),
            RssNewsSourceClient(),
            aiNetwork,
            aiPreferences
        )
    }

    override val viewModelFactory: AeonViewModelFactory by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AeonViewModelFactory(
            useCasesProvider = { useCases },
            repositoriesProvider = { repositories },
            newsRepositoryProvider = { newsRepository }
        )
    }

    override suspend fun initializeAppDefaults() {
        useCases.initializeDefaults()
        repositories.finance.ensureDefaultCategories()
        AeonSeedData.removeDemoData(database)
        AeonSeedData.seedProductionDefaultsIfNeeded(database)
    }

    override fun warmUpNavigationDependencies() {
        authRepository
        database
        repositories
        useCases
        newsRepository
        viewModelFactory
    }

    override fun close() {
        database.close()
    }
}


// ----------------------------------------------------
// CompositionLocal
// ----------------------------------------------------

val LocalAeonAppContainer = staticCompositionLocalOf<AeonAppContainer> {
    error(
        "AeonAppContainer was not provided. " +
            "Wrap your app with AeonAppContainerProvider."
    )
}


@Composable
fun AeonAppContainerProvider(
    container: AeonAppContainer = rememberAeonAppContainer(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAeonAppContainer provides container,
        content = content
    )
}


@Composable
fun rememberAeonAppContainer(): AeonAppContainer {
    val context = LocalContext.current.applicationContext

    return remember(context) {
        DefaultAeonAppContainer(context)
    }
}


@Composable
@ReadOnlyComposable
fun currentAeonAppContainer(): AeonAppContainer {
    return LocalAeonAppContainer.current
}


// ----------------------------------------------------
// ViewModel Helper
// ----------------------------------------------------

@Composable
inline fun <reified VM : ViewModel> aeonViewModel(): VM {
    return viewModel(
        factory = LocalAeonAppContainer.current.viewModelFactory
    )
}
