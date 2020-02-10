package dev.teapot.sample.di

import android.content.Context
import dev.teapot.log.LogType
import dev.teapot.log.TeapotLogger
import dev.teapot.program.ProgramBuilder
import dev.teapot.sample.SampleApp
import dev.teapot.sample.data.AppPrefs
import dev.teapot.sample.data.GitHubService
import dev.teapot.sample.data.RepoService
import dev.teapot.sample.data.IAppPrefs
import dev.teapot.sample.main.di.ActivityComponent
import dev.teapot.sample.main.di.ActivityModule
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Singleton

@Singleton
@Component(
    modules = [AppComponent.AppModule::class]
)
interface AppComponent {

    fun plusActivityComponent(module: ActivityModule): ActivityComponent

    @Component.Builder
    interface Builder {

        fun build(): AppComponent

        @BindsInstance
        fun application(app: SampleApp): Builder
    }

    @Module
    class AppModule {

        @Provides
        @Singleton
        fun provideContext(app: SampleApp): Context = app

        @Provides
        @Singleton
        fun githubService(): RepoService {
            return GitHubService()
        }

        @Provides
        @Singleton
        fun teapotLogger() : TeapotLogger {
            return object : TeapotLogger {

                override fun logType(): LogType {
                    return LogType.All
                }

                override fun error(stateName: String, t: Throwable) {
                    Timber.tag(stateName).e(t)
                }

                override fun log(stateName: String, message: String) {
                    Timber.tag(stateName).d(message)
                }

            }
        }

        @Provides
        @Singleton
        fun programBuilder(logger: TeapotLogger): ProgramBuilder {
            return ProgramBuilder()
                .outputDispatcher(Dispatchers.Main)
                .handleCmdErrors(true)
                .logger(logger)
        }

        @Provides
        @Singleton
        fun appPrefs(context: Context): IAppPrefs {
            return AppPrefs(context.getSharedPreferences("prefs", Context.MODE_PRIVATE))
        }

    }

}