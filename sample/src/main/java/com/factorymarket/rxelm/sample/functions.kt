package com.factorymarket.rxelm.sample

import com.factorymarket.rxelm.msg.Idle
import com.factorymarket.rxelm.msg.Msg
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers

inline fun inView(crossinline operations: () -> Unit): Single<Msg> {
    return Single.fromCallable {
        operations()
    }.subscribeOn(AndroidSchedulers.mainThread()).map { Idle }
}