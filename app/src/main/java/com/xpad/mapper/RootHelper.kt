package com.xpad.mapper

import java.io.File

/** Проверка наличия root (для честной справки о возможностях глобального ремапа). */
object RootHelper {
    fun isRootAvailable(): Boolean {
        return runCatching {
            val paths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su")
            paths.any { File(it).exists() }
        }.getOrDefault(false)
    }
}