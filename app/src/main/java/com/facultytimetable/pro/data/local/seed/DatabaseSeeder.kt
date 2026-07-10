package com.facultytimetable.pro.data.local.seed

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor() {
    suspend fun seed() {
        // No-op: App starts with empty database
        // All data is created manually through the setup wizard
    }
}
