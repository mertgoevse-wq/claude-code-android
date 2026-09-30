package dev.ccandroid.data.repository

import dev.ccandroid.core.AppError
import dev.ccandroid.core.ErrorCode
import dev.ccandroid.core.Outcome
import dev.ccandroid.core.tryCatch
import dev.ccandroid.data.dao.ConversationDao
import dev.ccandroid.data.dao.CostRecordDao
import dev.ccandroid.data.dao.MessageDao
import dev.ccandroid.data.dao.PlanDao
import dev.ccandroid.data.dao.ProjectDao
import dev.ccandroid.data.dao.ProjectSettingDao
import dev.ccandroid.data.dao.RunDao
import dev.ccandroid.data.dao.TurnDao
import dev.ccandroid.data.dao.VerificationDao
import dev.ccandroid.data.db.AppDatabase
import dev.ccandroid.data.entity.ConversationEntity
import dev.ccandroid.data.entity.CostRecordEntity
import dev.ccandroid.data.entity.MessageEntity
import dev.ccandroid.data.entity.MessagePartEntity
import dev.ccandroid.data.entity.PlanEntity
import dev.ccandroid.data.entity.PlanStepEntity
import dev.ccandroid.data.entity.ProjectEntity
import dev.ccandroid.data.entity.ProjectSettingEntity
import dev.ccandroid.data.entity.RunEntity
import dev.ccandroid.data.entity.TestResultEntity
import dev.ccandroid.data.entity.TurnEntity
import dev.ccandroid.data.entity.VerificationRunEntity
import dev.ccandroid.domain.Conversation
import dev.ccandroid.domain.CostRecord
import dev.ccandroid.domain.Message
import dev.ccandroid.domain.MessagePart
import dev.ccandroid.domain.Plan
import dev.ccandroid.domain.PlanStep
import dev.ccandroid.domain.Project
import dev.ccandroid.domain.ProjectSetting
import dev.ccandroid.domain.Run
import dev.ccandroid.domain.RunState
import dev.ccandroid.domain.TestResult
import dev.ccandroid.domain.Turn
import dev.ccandroid.domain.VerificationRun
import dev.ccandroid.domain.usecase.ConversationRepository
import dev.ccandroid.domain.usecase.CostRepository
import dev.ccandroid.domain.usecase.MessageRepository
import dev.ccandroid.domain.usecase.PlanRepository
import dev.ccandroid.domain.usecase.ProjectRepository
import dev.ccandroid.domain.usecase.ProjectSettingRepository
import dev.ccandroid.domain.usecase.RunRepository
import dev.ccandroid.domain.usecase.TurnRepository
import dev.ccandroid.domain.usecase.VerificationRepository

/**
 * The Room-backed implementations of the repository interfaces the use cases
 * declare in `shared/domain`.
 *
 * Three rules hold across every class in this file:
 *
 * - **Thin adapters.** The mapping lives on the entities (`fromDomain` /
 *   `toDomain`, P2-6); a repository translates a call, it does not decide
 *   anything. Logic in a repository cannot be tested without a database, and
 *   the use cases already own the logic.
 * - **Typed errors at the boundary.** Every method returns `Outcome<T>`; code
 *   rule 4 forbids a bare exception from crossing. Nothing in this file
 *   throws, except a cancellation, which is not an error.
 * - **No deletes.** Hard block 1 means no `DELETE` statement exists anywhere in
 *   the schema, so a repository interface that came out of the domain asking
 *   for one is answered with a policy refusal, not with a delete.
 *
 * The facade exists so the graph has one thing to inject: nine interfaces, one
 * object.
 */
public class RoomRepositories(private val database: AppDatabase) {

