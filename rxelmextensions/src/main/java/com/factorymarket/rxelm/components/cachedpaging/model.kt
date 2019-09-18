package com.factorymarket.rxelm.components.cachedpaging

import com.factorymarket.rxelm.contract.State

data class CachedPagingState<T, FETCH_PARAMS>(
    val isStarted: Boolean = false,
    val isRefreshingVisible: Boolean = false,
    val isRefreshingEnabled: Boolean = false,
    val showFullScreenError: Boolean = false,
    val isFullscreenSpinnerVisible: Boolean = false,
    val isListVisible: Boolean = false,
    val isSyncing: Boolean = false,
    val isCacheLoading: Boolean = false,
    val hasSpinner: Boolean = false,
    val hasRetryButton: Boolean = false,
    val hasSyncError: Boolean = false,
    val isNoMoreCache: Boolean = false,
    val items: List<T> = listOf(),
    val totalPages: Int? = null,
    val nextPage: Int = 1,
    val pageSize: Int,
    val fetchParams: FETCH_PARAMS
) : State() {

    fun isBlockedForLoadingNextPage(): Boolean {
        return !isStarted || isCacheLoading || hasRetryButton || showFullScreenError || isEmpty()
    }

    fun isEmpty(): Boolean {
        return isStarted && !isSyncing && !isCacheLoading && !hasSyncError && !showFullScreenError && items.isEmpty()
    }

    fun hasLoadedAllItems(): Boolean {
        return if (totalPages != null && !isSyncing) {
            nextPage > totalPages
        } else {
            hasRetryButton || showFullScreenError || isNoMoreCache
        }
    }

    fun showSyncError(): Boolean {
        return hasSyncError && !showFullScreenError && items.isNotEmpty()
    }

    fun toFullscreenLoadingState() = copy(
        isStarted = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = false,
        showFullScreenError = false,
        isFullscreenSpinnerVisible = true,
        isListVisible = false,
        hasSpinner = false,
        hasRetryButton = false,
        hasSyncError = false,
        items = listOf()
    )

    fun toNoItemsState() = copy(
        isStarted = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = true,
        showFullScreenError = false,
        isFullscreenSpinnerVisible = false,
        isListVisible = false,
        hasSpinner = false,
        hasRetryButton = false,
        isSyncing = false,
        isCacheLoading = false,
        hasSyncError = false,
        items = listOf(),
        nextPage = 1
    )

    fun toFullscreenErrorState() = this.copy(
        isStarted = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = true,
        showFullScreenError = true,
        isFullscreenSpinnerVisible = false,
        isListVisible = false,
        hasSpinner = false,
        hasRetryButton = false,
        isSyncing = false,
        isCacheLoading = false,
        hasSyncError = false,
        items = listOf(),
        nextPage = 1
    )

    fun toPageLoadingState() = copy(
        isStarted = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = true,
        showFullScreenError = false,
        isFullscreenSpinnerVisible = false,
        isListVisible = true,
        hasSpinner = true,
        hasRetryButton = false,
        hasSyncError = false
    )

    fun toLoadingStateWithSyncError() = copy(
        isStarted = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = true,
        showFullScreenError = false,
        isFullscreenSpinnerVisible = false,
        isListVisible = true,
        hasSpinner = true,
        hasRetryButton = false,
        hasSyncError = true
    )

    fun toListStateWithSyncError() = copy(
        isStarted = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = true,
        showFullScreenError = false,
        isFullscreenSpinnerVisible = false,
        isListVisible = true,
        hasSpinner = false,
        hasRetryButton = false,
        hasSyncError = true
    )

    fun toRetryPageState() = this.copy(
        isStarted = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = true,
        showFullScreenError = false,
        isFullscreenSpinnerVisible = false,
        isListVisible = true,
        hasSpinner = false,
        hasRetryButton = true
    )

    fun toRefreshingState() = this.copy(
        isStarted = true,
        isRefreshingVisible = true,
        isRefreshingEnabled = true,
        showFullScreenError = false,
        isFullscreenSpinnerVisible = false,
        isListVisible = true,
        hasSpinner = false,
        hasRetryButton = false,
        hasSyncError = false
    )

    fun toRefreshingStateWithSpinner() = this.copy(
        isStarted = true,
        isRefreshingVisible = true,
        isRefreshingEnabled = true,
        showFullScreenError = false,
        isFullscreenSpinnerVisible = false,
        isListVisible = true,
        hasSpinner = true,
        hasRetryButton = false,
        hasSyncError = false
    )

    fun toCompletelyLoadedState() = this.copy(
        isStarted = true,
        isRefreshingVisible = false,
        isRefreshingEnabled = true,
        showFullScreenError = false,
        isFullscreenSpinnerVisible = false,
        isListVisible = true,
        hasSpinner = false,
        hasRetryButton = false,
        hasSyncError = false,
        isSyncing = false,
        isCacheLoading = false
    )
}
