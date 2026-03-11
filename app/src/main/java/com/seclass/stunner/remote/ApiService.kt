package com.seclass.stunner.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/goal-events")
    suspend fun postGoalEvent(@Body request: GoalEventRequest): Response<Unit>
}
