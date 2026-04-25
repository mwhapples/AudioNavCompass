/*
 * Copyright (C) 2026 Michael Whapples
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package app.audionav.compass

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "compass_settings")
private val ONBOARDING_VERSION_KEY = intPreferencesKey("onboarding_version")

class CompassSettings(private val context: Context) {
    fun onboardingVersionFlow(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_VERSION_KEY] ?: 0
    }
    suspend fun updateOnboardingVersion(version: Int) {
        context.dataStore.edit { it[ONBOARDING_VERSION_KEY] = version }
    }
}