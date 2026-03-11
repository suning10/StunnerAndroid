package com.seclass.stunner.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.seclass.stunner.model.GoalEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalEventDao {

    @Insert
    suspend fun insert(event: GoalEvent): Long

    @Query("SELECT * FROM goal_event ORDER BY create_time DESC")
    fun getAllEvents(): Flow<List<GoalEvent>>

    @Query("SELECT * FROM goal_event WHERE create_time >= :sinceMs ORDER BY create_time DESC")
    fun getEventsSince(sinceMs: Long): Flow<List<GoalEvent>>

    @Query("SELECT COUNT(*) FROM goal_event WHERE goal = 1 AND create_time >= :sinceMs")
    suspend fun getGoalCount(sinceMs: Long): Int

    @Query("SELECT COUNT(*) FROM goal_event WHERE create_time >= :sinceMs")
    suspend fun getShotCount(sinceMs: Long): Int

    @Query("SELECT * FROM goal_event WHERE zone = :zone ORDER BY create_time DESC")
    fun getEventsByZone(zone: String): Flow<List<GoalEvent>>
}