    public val project: RoomProjectRepository by lazy {
        RoomProjectRepository(database.projectDao(), database.projectSettingDao())
    }
    public val conversation: RoomConversationRepository by lazy {
        RoomConversationRepository(database.conversationDao())
    }
    public val turn: RoomTurnRepository by lazy {
        RoomTurnRepository(database.turnDao(), database.conversationDao())
    }
    public val message: RoomMessageRepository by lazy { RoomMessageRepository(database.messageDao()) }
    public val run: RoomRunRepository by lazy { RoomRunRepository(database.runDao()) }
    public val cost: RoomCostRepository by lazy { RoomCostRepository(database.costRecordDao()) }
    public val plan: RoomPlanRepository by lazy { RoomPlanRepository(database.planDao()) }
    public val verification: RoomVerificationRepository by lazy { RoomVerificationRepository(database.verificationDao()) }
    public val settings: RoomProjectSettingRepository by lazy { RoomProjectSettingRepository(database.projectSettingDao()) }
}

// --- project ------------------------------------------------------------

public class RoomProjectRepository(
    private val dao: ProjectDao,
    private val settingDao: ProjectSettingDao,
) : ProjectRepository {

    override suspend fun insert(project: Project): Outcome<Unit> {
        // The remote-owner index is unique. Pre-checking turns what would be a
        // driver exception with a code nobody reads into a named Conflict.
        if (dao.getProjectById(project.id) != null) {
            return Outcome.Failure(
                AppError.Conflict(
                    code = ErrorCode.PROJECT_ALREADY_EXISTS,
                    messageDe = "Ein Projekt mit dieser ID existiert bereits.",
                    messageEn = "A project with this id already exists.",
                    details = project.id,
                ),
            )
        }
        return tryCatch { dao.insertProject(ProjectEntity.fromDomain(project)) }
    }

    override suspend fun insertSetting(setting: ProjectSetting): Outcome<Unit> = tryCatch {
        settingDao.insertOrUpdateSetting(ProjectSettingEntity.fromDomain(setting))
    }

    override suspend fun getById(id: String): Outcome<Project?> = tryCatch {
        dao.getProjectById(id)?.toDomain()
    }

    override suspend fun getAll(): Outcome<List<Project>> = tryCatch {
        dao.getAllProjects().map { it.toDomain() }
    }

    override suspend fun update(project: Project): Outcome<Unit> = tryCatch {
        dao.updateProject(ProjectEntity.fromDomain(project))
    }

    override suspend fun updateSetting(setting: ProjectSetting): Outcome<Unit> = tryCatch {
        settingDao.insertOrUpdateSetting(ProjectSettingEntity.fromDomain(setting))
    }

    override suspend fun existsByRemote(owner: String, name: String): Outcome<Boolean> = tryCatch {
        dao.getProjectByRemote(owner, name) != null
    }

    override suspend fun getSetting(projectId: String): Outcome<ProjectSetting?> = tryCatch {
        settingDao.getSettingForProject(projectId)?.toDomain()
    }
}

// --- conversation ---------------------------------------------------------

public class RoomConversationRepository(
    private val dao: ConversationDao,
) : ConversationRepository {

    override suspend fun insert(conversation: Conversation): Outcome<Unit> = tryCatch {
        dao.insertConversation(ConversationEntity.fromDomain(conversation))
    }

    override suspend fun getById(id: String): Outcome<Conversation?> = tryCatch {
        dao.getConversationById(id)?.toDomain()
    }

    override suspend fun getByProject(projectId: String, includeArchived: Boolean): Outcome<List<Conversation>> = tryCatch {
        val conversations = dao.getConversationsForProject(projectId)
        (if (includeArchived) conversations else conversations.filter { !it.isArchived }).map { it.toDomain() }
    }

    override suspend fun update(conversation: Conversation): Outcome<Unit> = tryCatch {
        dao.updateConversation(ConversationEntity.fromDomain(conversation))
    }

    /**
     * Hard block 1: nothing is ever deleted, and no delete statement exists in
     * the schema to hide behind. The interface carries the method because the
     * use cases were written against the data model before the block was
     * applied to it; the implementation answers with the refusal the policy
     * defines, so a caller that tries gets a typed error instead of a
     * disappearing conversation.
     */
    override suspend fun delete(id: String): Outcome<Unit> = Outcome.Failure(
        AppError.PolicyRefusal(
            code = ErrorCode.POLICY_BLOCK_DELETE,
            messageDe = "Löschen ist dauerhaft deaktiviert. Archiviere stattdessen.",
            messageEn = "Deletion is permanently disabled. Archive instead.",
            command = "delete conversation $id",
        ),
    )
}

