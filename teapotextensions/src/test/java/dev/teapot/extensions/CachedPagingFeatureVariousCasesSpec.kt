package dev.teapot.extensions

import dev.teapot.extensions.paging.LogThrowableCmd
import dev.teapot.test.TeapotSpec
import dev.teapot.cmd.None
import dev.teapot.extensions.cachedpaging.*
import io.kotlintest.specs.DescribeSpec
import io.reactivex.Single
import java.net.UnknownHostException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CachedPagingFeatureVariousCasesSpec : DescribeSpec({

    val pagingComponent: CachedPagingFeature<String, Unit> =
            CachedPagingFeature(object : CachedPagingCommandsHandler<String, Unit> {
                override fun getCachedItems(limit: Int, params: Unit): Single<CachedPagingResult<String>> {
                    TODO("no need to implement")
                }

                override fun fetchPage(page: Int, pageSize: Int, params: Unit): Single<CachedPagingSyncResult<String>> {
                    TODO("no need to implement")
                }
            }, Unit, CACHE_PAGING_PAGE_SIZE)

    val spec = TeapotSpec(pagingComponent).withState(pagingComponent.initialState(Unit))

    describe("various cases") {

        it("should not allow to start paging request before started") {
            spec.checkState { state ->
                assertTrue { state.isBlockedForLoadingNextPage() }
                assertFalse { state.hasLoadedAllItems() }
            }
        }

        context("that paging started when no elements at all ") {

            it(
                "should request data from cache and api, and do not show fullscreen loader while we are sure " +
                        "that no data in cache. This is because if we have data in cache and we will show spinner then there will be " +
                        "blinking"
            ) {
                spec.whenMsg(CachedPagingStartMsg)
                    .thenCmdBatch(
                            CachedPagingLoadItemsFromCacheCmd(CACHE_PAGING_PAGE_SIZE, Unit),
                            CachedPagingSyncItemsCmd(
                                    1,
                                    CACHE_PAGING_PAGE_SIZE, Unit
                            )
                    ).checkState {
                        assertTrue { it.isBlockedForLoadingNextPage() }
                        assertFalse { it.hasLoadedAllItems() }
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

            context("first page arrived with 2 element cache") {
                val smallCacheSpec = spec.copy()

                it("should show cached list and show refreshing, and do not show spinner as it looks wierd") {
                    smallCacheSpec.whenMsg(
                            CachedPagingItemsLoadedFromCacheMsg(
                                    getItemsForPaging(size = 2),
                                    CACHE_PAGING_PAGE_SIZE
                            )
                    )
                        .thenCmd(None)
                        .checkState {
                            assertFalse { it.isBlockedForLoadingNextPage() }
                            assertTrue { it.hasLoadedAllItems() }
                        }
                        .diffState { prevState ->
                            prevState.copy(
                                isRefreshingEnabled = true,
                                isRefreshingVisible = true,
                                isFullscreenSpinnerVisible = false,
                                isListVisible = true,
                                items = getItemsForPaging(size = 2),
                                isSyncing = true,
                                isCacheLoading = false,
                                hasSpinner = false,
                                isNoMoreCache = true
                            )
                        }
                }

                context("network error for sync command") {
                    it("should show sync refresh btn and do not allow for further cache requests") {
                        val err = UnknownHostException("error")
                        smallCacheSpec.whenMsg(
                                CachedPagingErrorMsg(
                                        err,
                                        CachedPagingSyncItemsCmd(
                                                1,
                                                CACHE_PAGING_PAGE_SIZE, Unit
                                        )
                                )
                        ).thenCmd(LogThrowableCmd(err))
                            .checkState {
                                assertFalse { it.isBlockedForLoadingNextPage() }
                                assertTrue { it.hasLoadedAllItems() }
                            }
                            .diffState { prevState ->
                                prevState.copy(
                                    hasSyncError = true,
                                    hasSpinner = false,
                                    isSyncing = false,
                                    isCacheLoading = false,
                                    isRefreshingVisible = false)
                            }
                    }
                }
            }

            context("first page arrived with cache") {
                it("should show cached list and show refreshing, since the syncing process goes in background") {
                    spec.copy().whenMsg(CachedPagingItemsLoadedFromCacheMsg(getItemsForPaging(), CACHE_PAGING_PAGE_SIZE))
                        .thenCmd(None)
                        .checkState {
                            assertFalse { it.isBlockedForLoadingNextPage() }
                            assertFalse { it.hasLoadedAllItems() }
                        }
                        .diffState { prevState ->
                            prevState.copy(
                                isRefreshingEnabled = true,
                                isRefreshingVisible = true,
                                isFullscreenSpinnerVisible = false,
                                isListVisible = true,
                                items = getItemsForPaging(),
                                isSyncing = true,
                                isCacheLoading = false,
                                hasSpinner = true,
                                isNoMoreCache = false
                            )
                        }
                }
            }

            context("first page arrived with no cache") {
                it("should show fullscreen loader and not allow for new cache request ") {
                    spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(listOf<String>(), CACHE_PAGING_PAGE_SIZE))
                        .thenCmd(None)
                        .checkState {
                            assertFalse { it.isBlockedForLoadingNextPage() }
                            assertTrue { it.hasLoadedAllItems() }
                            assertFalse { it.isEmpty() }
                        }
                        .diffState { prevState ->
                            prevState.copy(
                                isRefreshingEnabled = false,
                                isFullscreenSpinnerVisible = true,
                                isListVisible = false,
                                isSyncing = true,
                                isCacheLoading = false,
                                hasSpinner = false,
                                isNoMoreCache = true
                            )
                        }
                }
            }

            context("sync result message arrived") {
                it("should emit new cache request") {
                    spec.whenMsg(
                            CachedPagingSyncedItemsMsg(
                                    1,
                                    0
                            )
                    )
                        .thenCmd(CachedPagingLoadItemsFromCacheCmd(CACHE_PAGING_PAGE_SIZE, Unit))
                        .checkState {
                            assertTrue { it.isBlockedForLoadingNextPage() }
                            assertTrue { it.hasLoadedAllItems() }
                            assertFalse { it.isEmpty() }
                        }
                        .diffState { prevState ->
                            prevState.copy(
                                isSyncing = false,
                                isCacheLoading = true,
                                totalPages = 0,
                                hasSpinner = false
                            )
                        }
                }
            }

            context("empty cache arrived") {
                it("should show empty text") {
                    spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(listOf<String>(), CACHE_PAGING_PAGE_SIZE))
                        .thenCmd(None)
                        .checkState {
                            assertTrue { it.isBlockedForLoadingNextPage() }
                            assertTrue { it.hasLoadedAllItems() }
                            assertTrue { it.isEmpty() }
                        }
                        .diffState { oldState ->
                            oldState.copy(
                                isFullscreenSpinnerVisible = false,
                                isListVisible = false,
                                isSyncing = false,
                                nextPage = 1,
                                isCacheLoading = false,
                                isRefreshingEnabled = true,
                                hasSpinner = false
                            )
                        }
                }
            }
        }
    }

    describe("when pull to refresh started") {
        context("that one page is rendered on the screen") {
            val pullToRefreshSpec = TeapotSpec(pagingComponent).withState(
                    CachedPagingState(
                            isStarted = true,
                            isRefreshingEnabled = true,
                            isListVisible = true,
                            hasSpinner = true,
                            items = getItemsForPaging(),
                            totalPages = CACHE_PAGING_TOTAL_PAGES,
                            nextPage = 2,
                            pageSize = CACHE_PAGING_PAGE_SIZE,
                            fetchParams = Unit
                    )
            )

            it("ready for continuing paging") {
                pullToRefreshSpec.checkState { state ->
                    assertFalse { state.isBlockedForLoadingNextPage() }
                    assertFalse { state.hasLoadedAllItems() }
                }
            }

            context("pull to refresh msg sent") {
                it("should show refreshing progress") {
                    pullToRefreshSpec.whenMsg(CachedPagingOnSwipeMsg)
                        .thenCmdBatch(
                                CachedPagingLoadItemsFromCacheCmd(1 * CACHE_PAGING_PAGE_SIZE, Unit),
                                CachedPagingSyncItemsCmd(1, CACHE_PAGING_PAGE_SIZE, Unit)
                        )
                        .diffState {
                            it.copy(
                                isSyncing = true,
                                isCacheLoading = true,
                                isRefreshingVisible = true,
                                nextPage = 2,
                                hasSpinner = false,
                                isListVisible = true
                            )
                        }
                }
            }
        }
    }
})