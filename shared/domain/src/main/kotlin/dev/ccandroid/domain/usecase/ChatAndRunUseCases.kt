package dev.ccandroid.domain.usecase

import dev.ccandroid.core.AppError
import dev.ccandroid.core.IdGenerator
import dev.ccandroid.core.Outcome
import dev.ccandroid.core.OutcomeException
import dev.ccandroid.core.tryCatch
import dev.ccandroid.domain.Conversation
import dev.ccandroid.domain.Message
import dev.ccandroid.domain.MessagePart
import dev.ccandroid.domain.MessagePartKind
import dev.ccandroid.domain.MessageRole
import dev.ccandroid.domain.Run
import dev.ccandroid.domain.RunState
import dev.ccandroid.domain.Turn
import dev.ccandroid.domain.TurnState

/**
 * Repository interfaces for chat and run lifecycle.
 */
public interface ConversationRepository {
    suspend fun insert(conversation: Conversation): Outcome<Unit>
    suspend fun getById(id: String): Outcome<Conversation?>
    suspend fun getByProject(projectId: String, includeArchived: Boolean): Outcome<List<Conversation>>
    suspend fun update(conversation: Conversation): Outcome<Unit>
    suspend fun delete(id: String): Outcome<Unit>
}

public interface TurnRepository {
    suspend fun insert(turn: Turn): Outcome<Unit>
    suspend fun getById(id: String): Outcome<Turn?>
    suspend fun getByConversation(conversationId: String): Outcome<List<Turn>>
    suspend fun getLastTurn(conversationId: String): Outcome<Turn?>
    suspend fun update(turn: Turn): Outcome<Unit>
}

public interface MessageRepository {
    suspend fun insert(message: Message): Outcome<Unit>
    suspend fun insertPart(part: MessagePart): Outcome<Unit>
    suspend fun getByTurn(turnId: String): Outcome<List<Message>>
    suspend fun getParts(messageId: String): Outcome<List<MessagePart>>
    suspend fun update(message: Message): Outcome<Unit>
    suspend fun updatePart(part: MessagePart): Outcome<Unit>
}

public interface RunRepository {
    suspend fun insert(run: Run): Outcome<Unit>
    suspend fun getById(id: String): Outcome<Run?>
    suspend fun getByProject(projectId: String): Outcome<List<Run>>
    suspend fun getByState(state: RunState): Outcome<List<Run>>
    suspend fun update(run: Run): Outcome<Unit>
}

/**
 * Creates a new conversation for a project.
 */
public class CreateConversationUseCase(
    private val idGenerator: IdGenerator,
) {
    public suspend operator fun invoke(
        repo: ConversationRepository,
        projectId: String,
        sessionId: String,
        backendId: String? = null,
        initialTitle: String? = null,
    ): Outcome<Conversation> = tryCatch {
        val id = idGenerator.newId("conv")
        val now = System.currentTimeMillis()
        val title = initialTitle ?: "New conversation"
        val conversation = Conversation(
            id = id,
            projectId = projectId,
            title = title,
            sessionId = sessionId,
            backendId = backendId,
            createdAt = now,
            updatedAt = now,
            lastMessagePreview = null,
        )
        repo.insert(conversation).getOrThrow()
        conversation
    }
}

/**
 * Gets a conversation by ID.
 */
public class GetConversationUseCase {
    public suspend operator fun invoke(
        repo: ConversationRepository,
        conversationId: String,
    ): Outcome<Conversation?> = repo.getById(conversationId)
}

/**
 * Gets all conversations for a project.
 */
public class GetConversationsUseCase {
    public suspend operator fun invoke(
        repo: ConversationRepository,
        projectId: String,
        includeArchived: Boolean = false,
    ): Outcome<List<Conversation>> = repo.getByProject(projectId, includeArchived)
}

/**
 * Updates a conversation (title, archive status, last message preview).
 */
