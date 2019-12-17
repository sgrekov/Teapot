package dev.teapot.extensions

import dev.teapot.test.TeapotSpec
import dev.teapot.cmd.None
import dev.teapot.extensions.cachedpaging.*
import io.reactivex.Single
import io.kotlintest.specs.DescribeSpec
import java.net.UnknownHostException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

const val CACHE_PAGING_PAGE_SIZE = 10
const val CACHE_PAGING_TOTAL_PAGES = 3

class CachedPagingFeatureNoCacheCaseSpec : DescribeSpec({

    describe("first iteration of cacheable paging with no cache") {

        val pagingComponent: CachedPagingFeature<String, Unit> =
                CachedPagingFeature(object : CachedPagingCommandsHandler<String, Unit> {
                    override fun getCachedItems(limit: Int, params: Unit): Single<CachedPagingResult<String>> {
                        TODO("no need to implement")
                    }

                    override fun fetchPage(page: Int, pageSize: Int, params: Unit): Single<CachedPagingSyncResult<String>> {
                        TODO("no need to implement")
                    }
                }, Unit, CACHE_PAGING_PAGE_SIZE)

        val spec = TeapotSpec(pagingComponent).withState(pagingComponent.initialState())

        it("should not allow to start paging request before started") {
            spec.checkState { state ->
                assertTrue { state.isBlockedForLoadingNextPage() }
                assertFalse { state.hasLoadedAllItems() }
            }
        }

        context("that paging started") {

            it("should request data from cache and api, and show fullscreen loader") {
                spec.whenMsg(CachedPagingStartMsg)
                    .thenCmdBatch(
                            CachedPagingLoadItemsFromCacheCmd(CACHE_PAGING_PAGE_SIZE, Unit),
                            CachedPagingSyncItemsCmd(
                                    1,
                                    CACHE_PAGING_PAGE_SIZE, Unit
                            )
                    ).checkState {
                        assertTrue { it.isBlockedForLoadingNextPage() }
                        assertFalse { it.isEmpty() }
                    }.diffState { prevState ->
                        prevState.copy(
                            isStarted = true,
                            isFullscreenSpinnerVisible = false,
                            isListVisible = false,
                            isSyncing = true,
                            isCacheLoading = true,
                            nextPage = 2
                        )
                    }
            }

            context("rare case: sync error for first page, but when sync error comes before cache result") {

                val networkErrorBeforeCacheSpec = spec.copy()

                context("network error for sync request") {
                    it("should show fullscreen loader") {
                        networkErrorBeforeCacheSpec!!.whenMsg(
                                CachedPagingErrorMsg(
                                        UnknownHostException(""), CachedPagingSyncItemsCmd(
                                        1,
                                        CACHE_PAGING_PAGE_SIZE, Unit
                                )
                                )
                        )
                            .diffState { prevState ->
                                prevState.copy(
                                    isFullscreenSpinnerVisible = true,
                                    showFullScreenError = false,
                                    isListVisible = false,
                                    isSyncing = false,
                                    hasSyncError = true
                                )
                            }.checkState {
                                assertTrue { it.isBlockedForLoadingNextPage() }
                                assertFalse { it.hasLoadedAllItems() }
                                assertFalse { it.isEmpty() }
                                assertFalse { it.showSyncError() }
                            }
                    }
                }

                context("zero list cache arrived") {
                    it("should show full screen error, also not allow new cache request ") {
                        networkErrorBeforeCacheSpec.whenMsg(
                                CachedPagingItemsLoadedFromCacheMsg(
                                        listOf<String>(),
                                        CACHE_PAGING_PAGE_SIZE
                                )
                        )
                            .thenCmd(None)
                            .diffState { prevState ->
                                prevState.copy(
                                    isFullscreenSpinnerVisible = false,
                                    isRefreshingEnabled = true,
                                    showFullScreenError = true,
                                    isListVisible = false,
                                    hasSyncError = false,
                                    nextPage = 1,
                                    isCacheLoading = false,
                                    isNoMoreCache = true
                                )
                            }.checkState {
                                assertTrue { it.isBlockedForLoadingNextPage() }
                                assertTrue { it.hasLoadedAllItems() }
                                assertFalse { it.isEmpty() }
                                assertFalse { it.showSyncError() }
                            }
                    }
                }
            }

            context("zero list cache arrived") {
                it("should still show full screen and wait for sync, also not allow new cache request ") {
                    spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(listOf<String>(), CACHE_PAGING_PAGE_SIZE))
                        .thenCmd(None)
                        .checkState {
                            assertFalse { it.isBlockedForLoadingNextPage() }
                            assertTrue { it.hasLoadedAllItems() }
                            assertFalse { it.isEmpty() }
                        }
                        .diffState { prevState ->
                            prevState.copy(
                                isFullscreenSpinnerVisible = true,
                                isListVisible = false,
                                isSyncing = true,
                                isCacheLoading = false,
                                isNoMoreCache = true
                            )
                        }
                }
            }

            context("sync error for first page") {
                val networkErrorSpec = spec.copy()

                it("should show fullscreen error") {
                    networkErrorSpec.whenMsg(
                            CachedPagingErrorMsg(
                                    UnknownHostException(""), CachedPagingSyncItemsCmd(
                                    1,
                                    CACHE_PAGING_PAGE_SIZE, Unit
                            )
                            )
                    )
                        .checkState {
                            assertTrue { it.isBlockedForLoadingNextPage() }
                            assertTrue { it.hasLoadedAllItems() }
                            assertFalse { it.isEmpty() }
                            assertFalse { it.showSyncError() }
                        }
                        .diffState { prevState ->
                            prevState.copy(
                                isFullscreenSpinnerVisible = false,
                                showFullScreenError = true,
                                isRefreshingEnabled = true,
                                isListVisible = false,
                                isSyncing = false,
                                hasSyncError = true,
                                nextPage = 1
                            )
                        }
                }
            }

            context("sync result message arrived") {
                it("should emit new cache request") {
                    spec.whenMsg(
                            CachedPagingSyncedItemsMsg(
                                    1,
                                    CACHE_PAGING_TOTAL_PAGES
                            )
                    )
                        .thenCmd(CachedPagingLoadItemsFromCacheCmd(CACHE_PAGING_PAGE_SIZE, Unit))
                        .checkState {
                            assertTrue { it.isBlockedForLoadingNextPage() }
                            assertFalse { it.hasLoadedAllItems() }
                            assertFalse { it.isEmpty() }
                        }
                        .diffState { prevState ->
                            prevState.copy(
                                isFullscreenSpinnerVisible = true,
                                isListVisible = false,
                                isSyncing = false,
                                isCacheLoading = true,
                                totalPages = CACHE_PAGING_TOTAL_PAGES
                            )
                        }
                }
            }

            context("fresh cache arrived") {
                it("should show list of fresh cached elements and allow new cache requests") {
                    val list = getItemsForPaging()

                    spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(list, CACHE_PAGING_PAGE_SIZE))
                        .thenCmd(None)
                        .checkState {
                            assertFalse { it.isBlockedForLoadingNextPage() }
                            assertFalse { it.hasLoadedAllItems() }
                            assertFalse { it.isEmpty() }
                        }
                        .diffState { oldState ->
                            oldState.copy(
                                items = list,
                                isFullscreenSpinnerVisible = false,
                                isListVisible = true,
                                isSyncing = false,
                                isCacheLoading = false,
                                isRefreshingEnabled = true,
                                hasSpinner = true,
                                isNoMoreCache = false
                            )
                        }
                }
            }

            context("user scrolled to the bottom of list") {

                it("should start loading second page ") {
                    spec.whenMsg(CachedPagingOnScrolledToEndMsg)
                        .thenCmdBatch(
                                CachedPagingLoadItemsFromCacheCmd(CACHE_PAGING_PAGE_SIZE * 2, Unit),
                                CachedPagingSyncItemsCmd(
                                        2,
                                        CACHE_PAGING_PAGE_SIZE, Unit
                                )
                        ).checkState {
                            assertTrue { it.isBlockedForLoadingNextPage() }
                        }.diffState { prevState ->
                            prevState.copy(
                                isSyncing = true,
                                isCacheLoading = true,
                                nextPage = 3,
                                hasSpinner = true
                            )
                        }
                }

                context("same list from cache arrived") {
                    it("should still show loader in the bottom, also not allow new cache request ") {
                        spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(getItemsForPaging(), CACHE_PAGING_PAGE_SIZE * 2))
                            .thenCmd(None)
                            .checkState {
                                assertFalse { it.isBlockedForLoadingNextPage() }
                                assertTrue { it.hasLoadedAllItems() }
                            }
                            .diffState { prevState ->
                                prevState.copy(
                                    isSyncing = true,
                                    isCacheLoading = false,
                                    isNoMoreCache = true
                                )
                            }
                    }
                }

                context("sync result message arrived") {
                    it("should emit new cache request") {
                        spec.whenMsg(
                                CachedPagingSyncedItemsMsg(
                                        2,
                                        CACHE_PAGING_TOTAL_PAGES
                                )
                        )
                            .thenCmd(CachedPagingLoadItemsFromCacheCmd(CACHE_PAGING_PAGE_SIZE * 2, Unit))
                            .checkState {
                                assertTrue { it.isBlockedForLoadingNextPage() }
                                assertFalse { it.hasLoadedAllItems() }
                            }
                            .diffState { prevState ->
                                prevState.copy(
                                    isSyncing = false,
                                    isCacheLoading = true
                                )
                            }
                    }
                }

                context("fresh cache arrived") {
                    it("should show list of fresh cached 2 page of elements and allow new cache requests") {
                        val list = getItemsForPaging(size = 20)

                        spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(list, CACHE_PAGING_PAGE_SIZE * 2))
                            .thenCmd(None)
                            .diffState { oldState ->
                                oldState.copy(
                                    items = list,
                                    isSyncing = false,
                                    isCacheLoading = false,
                                    hasSpinner = true,
                                    isNoMoreCache = false
                                )
                            }.checkState {
                                assertFalse { it.isBlockedForLoadingNextPage() }
                                assertFalse { it.hasLoadedAllItems() }
                            }
                    }
                }

                context("user scrolled to the bottom of list last time") {

                    it("should start loading third page ") {
                        spec.whenMsg(CachedPagingOnScrolledToEndMsg)
                            .thenCmdBatch(
                                    CachedPagingLoadItemsFromCacheCmd(CACHE_PAGING_PAGE_SIZE * 3, Unit),
                                    CachedPagingSyncItemsCmd(
                                            3,
                                            CACHE_PAGING_PAGE_SIZE, Unit
                                    )
                            ).checkState {
                                assertTrue { it.isBlockedForLoadingNextPage() }
                                assertFalse { it.hasLoadedAllItems() }
                            }.diffState { prevState ->
                                prevState.copy(
                                    isSyncing = true,
                                    isCacheLoading = true,
                                    nextPage = 4,
                                    hasSpinner = true
                                )
                            }
                    }

                    context("same list from cache arrived") {
                        it("should still show loader in the bottom, also not allow new cache request ") {
                            spec.whenMsg(
                                    CachedPagingItemsLoadedFromCacheMsg(
                                            getItemsForPaging(size = 20),
                                            CACHE_PAGING_PAGE_SIZE * 3
                                    )
                            )
                                .thenCmd(None)
                                .checkState {
                                    assertFalse { it.isBlockedForLoadingNextPage() }
                                    assertTrue { it.hasLoadedAllItems() }
                                }
                                .diffState { prevState ->
                                    prevState.copy(
                                        isSyncing = true,
                                        isCacheLoading = false,
                                        isNoMoreCache = true
                                    )
                                }
                        }
                    }

                    context("sync result message arrived") {
                        it("should emit new cache request") {
                            spec.whenMsg(
                                    CachedPagingSyncedItemsMsg(
                                            3,
                                            CACHE_PAGING_TOTAL_PAGES
                                    )
                            )
                                .thenCmd(CachedPagingLoadItemsFromCacheCmd(CACHE_PAGING_PAGE_SIZE * 3, Unit))
                                .checkState {
                                    assertTrue { it.isBlockedForLoadingNextPage() }
                                    assertTrue { it.hasLoadedAllItems() }
                                }
                                .diffState { prevState ->
                                    prevState.copy(
                                        isSyncing = false,
                                        isCacheLoading = true
                                    )
                                }
                        }
                    }

                    context("fresh cache arrived") {
                        it("should show list of fresh cached 2 page of elements and allow new cache requests") {
                            val list = getItemsForPaging(size = 30)

                            spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(list, CACHE_PAGING_PAGE_SIZE * 3))
                                .thenCmd(None)
                                .diffState { oldState ->
                                    oldState.copy(
                                        items = list,
                                        isSyncing = false,
                                        isCacheLoading = false,
                                        hasSpinner = false,
                                        isNoMoreCache = false
                                    )
                                }.checkState {
                                    assertFalse { it.isBlockedForLoadingNextPage() }
                                    assertTrue { it.hasLoadedAllItems() }
                                }
                        }
                    }
                }
            }
        }
    }
})

fun getItems(delta: Int = 0, size: Int = 10): List<String> {
    return arrayOfNulls<Int>(size).mapIndexed { index, i -> index }
        .map { (it + delta).toString() }
}
