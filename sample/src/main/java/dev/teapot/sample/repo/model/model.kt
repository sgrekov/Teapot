package dev.teapot.sample.repo.model

import dev.teapot.msg.Msg
import dev.teapot.cmd.Cmd
import dev.teapot.contract.State
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