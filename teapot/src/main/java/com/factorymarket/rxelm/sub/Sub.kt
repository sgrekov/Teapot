package com.factorymarket.rxelm.sub

import com.factorymarket.rxelm.contract.State
import com.factorymarket.rxelm.program.MessageConsumer

interface Sub<S : State> {

    fun setMessageConsumer(mc : MessageConsumer)

    fun subscribe(state: S)

    fun dispose()
}