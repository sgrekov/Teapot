package com.factorymarket.rxelm.sample.repo.model

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.msg.Msg
import org.eclipse.egit.github.core.Repository

data class RepoState(
        val openRepoId: String,
        val repository: Repository? = null,
        val isLoading: Boolean = false) : State()

sealed class RepoMsg : Msg()

object InitRepo : RepoMsg()
data class RepoLoaded(val repo: Repository) : RepoMsg()

sealed class RepoCmd : Cmd()
data class LoadRepo(val id: String) : RepoCmd()