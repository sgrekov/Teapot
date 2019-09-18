package com.factorymarket.rxelm.components.cachedpaging

import com.factorymarket.rxelm.cmd.Cmd

data class CachedPagingLoadItemsFromCacheCmd<FETCH_PARAMS>(
    val limit: Int,
    val params: FETCH_PARAMS
) : Cmd()

data class CachedPagingSyncItemsCmd<FETCH_PARAMS>(
    val page: Int,
    val pageSize: Int,
    val params: FETCH_PARAMS
) : Cmd()