package com.factorymarket.rxelm.sample.data

import io.reactivex.Scheduler
import io.reactivex.Single
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.client.GitHubClient
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

    override fun getUserName(): String {
        return client.user
    }


    override fun getStarredRepos(userName: String): Single<List<Repository>> {
        return Single.fromCallable {
            val stargazerService = StargazerService(client)
            stargazerService.starred
        }.subscribeOn(scheduler)
    }
}
