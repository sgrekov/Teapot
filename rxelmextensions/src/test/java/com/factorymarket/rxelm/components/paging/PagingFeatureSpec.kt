package com.factorymarket.rxelm.components.paging

import com.factorymarket.rxelm.cmd.CancelCmd
import com.factorymarket.rxelm.cmd.None
import com.factorymarket.rxelm.test.RxElmSpec
import io.reactivex.Single
import io.kotlintest.specs.DescribeSpec
import java.net.UnknownHostException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PagingFeatureSpec : DescribeSpec({

    describe("paging process without cache") {

        val pagingComponent: PagingFeature<String, Unit> =
            PagingFeature(object : PagingCommandsHandler<String, Unit> {

                override fun fetchPage(page: Int, params: Unit): Single<PagingResult<String>> {
                    return Single.just(PagingResult(listOf(), 4))
                }
            }, Unit)

        val spec = RxElmSpec(pagingComponent)

        context("a presenter") {
            it("should show full screen spinner, hide list and cancel previous request if any") {
                spec.withState(PagingState(fetchParams = Unit))
                    .whenMsg(PagingStartMsg())
                    .andCmdBatch(
                        CancelCmd(PagingLoadItemsCmd(1, Unit, "")),
                        CancelCmd(PagingRefreshItemsCmd(Unit, "")),
                        PagingRefreshItemsCmd(params = Unit)
                    )
                    .checkState {
                        assertTrue { it.isFullscreenSpinnerVisible }
                        assertTrue { it.isStarted }
                        assertFalse { it.isListVisible }
                        assertEquals(2, it.nextPage)
                        assertTrue { it.isPageLoading }
                    }
            }

            context("first page load finished with error") {
                it("should show fullscreen error") {
                    val onePageSpec = spec.copy()

                    onePageSpec.whenMsg(PagingErrorMsg(UnknownHostException(), PagingRefreshItemsCmd(Unit)))
                        .checkState {
                            assertFalse { it.isListVisible }
                            assertFalse { it.isRefreshingEnabled }
                            assertFalse { it.hasSpinner }
                            assertFalse { it.isFullscreenSpinnerVisible }
                            assertEquals(1, it.nextPage)
                            assertEquals(false, it.isPageLoading)
                            assertNull(it.totalPages)
                            assertTrue { it.isBlockedForLoadingNextPage() }
                        }
                }
            }

            context("only one page exist") {
                it("should show items and no loading spinners") {
                    val onePageSpec = spec.copy()

                    onePageSpec.whenMsg(PagingOnLoadedItemsMsg(getItems(), 1, 1))
                        .checkState {
                            assertTrue { it.isListVisible }
                            assertTrue { it.isRefreshingEnabled }
                            assertFalse { it.hasSpinner }
                            assertFalse { it.isFullscreenSpinnerVisible }
                            assertEquals(2, it.nextPage)
                            assertEquals(false, it.isPageLoading)
                            assertEquals(1, it.totalPages)
                            assertTrue { it.hasLoadedAllItems() }
                        }
                }
            }

            context("first from four page loaded") {
                it("should show list and spinner in the bottom") {
                    spec.whenMsg(PagingOnLoadedItemsMsg(getItems(), 4, 1))
                        .checkState {
                            assertTrue { it.isListVisible }
                            assertTrue { it.hasSpinner }
                            assertFalse { it.isFullscreenSpinnerVisible }
                            assertEquals(2, it.nextPage)
                            assertFalse { it.isPageLoading }
                            assertEquals(4, it.totalPages)
                        }
                }
            }
            context("pull to refresh") {
                it("should show refresh animation, enable refreshing, show list, refresh items  and cancel previous request if any") {
                    spec.copy()
                        .whenMsg(PagingOnSwipeMsg())
                        .andCmdBatch(
                            CancelCmd(PagingLoadItemsCmd(1, Unit, "")),
                            CancelCmd(PagingRefreshItemsCmd(Unit, "")),
                            PagingRefreshItemsCmd(params = Unit)
                        )
                        .checkState {
                            assertTrue { it.isRefreshingEnabled }
                            assertTrue { it.isRefreshingVisible }
                            assertTrue { it.isListVisible }
                            assertEquals(2, it.nextPage)
                            assertTrue { it.isPageLoading }
                        }
                }
            }
            context("paging will go ok") {
                it("should start loading second page and show spinner") {
                    spec.whenMsg(PagingOnScrolledToEndMsg())
                        .thenCmd(PagingLoadItemsCmd(2, Unit))
                        .diffState {
                            it.copy(
                                nextPage = 3,
                                isPageLoading = true
                            )
                        }
                }

                context("error while loading new page") {
                    val errorPageSpec = spec.copy()

                    it("should show error spinner in the bottom") {
                        val err = UnknownHostException()

                        errorPageSpec.whenMsg(
                            PagingErrorMsg(
                                err,
                                PagingLoadItemsCmd(2, Unit)
                            )
                        )
                            .thenCmd(LogThrowableCmd(err))
                            .diffState {
                                it.copy(
                                    nextPage = 2,
                                    isPageLoading = false,
                                    hasRetryButton = true,
                                    hasSpinner = false
                                )
                            }
                    }

                    it("should show loading spinner in the bottom on retry on the bottom clicked") {
                        errorPageSpec.whenMsg(
                            PagingOnRetryListButtonClickMsg()
                        )
                            .thenCmd(PagingLoadItemsCmd(2, Unit))
                            .diffState {
                                it.copy(
                                    nextPage = 3,
                                    isPageLoading = true,
                                    hasRetryButton = false,
                                    hasSpinner = true
                                )
                            }
                    }
                }

                context("first page is loaded") {
                    it("should stop loading and show new items") {
                        val newItems = getItems(10)
                        spec.whenMsg(
                            PagingOnLoadedItemsMsg(newItems, 4, 1)
                        )
                            .thenCmd(None)
                            .diffState {
                                it.copy(
                                    nextPage = 3,
                                    isPageLoading = false,
                                    items = it.items + newItems
                                )
                            }
                        assertEquals(20, spec.state().items.size)
                    }
                }

                context("scrolling to the end") {
                    it("should start loading third page and show spinner") {
                        spec.whenMsg(PagingOnScrolledToEndMsg())
                            .thenCmd(PagingLoadItemsCmd(3, Unit))
                            .diffState {
                                it.copy(
                                    nextPage = 4,
                                    isPageLoading = true
                                )
                            }
                    }
                }

                context("third page  loaded") {
                    it("should stop loading and show new items") {
                        val newItems = getItems(20)
                        spec.whenMsg(
                            PagingOnLoadedItemsMsg(newItems, 4, 3)
                        )
                            .thenCmd(None)
                            .diffState {
                                it.copy(
                                    nextPage = 4,
                                    isPageLoading = false,
                                    hasSpinner = true,
                                    items = it.items + newItems
                                )
                            }

                        assertEquals(30, spec.state().items.size)
                        assertFalse(spec.state().hasLoadedAllItems())
                    }
                }

                context("scrolling to the end ") {
                    it("should start loading forth page and show spinner") {
                        spec.whenMsg(PagingOnScrolledToEndMsg())
                            .thenCmd(PagingLoadItemsCmd(4, Unit))
                            .diffState {
                                it.copy(
                                    nextPage = 5,
                                    isPageLoading = true
                                )
                            }
                    }
                }

                context("forth page loaded") {
                    it("should stop loading, hide spinner and show new items") {
                        val newItems = getItems(30)
                        spec.whenMsg(
                            PagingOnLoadedItemsMsg(newItems, 4, 4)
                        )
                            .thenCmd(None)
                            .diffState {
                                it.copy(
                                    nextPage = 5,
                                    isPageLoading = false,
                                    hasSpinner = false,
                                    items = it.items + newItems
                                )
                            }
                    }

                    it("should show that paging is over") {
                        assertEquals(40, spec.state().items.size)
                        assertTrue(spec.state().hasLoadedAllItems())
                    }
                }
            }
        }
        context("a presenter in error state") {
            context("retry button click") {
                it("should show fullscreen spinner and refresh products") {
                    spec.withState(PagingState<String, Unit>(isStarted = true, fetchParams = Unit).toErrorState())
                        .whenMsg(PagingOnRetryAfterFullscreenErrorButtonClickMsg())
                        .thenCmd(PagingRefreshItemsCmd(Unit))
                        .checkState {
                            assertTrue { it.isFullscreenSpinnerVisible }
                            assertFalse { it.isListVisible }
                            assertTrue { it.isPageLoading }
                        }
                }
            }
        }
        context("a presenter with visible retry button") {
            context("retry button click") {
                it("should show spinner, hide retry button and continue loading items") {
                    spec.withState(PagingState(hasRetryButton = true, fetchParams = Unit))
                        .whenMsg(PagingOnRetryListButtonClickMsg())
                        .thenCmd(PagingLoadItemsCmd(1, Unit))
                        .checkState {
                            assertTrue { it.hasSpinner }
                            assertFalse { it.hasRetryButton }
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