// --- turn -----------------------------------------------------------------

public class RoomTurnRepository(
    private val dao: TurnDao,
    private val conversationDao: ConversationDao,
) : TurnRepository {

    override suspend fun insert(turn: Turn): Outcome<Unit> {
        // The turn's conversation must exist: the foreign key makes it true on
        // Android, but the check makes the error typed on every driver.
        if (conversationDao.getConversationById(turn.conversationId) == null) {
            return Outcome.Failure(
                AppError.NotFound(
                    code = ErrorCode.PROJECT_NOT_FOUND,
                    messageDe = "Konversation nicht gefunden.",
                    messageEn = "Conversation not found.",
                    resourceId = turn.conversationId,
                ),
            )
        }
        return tryCatch { dao.insertTurn(TurnEntity.fromDomain(turn)) }
    }

    override suspend fun getById(id: String): Outcome<Turn?> = tryCatch {
        dao.getTurnById(id)?.toDomain()
    }

    override suspend fun getByConversation(conversationId: String): Outcome<List<Turn>> = tryCatch {
        dao.getTurnsForConversation(conversationId).map { it.toDomain() }
    }

    override suspend fun getLastTurn(conversationId: String): Outcome<Turn?> = tryCatch {
        dao.getLatestTurnForConversation(conversationId)?.toDomain()
    }

    override suspend fun update(turn: Turn): Outcome<Unit> = tryCatch {
        dao.updateTurn(TurnEntity.fromDomain(turn))
    }
}

// --- message --------------------------------------------------------------

public class RoomMessageRepository(private val dao: MessageDao) : MessageRepository {

    override suspend fun insert(message: Message): Outcome<Unit> = tryCatch {
        dao.insertMessage(MessageEntity.fromDomain(message))
    }

    override suspend fun insertPart(part: MessagePart): Outcome<Unit> = tryCatch {
        dao.insertMessagePart(MessagePartEntity.fromDomain(part))
    }

    override suspend fun getByTurn(turnId: String): Outcome<List<Message>> = tryCatch {
        dao.getMessagesForTurn(turnId).map { it.toDomain() }
    }

    override suspend fun getParts(messageId: String): Outcome<List<MessagePart>> = tryCatch {
        dao.getMessagePartsForMessage(messageId).map { it.toDomain() }
    }

    override suspend fun update(message: Message): Outcome<Unit> = tryCatch {
        dao.updateMessage(MessageEntity.fromDomain(message))
    }

    override suspend fun updatePart(part: MessagePart): Outcome<Unit> = tryCatch {
        dao.updateMessagePart(MessagePartEntity.fromDomain(part))
    }
}

// --- run --------------------------------------------------------------------

public class RoomRunRepository(private val dao: RunDao) : RunRepository {

    override suspend fun insert(run: Run): Outcome<Unit> = tryCatch {
        dao.insertRun(RunEntity.fromDomain(run))
    }

    override suspend fun getById(id: String): Outcome<Run?> = tryCatch {
        dao.getRunById(id)?.toDomain()
    }

    override suspend fun getByProject(projectId: String): Outcome<List<Run>> = tryCatch {
        dao.getRunsForProject(projectId).map { it.toDomain() }
    }

    override suspend fun getByState(state: RunState): Outcome<List<Run>> = tryCatch {
        dao.getRunsByState(state).map { it.toDomain() }
    }

    override suspend fun update(run: Run): Outcome<Unit> = tryCatch {
        dao.updateRun(RunEntity.fromDomain(run))
    }
}

// --- cost -------------------------------------------------------------------

public class RoomCostRepository(private val dao: CostRecordDao) : CostRepository {

    override suspend fun insert(record: CostRecord): Outcome<Unit> = tryCatch {
        dao.insertCostRecord(CostRecordEntity.fromDomain(record))
    }