public class UpdateConversationUseCase {
    public suspend operator fun invoke(
        repo: ConversationRepository,
        conversationId: String,
        title: String? = null,
        isArchived: Boolean? = null,
        lastMessagePreview: String? = null,
    ): Outcome<Conversation> = tryCatch {
        val conversation = repo.getById(conversationId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Konversation nicht gefunden.",
                messageEn = "Conversation not found.",
            ))
        val updated = conversation.copy(
            title = title ?: conversation.title,
            isArchived = isArchived ?: conversation.isArchived,
            lastMessagePreview = lastMessagePreview ?: conversation.lastMessagePreview,
            updatedAt = System.currentTimeMillis(),
        )
        repo.update(updated).getOrThrow()
        updated
    }
}

/**
 * Archives a conversation (soft delete - hard block 1: no hard delete).
 */
public class ArchiveConversationUseCase {
    public suspend operator fun invoke(
        repo: ConversationRepository,
        conversationId: String,
    ): Outcome<Conversation> = tryCatch {
        val conversation = repo.getById(conversationId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Konversation nicht gefunden.",
                messageEn = "Conversation not found.",
            ))
        val updated = conversation.copy(
            isArchived = true,
            updatedAt = System.currentTimeMillis(),
        )
        repo.update(updated).getOrThrow()
        updated
    }
}

/**
 * Creates a new turn in a conversation.
 */
public class CreateTurnUseCase(
    private val idGenerator: IdGenerator,
) {
    public suspend operator fun invoke(
        repo: TurnRepository,
        conversationId: String,
        userMessageId: String,
    ): Outcome<Turn> = tryCatch {
        val lastTurn = repo.getLastTurn(conversationId).getOrThrow()
        val index = (lastTurn?.index ?: 0) + 1
        val id = idGenerator.newId("turn")
        val turn = Turn(
            id = id,
            conversationId = conversationId,
            index = index,
            userMessageId = userMessageId,
            startedAt = System.currentTimeMillis(),
            state = TurnState.PLANNING,
        )
        repo.insert(turn).getOrThrow()
        turn
    }
}

/**
 * Updates a turn's state and metadata.
 */
public class UpdateTurnUseCase {
    public suspend operator fun invoke(
        repo: TurnRepository,
        turnId: String,
        state: TurnState? = null,
        endedAt: Long? = null,
        runId: String? = null,
        costUsd: Long? = null,
        attemptCount: Int? = null,
    ): Outcome<Turn> = tryCatch {
        val turn = repo.getById(turnId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Turn nicht gefunden.",
                messageEn = "Turn not found.",
            ))
        val updated = turn.copy(
            state = state ?: turn.state,
            endedAt = endedAt ?: turn.endedAt,
            runId = runId ?: turn.runId,
            costUsd = costUsd ?: turn.costUsd,
            attemptCount = attemptCount ?: turn.attemptCount,
        )
        repo.update(updated).getOrThrow()
        updated
    }
}

/**
 * Gets all turns for a conversation.
 */
public class GetTurnsUseCase {
    public suspend operator fun invoke(
        repo: TurnRepository,
        conversationId: String,
    ): Outcome<List<Turn>> = repo.getByConversation(conversationId)
}

/**
 * Adds a message to a turn.
 */
public class AddMessageUseCase(
    private val idGenerator: IdGenerator,
) {
    public suspend operator fun invoke(
        msgRepo: MessageRepository,
        turnRepo: TurnRepository,
        turnId: String,
        role: MessageRole,
        content: String,
        parentToolUseId: String? = null,
    ): Outcome<Message> = tryCatch {
        val id = idGenerator.newId("msg")
        val now = System.currentTimeMillis()
        val message = Message(
            id = id,
            turnId = turnId,
            role = role,
            createdAt = now,
            renderedMarkdown = content,
            parentToolUseId = parentToolUseId,
        )
        msgRepo.insert(message).getOrThrow()
        message
    }
}

/**
 * Adds a message part to a message.
 */
