package dev.teapot.sample.data

import dev.teapot.extensions.paging.PagingResult
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.RepositoryId
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.egit.github.core.service.StargazerService
import org.eclipse.egit.github.core.service.UserService

class GitHubService() : RepoService {

    companion object {
        const val PAGE_SIZE = 20
    }

    private var client = GitHubClient()

    override suspend fun login2(login: String, pass: String): Boolean {
        client.setCredentials(login, pass)
        val userService = UserService(client)
        return userService.user != null
    }

    override fun getUserName(): String {
        return client.user
    }

    override suspend fun getStarredRepos2(userName: String?, page: Int): PagingResult<Repository> {
        val stargazerService = StargazerService(client)
        val iterator = stargazerService.pageStarred(page , PAGE_SIZE)
        val items = iterator.next().toList()
        return PagingResult(items, iterator.lastPage, iterator.lastPage * PAGE_SIZE)
    }

    override suspend fun getRepo2(id: RepositoryId): Repository {
        val repoService = RepositoryService(client)
        return repoService.getRepository(id)
    }

}
