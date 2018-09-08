package com.factorymarket.rxelm.log

interface RxElmLogger {

    /**
     * @param stateName usually serves as tag
     * @param message log message
     */
    fun log(stateName : String, message : String)

    fun showLog() : Boolean

}