package dev.teapot.sample.main.model

import dev.teapot.extensions.paging.PagingState
import dev.teapot.msg.Msg
import dev.teapot.contract.State
import org.eclipse.egit.github.core.Repository

data class MainState(
    val isCanceled : Boolean = false,
    val userName: String,
    val reposList: PagingState<Repository, String>
) : State()

object CancelMsg: Msg()
object RefreshMsg: Msg()
data class SubMsg(val time : Int): Msg()
data class Sub2Msg(val time : Int): Msg()