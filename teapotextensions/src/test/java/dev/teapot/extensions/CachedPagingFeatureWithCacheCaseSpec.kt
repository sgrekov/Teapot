package dev.teapot.extensions

import dev.teapot.test.TeapotSpec
import dev.teapot.cmd.None
import dev.teapot.extensions.cachedpaging.*
import io.reactivex.Single
import io.kotlintest.specs.DescribeSpec

import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CachedPagingFeatureWithCacheCaseSpec : DescribeSpec({

    describe("second iteration of cacheable paging with cache") {

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

            it("should request data from cache and api, and do not show fullscreen loader") {
                spec.whenMsg(CachedPagingStartMsg)
                    .thenCmdBatch(
                            CachedPagingLoadItemsFromCacheCmd(CACHE_PAGING_PAGE_SIZE, Unit),
                            CachedPagingSyncItemsCmd(
                                    1,
                                    CACHE_PAGING_PAGE_SIZE, Unit
                            )
                    ).checkState {
                        assertTrue { it.isBlockedForLoadingNextPage() }
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

            context("first page arrived from cache") {
                it("should show cached list, also allow for new cache request ") {
                    val items = getItemsForPaging()
                    spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(items, CACHE_PAGING_PAGE_SIZE))
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
                                items = items,
                                isSyncing = true,
                                isCacheLoading = false,
                                hasSpinner = true
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
                        }
                        .diffState { prevState ->
                            prevState.copy(
                                isFullscreenSpinnerVisible = false,
                                isListVisible = true,
                                isSyncing = false,
                                isCacheLoading = true,
                                totalPages = CACHE_PAGING_TOTAL_PAGES,
                                hasSpinner = true
                            )
                        }
                }
            }

            context("fresh cache arrived") {
                it("should show list of fresh cached elements and allow new cache requests") {
                    val list = getItemsForPaging(size = 10)

                    spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(list, CACHE_PAGING_PAGE_SIZE))
                        .thenCmd(None)
                        .diffState { oldState ->
                            oldState.copy(
                                items = list,
                                isFullscreenSpinnerVisible = false,
                                isListVisible = true,
                                isSyncing = false,
                                isCacheLoading = false,
                                isRefreshingEnabled = true,
                                isRefreshingVisible = false,
                                hasSpinner = true
                            )
                        }.checkState {
                            assertFalse { it.isBlockedForLoadingNextPage() }
                            assertFalse { it.hasLoadedAllItems() }
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

                context("two pages from cache arrived") {
                    it("should show two pages of items, show loader in the bottom, and allow new cache request ") {
                        val twoPageList = getItemsForPaging(size = 2 * CACHE_PAGING_PAGE_SIZE)
                        spec.whenMsg(CachedPagingItemsLoadedFromCacheMsg(twoPageList, CACHE_PAGING_PAGE_SIZE * 2))
                            .thenCmd(None)
                            .checkState {
                                assertFalse { it.isBlockedForLoadingNextPage() }
                                assertFalse { it.hasLoadedAllItems() }
                            }
                            .diffState { prevState ->
                                prevState.copy(
                                    items = twoPageList,
                                    isSyncing = true,
                                    isCacheLoading = false
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
                                    hasSpinner = true
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

                    context("three page list from cache arrived") {
                        it("should show three page list of items, show loader in the bottom and not allow new cache request ") {
                            spec.copy().whenMsg(
                                    CachedPagingItemsLoadedFromCacheMsg(
                                            getItemsForPaging(size = 30),
                                            CACHE_PAGING_PAGE_SIZE * 3
                                    )
                            )
                                .thenCmd(None)
                                .checkState {
                                    assertFalse { it.isBlockedForLoadingNextPage() }
                                    assertFalse { it.hasLoadedAllItems() }
                                }
                                .diffState { prevState ->
                                    prevState.copy(
                                        items = getItemsForPaging(size = 30),
                                        isSyncing = true,
                                        isCacheLoading = false,
                                        hasSpinner = true
                                    )
                                }
                        }
                    }

                    context("two and a half page list from cache arrived") {
                        it("should show two and a half page list of items, show loader in the bottom and not allow new cache request ") {
                            spec.whenMsg(
                                    CachedPagingItemsLoadedFromCacheMsg(
                                            getItemsForPaging(size = 25),
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
                                        items = getItemsForPaging(size = 25),
                                        hasSpinner = true,
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
                                        isCacheLoading = true,
                                        isSyncing = false
                                    )
                                }
                        }
                    }

                    context("fresh cache of 3 page list arrived ") {
                        it("should show list of fresh cached 3 page of elements and not allow new cache requests") {
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