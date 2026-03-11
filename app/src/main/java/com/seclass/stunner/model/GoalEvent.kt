package com.seclass.stunner.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the goal_event table in db.sql.
 *
 * accuracy: placement score [0.0–1.0], where 1.0 = dead center of goal.
 * zone:     GoalZone.name(), null for a miss.
 */
@Entity(tableName = "goal_event")
data class GoalEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val goal: Boolean,

    val accuracy: Double,

    val zone: String?,

    @ColumnInfo(name = "device_id")
    val deviceId: String?,

    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis()
)
