package com.factorymarket.rxelm.sample.data

import com.factorymarket.rxelm.components.paging.PagingResult
import io.reactivex.Single
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.RepositoryId

interface RepoService {

    fun getUserName(): String

    fun login(login: String, pass: String): Single<Boolean>
    suspend fun login2(login: String, pass: String): Boolean

    fun getStarredRepos(userName: String): Single<List<Repository>>
    suspend fun getStarredRepos2(userName: String?, page : Int): PagingResult<Repository>
    fun getRepo(id: RepositoryId): Single<Repository>
    suspend fun getRepo2(id: RepositoryId): Repository
}
