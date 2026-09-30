package dev.ccandroid.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.ccandroid.data.entity.PlanEntity
import dev.ccandroid.data.entity.PlanStepEntity
import dev.ccandroid.domain.PlanStepState
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanSteps(steps: List<PlanStepEntity>)

    @Update
    suspend fun updatePlanStep(step: PlanStepEntity)

    @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: String): PlanEntity?

    @Query("SELECT * FROM plans WHERE runId = :runId LIMIT 1")
    suspend fun getPlanByRunId(runId: String): PlanEntity?

    @Query("SELECT * FROM plans WHERE runId = :runId LIMIT 1")
    fun observePlanByRunId(runId: String): Flow<PlanEntity?>

    @Query("SELECT * FROM plan_steps WHERE planId = :planId ORDER BY ordinal ASC")
    fun observePlanSteps(planId: String): Flow<List<PlanStepEntity>>

    @Query("SELECT * FROM plan_steps WHERE planId = :planId ORDER BY ordinal ASC")
    suspend fun getPlanSteps(planId: String): List<PlanStepEntity>

    @Query("UPDATE plan_steps SET state = :state, endedAt = :endedAt WHERE id = :stepId")
    suspend fun updatePlanStepState(stepId: String, state: PlanStepState, endedAt: Long? = System.currentTimeMillis())

    @Transaction
    suspend fun insertPlanWithSteps(plan: PlanEntity, steps: List<PlanStepEntity>) {
        insertPlan(plan)
        insertPlanSteps(steps)
    }
}
