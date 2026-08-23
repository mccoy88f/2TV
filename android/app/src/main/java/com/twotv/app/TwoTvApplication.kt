package com.twotv.app

import android.app.Application
import com.twotv.app.data.local.AppDatabase

class TwoTvApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
