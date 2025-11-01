package com.example.inspectorappupdate.utils

import android.content.Context

// Class used to get the
class AppVersionViewer {

    companion object {
        public fun process(context: Context): String {
            val packageManager = context.packageManager
            val packageName = context.packageName
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val appData = StringBuilder()
            appData.append("Eduspector v")
            appData.append(packageInfo.versionName)
            return appData.toString()
        }
    }
}