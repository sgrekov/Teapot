package com.factorymarket.rxelm.log

enum class LogType {
    All, Updates, Commands, UpdatesAndCommands, None;

    fun needToShowCommands() : Boolean {
        return this == LogType.All
                || this == LogType.Commands
                || this == LogType.UpdatesAndCommands
    }

    fun needToShowUpdates() : Boolean {
        return this == LogType.All
                || this == LogType.Updates
                || this == LogType.UpdatesAndCommands
    }
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

    fun error(stateName: String, t: Throwable)

}