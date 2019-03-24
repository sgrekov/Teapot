package com.factorymarket.rxelm.sample.repo.di

import com.factorymarket.rxelm.sample.repo.view.IRepoView
import com.factorymarket.rxelm.sample.repo.view.RepoFragment
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Named


@Subcomponent(modules = [RepoModule::class])
interface RepoComponent {

    fun inject(repoFragment: RepoFragment)

}

@Module
class RepoModule(private val fragment: RepoFragment, private val repoId: String) {

    @Provides
    fun view(): IRepoView = fragment

    @Provides
    @Named("repo_id")
    fun id(): String = repoId

}