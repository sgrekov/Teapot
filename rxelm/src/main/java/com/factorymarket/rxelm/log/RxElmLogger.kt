package com.factorymarket.rxelm.log

enum class LogType {
    All, Updates, Commands, None
}

interface RxElmLogger {

    fun logType(): LogType {
        return LogType.All
    }

    /**
     * @param stateName usually serves as tag
     * @param message log message
     */
    fun log(stateName: String, message: String)

    fun error(t: Throwable)

}