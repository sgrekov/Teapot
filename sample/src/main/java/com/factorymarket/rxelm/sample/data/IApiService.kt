package com.factorymarket.rxelm.sample.data

import io.reactivex.Single
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.RepositoryId

interface IApiService {

    fun getUserName(): String

    fun login(login: String, pass: String): Single<Boolean>

    fun getStarredRepos(userName: String): Single<List<Repository>>
    fun getRepo(id: RepositoryId): Single<Repository>
}