    override suspend fun getByRunId(runId: String): Outcome<List<CostRecord>> = tryCatch {
        dao.getCostRecordsForRun(runId).map { it.toDomain() }
    }

    override suspend fun getByProjectId(projectId: String): Outcome<List<CostRecord>> = tryCatch {
        dao.getCostRecordsForProject(projectId).map { it.toDomain() }
    }

    /** No records yet sums to zero; null from SQLite is an empty sum, not an error. */
    override suspend fun getTotalCost(projectId: String): Outcome<Long> = tryCatch {
        dao.getTotalCostForProject(projectId) ?: 0L
    }

    override suspend fun getTotalCostByConversation(conversationId: String): Outcome<Long> = tryCatch {
        dao.getTotalCostForConversation(conversationId) ?: 0L
    }
}

// --- plan ---------------------------------------------------------------------

public class RoomPlanRepository(private val dao: PlanDao) : PlanRepository {

    override suspend fun insert(plan: Plan): Outcome<Unit> = tryCatch {
        // The DAO method is @Transaction, so plan and steps are one fact. The
        // ktx withTransaction extension is not used because its JVM artifact is
        // not on the test classpath (see shared/data/build.gradle.kts).
        dao.insertPlanWithSteps(
            PlanEntity.fromDomain(plan),
            plan.steps.map { PlanStepEntity.fromDomain(it) },
        )
    }

    override suspend fun getById(id: String): Outcome<Plan?> = tryCatch {
        dao.getPlanById(id)?.toDomain(dao.getPlanSteps(id).map { it.toDomain() })
    }

    override suspend fun getByRunId(runId: String): Outcome<Plan?> = tryCatch {
        dao.getPlanByRunId(runId)?.let { plan ->
            plan.toDomain(dao.getPlanSteps(plan.id).map { it.toDomain() })
        }
    }

    override suspend fun update(plan: Plan): Outcome<Unit> = tryCatch {
        // REPLACE on both tables makes this an upsert; the unique (planId,
        // ordinal) index means a changed step list rewrites itself, not duplicates.
        dao.insertPlanWithSteps(
            PlanEntity.fromDomain(plan),
            plan.steps.map { PlanStepEntity.fromDomain(it) },
        )
    }
}

// --- verification ----------------------------------------------------------------

public class RoomVerificationRepository(private val dao: VerificationDao) : VerificationRepository {

    override suspend fun insert(verificationRun: VerificationRun): Outcome<Unit> = tryCatch {
        dao.insertVerificationRun(VerificationRunEntity.fromDomain(verificationRun))
    }

    override suspend fun getById(id: String): Outcome<VerificationRun?> = tryCatch {
        dao.getVerificationRunById(id)?.toDomain()
    }

    override suspend fun getByRunId(runId: String): Outcome<List<VerificationRun>> = tryCatch {
        dao.getVerificationRunsForRun(runId).map { it.toDomain() }
    }

    override suspend fun update(verificationRun: VerificationRun): Outcome<Unit> = tryCatch {
        dao.updateVerificationRun(VerificationRunEntity.fromDomain(verificationRun))
    }

    /**
     * A verification run and its test results are one fact, not two: a run
     * whose results arrive separately could be read before they do.
     */
    public suspend fun complete(run: VerificationRun, results: List<TestResult>): Outcome<Unit> = tryCatch {
        dao.completeVerificationRun(
            VerificationRunEntity.fromDomain(run),
            results.map { TestResultEntity.fromDomain(it) },
        )
    }
}

// --- project settings ------------------------------------------------------------

public class RoomProjectSettingRepository(private val dao: ProjectSettingDao) : ProjectSettingRepository {

    override suspend fun getSetting(projectId: String): Outcome<ProjectSetting?> = tryCatch {
        dao.getSettingForProject(projectId)?.toDomain()
    }

    override suspend fun updateSetting(setting: ProjectSetting): Outcome<Unit> = tryCatch {
        dao.insertOrUpdateSetting(ProjectSettingEntity.fromDomain(setting))
    }
}
