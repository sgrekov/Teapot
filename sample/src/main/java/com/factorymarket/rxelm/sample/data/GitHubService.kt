package com.factorymarket.rxelm.sample.data

import io.reactivex.Scheduler
import io.reactivex.Single
import kotlinx.coroutines.delay
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.RepositoryId
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.egit.github.core.service.StargazerService
import org.eclipse.egit.github.core.service.UserService

class GitHubService(private val scheduler: Scheduler) : IApiService {

    private var client = GitHubClient()

    override fun login(login: String, pass: String): Single<Boolean> {
        return Single.fromCallable {
            client.setCredentials(login, pass)
            val userService = UserService(client)
            userService.user != null
        }.subscribeOn(scheduler)
    }

    override suspend fun login2(login: String, pass: String): Boolean {
        client.setCredentials(login, pass)
        val userService = UserService(client)
        return userService.user != null
    }

    override fun getUserName(): String {
        return client.user
    }


    override fun getStarredRepos(userName: String): Single<List<Repository>> {
        return Single.fromCallable {
            val stargazerService = StargazerService(client)
            stargazerService.starred
        }.subscribeOn(scheduler)
    }

    override suspend fun getStarredRepos2(userName: String): List<Repository> {
        val stargazerService = StargazerService(client)
        return stargazerService.starred
    }

    override fun getRepo(id: RepositoryId): Single<Repository> {
        return Single.fromCallable {
            val repoService = RepositoryService(client)
            repoService.getRepository(id)
        }.subscribeOn(scheduler)
    }

    override suspend fun getRepo2(id: RepositoryId): Repository {
        val repoService = RepositoryService(client)
        return repoService.getRepository(id)
    }

}
