package com.factorymarket.rxelm.sample.data

import com.factorymarket.rxelm.components.paging.PagingResult
import io.reactivex.Scheduler
import io.reactivex.Single
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.RepositoryId
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.egit.github.core.service.StargazerService
import org.eclipse.egit.github.core.service.UserService
import timber.log.Timber

class GitHubService(private val scheduler: Scheduler) : RepoService {

    companion object {
        const val PAGE_SIZE = 20
    }

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


    override fun getStarredRepos(userName: String, page: Int): Single<PagingResult<Repository>> {
        return Single.fromCallable {
            val stargazerService = StargazerService(client)
            val iterator = stargazerService.pageStarred(page , PAGE_SIZE)
            val items = iterator.next().toList()
            return@fromCallable PagingResult(items, iterator.lastPage, iterator.lastPage * PAGE_SIZE)
        }.subscribeOn(scheduler)
    }

    override suspend fun getStarredRepos2(userName: String?, page: Int): PagingResult<Repository> {
        val stargazerService = StargazerService(client)
        val iterator = stargazerService.pageStarred(page , PAGE_SIZE)
        val items = iterator.next().toList()
        return PagingResult(items, iterator.lastPage, iterator.lastPage * PAGE_SIZE)
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
