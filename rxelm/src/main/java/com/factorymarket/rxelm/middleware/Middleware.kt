package com.factorymarket.rxelm.middleware

import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.msg.Msg

interface Middleware {

    fun beforeUpdate(msg: Msg, oldState: State)

    fun afterUpdate(msg: Msg, newState: State)

}