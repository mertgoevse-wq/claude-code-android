package dev.ccandroid.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.ccandroid.data.converter.TypeConverters as AppTypeConverters
import dev.ccandroid.data.dao.AppSettingDao
import dev.ccandroid.data.dao.BranchDao
import dev.ccandroid.data.dao.CheckpointDao
import dev.ccandroid.data.dao.ConversationDao
import dev.ccandroid.data.dao.CostRecordDao
import dev.ccandroid.data.dao.FileChangeDao
import dev.ccandroid.data.dao.MessageDao
import dev.ccandroid.data.dao.NotificationEventDao
import dev.ccandroid.data.dao.PlanDao
import dev.ccandroid.data.dao.ProjectDao
import dev.ccandroid.data.dao.ProjectSettingDao
import dev.ccandroid.data.dao.ProviderDao
import dev.ccandroid.data.dao.RemoteTargetDao
import dev.ccandroid.data.dao.RunDao
import dev.ccandroid.data.dao.SecretProfileDao
import dev.ccandroid.data.dao.SessionLogDao
import dev.ccandroid.data.dao.SkillDao
import dev.ccandroid.data.dao.ToolInvocationDao
import dev.ccandroid.data.dao.TurnDao
import dev.ccandroid.data.dao.VerificationDao
import dev.ccandroid.data.entity.AppSettingEntity
import dev.ccandroid.data.entity.BranchEntity
import dev.ccandroid.data.entity.CheckpointEntity
import dev.ccandroid.data.entity.ConversationEntity
import dev.ccandroid.data.entity.CostRecordEntity
import dev.ccandroid.data.entity.DiffEntryEntity
import dev.ccandroid.data.entity.FileChangeEntity
import dev.ccandroid.data.entity.MessageEntity
import dev.ccandroid.data.entity.MessagePartEntity
import dev.ccandroid.data.entity.ModelSpecEntity
import dev.ccandroid.data.entity.NotificationEventEntity
import dev.ccandroid.data.entity.PlanEntity
import dev.ccandroid.data.entity.PlanStepEntity
import dev.ccandroid.data.entity.ProjectEntity
import dev.ccandroid.data.entity.ProjectSettingEntity
import dev.ccandroid.data.entity.ProviderEntity
import dev.ccandroid.data.entity.RemoteTargetEntity
import dev.ccandroid.data.entity.RunEntity
import dev.ccandroid.data.entity.SecretProfileEntity
import dev.ccandroid.data.entity.SessionLogEntryEntity
import dev.ccandroid.data.entity.SkillEntity
import dev.ccandroid.data.entity.SkillInstallEntity
import dev.ccandroid.data.entity.TestResultEntity
import dev.ccandroid.data.entity.ToolInvocationEntity
import dev.ccandroid.data.entity.ToolResultEntity
import dev.ccandroid.data.entity.TurnEntity
import dev.ccandroid.data.entity.VerificationRunEntity

@Database(
    entities = [
        ProjectEntity::class,
        ProjectSettingEntity::class,
        ConversationEntity::class,
        TurnEntity::class,
        MessageEntity::class,
        MessagePartEntity::class,
        ToolInvocationEntity::class,
        ToolResultEntity::class,
        PlanEntity::class,
        PlanStepEntity::class,
        FileChangeEntity::class,
        DiffEntryEntity::class,
        VerificationRunEntity::class,
        TestResultEntity::class,
        CostRecordEntity::class,
        RunEntity::class,
        ProviderEntity::class,
        ModelSpecEntity::class,
        SecretProfileEntity::class,
        SkillEntity::class,
        SkillInstallEntity::class,
        RemoteTargetEntity::class,
        SessionLogEntryEntity::class,
        NotificationEventEntity::class,
        AppSettingEntity::class,
        BranchEntity::class,
        CheckpointEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun projectSettingDao(): ProjectSettingDao
    abstract fun conversationDao(): ConversationDao
    abstract fun turnDao(): TurnDao
    abstract fun messageDao(): MessageDao
    abstract fun toolInvocationDao(): ToolInvocationDao
    abstract fun planDao(): PlanDao
    abstract fun fileChangeDao(): FileChangeDao
    abstract fun verificationDao(): VerificationDao
    abstract fun costRecordDao(): CostRecordDao
    abstract fun runDao(): RunDao
    abstract fun providerDao(): ProviderDao
    abstract fun secretProfileDao(): SecretProfileDao
    abstract fun skillDao(): SkillDao
    abstract fun remoteTargetDao(): RemoteTargetDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun branchDao(): BranchDao
    abstract fun checkpointDao(): CheckpointDao

    companion object {
        const val DATABASE_NAME = "claude_code_android.db"
    }
}
