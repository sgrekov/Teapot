package com.factorymarket.rxelm.components.paging

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.msg.Msg

data class PagingState<T, FETCH_PARAMS>(
    val isStarted: Boolean = false,
    val isRefreshingVisible: Boolean = false,
    val isRefreshingEnabled: Boolean = false,
    val isErrorStateVisible: Boolean = false,
    val isFullscreenSpinnerVisible: Boolean = false,
    val isListVisible: Boolean = false,
    val isPageLoading: Boolean = false,
    val hasSpinner: Boolean = false,
    val hasRetryButton: Boolean = false,
    val items: List<T> = listOf(),
    val totalPages: Int? = null,
    val nextPage: Int = 1,
    val fetchParams: FETCH_PARAMS?,
    val totalCount: Int? = null
) : State() {

    fun isBlockedForLoadingNextPage(): Boolean = !isStarted || isPageLoading || hasRetryButton || isErrorStateVisible

    fun toFullscreenLoadingState() = copy(
        hasRetryButton = false,
        hasSpinner = false,
        items = emptyList(),
        isListVisible = false,
        isRefreshingVisible = false,
        isRefreshingEnabled = false,
        isFullscreenSpinnerVisible = true,
        isErrorStateVisible = false,
        nextPage = 2,
        totalPages = null
    )

    fun hasLoadedAllItems(): Boolean = totalPages != null && nextPage > totalPages

    fun toLoadingState() = copy(
        hasSpinner = true,
        hasRetryButton = false,
        isListVisible = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = true,
        isFullscreenSpinnerVisible = false,
        isErrorStateVisible = false
    )

    fun toCompletelyLoadedState() = this.copy(
        hasRetryButton = false,
        hasSpinner = false,
        isListVisible = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = true,
        isFullscreenSpinnerVisible = false,
        isErrorStateVisible = false
    )

    fun toRetryState() = this.copy(
        hasSpinner = false,
        hasRetryButton = true,
        isListVisible = true,
        isRefreshingEnabled = true,
        isRefreshingVisible = false,
        isFullscreenSpinnerVisible = false,
        isPageLoading = false
    )

    fun toRefreshingState() = this.copy(
        hasSpinner = true,
        hasRetryButton = false,
        isListVisible = true,
        isFullscreenSpinnerVisible = false,
        isRefreshingVisible = true,
        isRefreshingEnabled = true,
        isErrorStateVisible = false,
        nextPage = 2,
        totalPages = null
    )

    fun toErrorState() = this.copy(
        hasSpinner = false,
        hasRetryButton = false,
        items = emptyList(),
        isListVisible = false,
        isRefreshingVisible = false,
        isRefreshingEnabled = false,
        isErrorStateVisible = true,
        isFullscreenSpinnerVisible = false,
        isPageLoading = false,
        nextPage = 1
    )
}

abstract class PagingMsg(val namespace: String) : Msg()
abstract class PagingCmd(val namespace: String) : Cmd()

data class PagingStartMsg(val ns: String = "") : PagingMsg(ns)
data class PagingStartWithParamsMsg<PARAMS, FETCH_PARAMS>(
        val customParams: PARAMS? = null,
        val fetchParams: FETCH_PARAMS? = null,
        val ns: String = ""
) : PagingMsg(ns)

data class PagingOnScrolledToEndMsg(val ns: String = "") : PagingMsg(ns)
data class PagingOnLoadedItemsMsg<T>(
    val items: List<T>,
    val totalPages: Int,
    val loadedPage: Int,
    val ns: String = ""
) : PagingMsg(ns)

data class PagingOnRefreshedItemsMsg<T>(
    val items: List<T>,
    val totalPages: Int,
    val totalCount: Int?,
    val ns: String = ""
) : PagingMsg(ns)

data class PagingErrorMsg(val err: Throwable, val cmd: Cmd, val ns: String = "") : PagingMsg(ns)
data class PagingOnRetryListButtonClickMsg(val ns: String = "") : PagingMsg(ns)
data class PagingOnRetryAfterFullscreenErrorButtonClickMsg(val ns: String = "") : PagingMsg(ns)
data class PagingOnSwipeMsg(val ns: String = "") : PagingMsg(ns)

data class PagingLoadItemsCmd<FETCH_PARAMS>(
    val page: Int,
    val params: FETCH_PARAMS?,
    val ns: String = ""
) : PagingCmd(ns) {

    override fun hashCode(): Int {
        return ns.hashCode()
    }
}

data class PagingRefreshItemsCmd<FETCH_PARAMS>(
    val params: FETCH_PARAMS?,
    val ns: String = ""
) : PagingCmd(ns) {
    override fun hashCode(): Int {
        return ns.hashCode()
    }
}

data class LogThrowableCmd(val error: Throwable, val ns : String = "") : Cmd()