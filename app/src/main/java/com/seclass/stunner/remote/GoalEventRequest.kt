package com.seclass.stunner.remote

import com.google.gson.annotations.SerializedName
import com.seclass.stunner.model.GoalEvent

data class GoalEventRequest(
    val goal: Boolean,
    val accuracy: Double,
    val zone: String?,
    @SerializedName("device_id") val deviceId: String?,
    @SerializedName("create_time") val createTime: Long
) {
    companion object {
        fun from(event: GoalEvent) = GoalEventRequest(
            goal = event.goal,
            accuracy = event.accuracy,
            zone = event.zone,
            deviceId = event.deviceId,
            createTime = event.createTime
        )
    }
}
