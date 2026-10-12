package com.hoangvnt.taichinh

import android.app.Application
import com.hoangvnt.taichinh.work.Notifications
import com.hoangvnt.taichinh.work.ReminderWorker

class TaiChinhApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannel(this)
        if (container.firebaseReady) ReminderWorker.schedule(this)
    }
}
