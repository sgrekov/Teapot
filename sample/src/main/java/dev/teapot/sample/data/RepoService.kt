package dev.teapot.sample.data

import dev.teapot.extensions.paging.PagingResult
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.RepositoryId

interface RepoService {

    fun getUserName(): String
    suspend fun login2(login: String, pass: String): Boolean
    suspend fun getStarredRepos2(userName: String?, page : Int): PagingResult<Repository>
    suspend fun getRepo2(id: RepositoryId): Repository
}
