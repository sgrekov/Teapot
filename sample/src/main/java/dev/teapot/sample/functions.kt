package dev.teapot.sample

import dev.teapot.msg.Idle
import dev.teapot.msg.Msg
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers

inline fun inView(crossinline operations: () -> Unit): Single<Msg> {
    return Single.fromCallable {
        operations()
    }.subscribeOn(AndroidSchedulers.mainThread()).map { Idle }
}