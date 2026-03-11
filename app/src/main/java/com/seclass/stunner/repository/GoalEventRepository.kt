package com.seclass.stunner.repository

import com.seclass.stunner.model.GoalEvent
import kotlinx.coroutines.flow.Flow

class GoalEventRepository(private val dao: GoalEventDao) {

    fun getAllEvents(): Flow<List<GoalEvent>> = dao.getAllEvents()

    fun getEventsSince(sinceMs: Long): Flow<List<GoalEvent>> = dao.getEventsSince(sinceMs)

    suspend fun insert(event: GoalEvent): Long = dao.insert(event)

    suspend fun getGoalCount(sinceMs: Long): Int = dao.getGoalCount(sinceMs)

    suspend fun getShotCount(sinceMs: Long): Int = dao.getShotCount(sinceMs)
}
