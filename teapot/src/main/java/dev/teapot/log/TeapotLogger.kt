package dev.teapot.log

enum class LogType {
    All, Updates, Commands, UpdatesAndCommands, None;

    fun needToShowCommands() : Boolean {
        return this == All
                || this == Commands
                || this == UpdatesAndCommands
    }

    fun needToShowUpdates() : Boolean {
        return this == All
                || this == Updates
                || this == UpdatesAndCommands
    }
}

interface TeapotLogger {

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