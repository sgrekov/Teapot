package com.factorymarket.rxelm.program

import com.factorymarket.rxelm.msg.Msg

interface MessageConsumer {

    fun accept(msg : Msg)

}