package dev.ccandroid.domain.usecase

import dev.ccandroid.core.AppError
import dev.ccandroid.core.IdGenerator
import dev.ccandroid.core.Outcome
import dev.ccandroid.core.OutcomeException
import dev.ccandroid.core.tryCatch
import dev.ccandroid.domain.Project
import dev.ccandroid.domain.ProjectKind
import dev.ccandroid.domain.ProjectSetting

/**
 * Use cases for project lifecycle operations.
 *
 * Per docs/02-architecture/data-model.md and task P2-2:
 * - Create (local project)
 * - Clone (from remote)
 * - Rename
 * - Archive (no delete - hard block 1)
 *
 * All use cases are pure functions taking a repository interface as parameter.
 * The actual repository implementation lives in shared/data.
 */
public interface ProjectRepository {
    suspend fun insert(project: Project): Outcome<Unit>
    suspend fun insertSetting(setting: ProjectSetting): Outcome<Unit>
    suspend fun getById(id: String): Outcome<Project?>
    suspend fun getAll(): Outcome<List<Project>>
    suspend fun update(project: Project): Outcome<Unit>
    suspend fun updateSetting(setting: ProjectSetting): Outcome<Unit>
    suspend fun existsByRemote(owner: String, name: String): Outcome<Boolean>
    suspend fun getSetting(projectId: String): Outcome<ProjectSetting?>
}

/**
 * Creates a new local project on the device.
 */
public class CreateProjectUseCase(
    private val idGenerator: IdGenerator,
) {
    public suspend operator fun invoke(
        repo: ProjectRepository,
        name: String,
        path: String,
    ): Outcome<Project> = tryCatch {
        val id = idGenerator.newId("proj")
        val now = System.currentTimeMillis()
        val project = Project(
            id = id,
            name = name,
            kind = ProjectKind.LOCAL,
            path = path,
            createdAt = now,
            updatedAt = now,
        )
        val setting = ProjectSetting(
            projectId = id,
            modelProviderId = "anthropic",
            modelId = "claude-sonnet-4-5",
        )
        repo.insert(project).getOrThrow()
        repo.insertSetting(setting).getOrThrow()
        project
    }
}

/**
 * Clones a remote repository as a new project.
 */
public class CloneProjectUseCase(
    private val idGenerator: IdGenerator,
) {
    public suspend operator fun invoke(
        repo: ProjectRepository,
        name: String,
        path: String,
        remoteOwner: String,
        remoteName: String,
        defaultBranch: String,
    ): Outcome<Project> = tryCatch {
        // Check if already cloned
        val alreadyExists = repo.existsByRemote(remoteOwner, remoteName).getOrThrow()
        if (alreadyExists) {
            throw OutcomeException(AppError.Conflict(
                code = dev.ccandroid.core.ErrorCode.PROJECT_ALREADY_EXISTS,
                messageDe = "Dieses Repository wurde bereits geklont.",
                messageEn = "This repository has already been cloned.",
            ))
        }
        val id = idGenerator.newId("proj")
        val now = System.currentTimeMillis()
        val project = Project(
            id = id,
            name = name,
            kind = ProjectKind.CLONED,
            path = path,
            vcsProvider = "GITHUB",
            remoteOwner = remoteOwner,
            remoteName = remoteName,
            defaultBranch = defaultBranch,
            createdAt = now,
            updatedAt = now,
        )
        val setting = ProjectSetting(
            projectId = id,
            modelProviderId = "anthropic",
            modelId = "claude-sonnet-4-5",
        )
        repo.insert(project).getOrThrow()
        repo.insertSetting(setting).getOrThrow()
        project
    }
}

/**
 * Renames a project. The project's identity (id) is immutable.
 */
public class RenameProjectUseCase {
    public suspend operator fun invoke(
        repo: ProjectRepository,
        projectId: String,
        newName: String,
    ): Outcome<Project> = tryCatch {
        val project = repo.getById(projectId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Projekt nicht gefunden.",
                messageEn = "Project not found.",
            ))
        if (project.isArchived) {
            throw OutcomeException(AppError.Conflict(
                code = dev.ccandroid.core.ErrorCode.PROJECT_ARCHIVED,
                messageDe = "Ein archiviertes Projekt kann nicht umbenannt werden.",
                messageEn = "An archived project cannot be renamed.",
            ))
        }
        val updated = project.copy(
            name = newName,
            updatedAt = System.currentTimeMillis(),
        )
        repo.update(updated).getOrThrow()
        updated
    }
}

/**
 * Archives a project. This hides it from the active list but never deletes data.
 * Hard Block 1: Nothing is ever deleted.
 */
public class ArchiveProjectUseCase {
    public suspend operator fun invoke(
        repo: ProjectRepository,
        projectId: String,
    ): Outcome<Project> = tryCatch {
        val project = repo.getById(projectId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Projekt nicht gefunden.",
                messageEn = "Project not found.",
            ))
        val updated = project.copy(
            isArchived = true,
            updatedAt = System.currentTimeMillis(),
        )
        repo.update(updated).getOrThrow()
        updated
    }
}

/**
 * Unarchives a previously archived project.
 */
public class UnarchiveProjectUseCase {
    public suspend operator fun invoke(
        repo: ProjectRepository,
        projectId: String,
    ): Outcome<Project> = tryCatch {
        val project = repo.getById(projectId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Projekt nicht gefunden.",
                messageEn = "Project not found.",
            ))
        val updated = project.copy(
            isArchived = false,
            updatedAt = System.currentTimeMillis(),
        )
        repo.update(updated).getOrThrow()
        updated
    }
}

/**
 * Gets all projects, optionally including archived ones.
 */
public class GetProjectsUseCase {
    public suspend operator fun invoke(
        repo: ProjectRepository,
        includeArchived: Boolean = false,
    ): Outcome<List<Project>> = tryCatch {
        val all = repo.getAll().getOrThrow()
        if (includeArchived) all else all.filter { !it.isArchived }
    }
}

/**
 * Gets a single project by ID.
 */
public class GetProjectUseCase {
    public suspend operator fun invoke(
        repo: ProjectRepository,
        projectId: String,
    ): Outcome<Project?> = repo.getById(projectId)
}

/**
 * Updates project settings (autonomy level, model, verification commands, etc.).
 */
public class UpdateProjectSettingsUseCase {
    public suspend operator fun invoke(
        repo: ProjectRepository,
        projectId: String,
        settings: ProjectSetting,
    ): Outcome<ProjectSetting> = tryCatch {
        // Verify project exists
        val project = repo.getById(projectId).getOrThrow()
            ?: throw OutcomeException(AppError.NotFound(
                code = dev.ccandroid.core.ErrorCode.PROJECT_NOT_FOUND,
                messageDe = "Projekt nicht gefunden.",
                messageEn = "Project not found.",
            ))
        val updatedSettings = settings.copy(projectId = projectId)
        repo.updateSetting(updatedSettings).getOrThrow()
        updatedSettings
    }
}

/**
 * Gets project settings.
 */
public class GetProjectSettingsUseCase {
    public suspend operator fun invoke(
        repo: ProjectRepository,
        projectId: String,
    ): Outcome<ProjectSetting?> = repo.getSetting(projectId)
}