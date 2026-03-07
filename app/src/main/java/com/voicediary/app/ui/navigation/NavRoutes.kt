package com.voicediary.app.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val RECORD = "record/{type}"
    const val RESULT = "result/{type}"
    const val LIST = "list"
    const val DETAIL = "detail/{id}"

    fun record(type: String): String = "record/$type"
    fun result(type: String): String = "result/$type"
    fun detail(id: Long): String = "detail/$id"
}