public class AddMessagePartUseCase(
    private val idGenerator: IdGenerator,
) {
    public suspend operator fun invoke(
        msgRepo: MessageRepository,
        messageId: String,
        kind: MessagePartKind,
        ordinal: Int,
        payloadJson: String,
    ): Outcome<MessagePart> = tryCatch {
        val id = idGenerator.newId("part")
        val part = MessagePart(
            id = id,
            messageId = messageId,
            kind = kind,
            ordinal = ordinal,
            payloadJson = payloadJson,
        )
        msgRepo.insertPart(part).getOrThrow()
        part
    }
}

/**
 * Gets all messages for a turn with their parts.
 */
public class GetMessagesWithPartsUseCase {
    public suspend operator fun invoke(
        msgRepo: MessageRepository,
        turnId: String,
    ): Outcome<List<Pair<Message, List<MessagePart>>>> = tryCatch {
        val messages = msgRepo.getByTurn(turnId).getOrThrow()
        val result = mutableListOf<Pair<Message, List<MessagePart>>>()
        for (msg in messages) {
            val parts = msgRepo.getParts(msg.id).getOrThrow()
            result.add(Pair(msg, parts))
        }
        result
    }
}

/**
 * Creates a new run for a turn.
 */
public class CreateRunUseCase(
    private val idGenerator: IdGenerator,
) {
    public suspend operator fun invoke(
        runRepo: RunRepository,
        projectId: String,
        conversationId: String,
        turnId: String,
        backendId: String,
        backendProfile: String,
        taskText: String,
        maxAttempts: Int = 10,
    ): Outcome<Run> = tryCatch {
        val id = idGenerator.newId("run")
        val now = System.currentTimeMillis()
        val run = Run(
            id = id,
            projectId = projectId,
            conversationId = conversationId,
            turnId = turnId,
            backendId = backendId,
            backendProfile = backendProfile,
            state = RunState.CREATED,
            taskText = taskText,
            startedAt = now,
            maxAttempts = maxAttempts,
        )
        runRepo.insert(run).getOrThrow()
        run
    }
}

/**
 * Updates a run's state and metadata.
 */
public class UpdateRunUseCase {
    public suspend operator fun invoke(
        runRepo: RunRepository,
        runId: String,
        state: RunState? = null,
        branchName: String? = null,
        commitSha: String? = null,
        pullRequestUrl: String? = null,
        attemptCount: Int? = null,
        errorId: String? = null,
        isOffloaded: Boolean? = null,
    ): Outcome<Run> = tryCatch {
        val run = runRepo.getById(runId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Lauf nicht gefunden.",
                messageEn = "Run not found.",
            ))
        val now = System.currentTimeMillis()
        val endedAt = when {
            state == RunState.DONE || state == RunState.FAILED || state == RunState.CANCELLED || state == RunState.INTERRUPTED -> now
            else -> run.endedAt
        }
        val durationMs = endedAt?.let { it - run.startedAt } ?: run.durationMs
        val updated = run.copy(
            state = state ?: run.state,
            branchName = branchName ?: run.branchName,
            commitSha = commitSha ?: run.commitSha,
            pullRequestUrl = pullRequestUrl ?: run.pullRequestUrl,
            attemptCount = attemptCount ?: run.attemptCount,
            errorId = errorId ?: run.errorId,
            isOffloaded = isOffloaded ?: run.isOffloaded,
            endedAt = endedAt,
            durationMs = durationMs,
        )
        runRepo.update(updated).getOrThrow()
        updated
    }
}

/**
 * Gets runs for a project.
 */
public class GetRunsUseCase {
    public suspend operator fun invoke(
        runRepo: RunRepository,
        projectId: String,
    ): Outcome<List<Run>> = runRepo.getByProject(projectId)
}

/**
 * Gets the currently running run for a project.
 */
public class GetRunningRunUseCase {
    public suspend operator fun invoke(
        runRepo: RunRepository,
        projectId: String,
    ): Outcome<Run?> = tryCatch {
        val runs = runRepo.getByState(RunState.RUNNING).getOrThrow()
        runs.find { it.projectId == projectId }
    }
}