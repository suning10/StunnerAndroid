package com.seclass.stunner.repository

import android.util.Log
import com.seclass.stunner.model.GoalEvent
import com.seclass.stunner.remote.ApiClient
import com.seclass.stunner.remote.GoalEventRequest

/**
 * Posts goal events to the configurable REST API.
 *
 * Failures are logged but do not interrupt the local Room save — the app
 * works offline and syncs opportunistically.
 */
class ApiRepository(private val settingsRepository: SettingsRepository) {

    suspend fun postGoalEvent(event: GoalEvent): Result<Unit> {
        val url = settingsRepository.apiBaseUrl
        return try {
            val response = ApiClient.create(url).postGoalEvent(GoalEventRequest.from(event))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server error ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post goal event to $url: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ApiRepository"
    }
}
