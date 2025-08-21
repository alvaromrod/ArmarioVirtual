package com.pep1lo.armariovirtual

import android.app.Application
import com.pep1lo.armariovirtual.data.AppDatabase

class WardrobeApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
