package com.sleepsounds.app.data.repository

import com.sleepsounds.app.data.local.db.dao.GenerationDao
import com.sleepsounds.app.data.local.db.dao.SoundDao
import com.sleepsounds.app.data.local.db.entity.GenerationEntity
import com.sleepsounds.app.data.local.db.entity.SoundEntity
import com.sleepsounds.app.data.remote.api.SleepSoundsApi
import com.sleepsounds.app.data.remote.dto.GenerationRequestDto
import com.sleepsounds.app.di.IoDispatcher
import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.model.DreamGeneration
import com.sleepsounds.app.domain.model.GenerationStage
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.Tier
import com.sleepsounds.app.domain.repository.AuthRepository
import com.sleepsounds.app.domain.repository.GenerationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class GenerationRepositoryImpl @Inject constructor(
    private val api: SleepSoundsApi,
    private val authRepository: AuthRepository,
    private val soundDao: SoundDao,
    private val generationDao: GenerationDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GenerationRepository {

    override fun observeHistory(): Flow<List<DreamGeneration>> {
        return generationDao.observeAll().map { entries ->
            entries.mapNotNull { entry ->
                val sound = soundDao.getById(entry.soundId)?.toDomain() ?: return@mapNotNull null
                DreamGeneration(
                    id = entry.id,
                    prompt = entry.prompt,
                    sound = sound,
                    createdAtEpochMs = entry.createdAtEpochMs,
                )
            }
        }.flowOn(ioDispatcher)
    }

    override fun generate(prompt: String): Flow<GenerationStage> = flow {
        emit(GenerationStage.SubmittingPrompt)
        // Make sure we have a Firebase user; the OkHttp interceptor pulls the
        // ID token from this user when attaching `Authorization: Bearer ...`.
        val userIdResult = authRepository.ensureSignedIn()
        userIdResult.getOrElse {
            emit(GenerationStage.Failed(it))
            return@flow
        }
        try {
            // The Cloud Function does both image and audio generation as a single call,
            // but we surface granular progress for the UX.
            emit(GenerationStage.GeneratingCover)
            // Small delay so the UI stage is visible even on fast responses.
            delay(MIN_STAGE_VISIBILITY_MS)
            emit(GenerationStage.GeneratingAudio)
            val response = api.generateSound(GenerationRequestDto(prompt))
            emit(GenerationStage.Finalizing)
            val sound = Sound(
                id = response.id,
                title = response.title ?: "Dream: ${prompt.take(40)}",
                description = prompt,
                source = SoundSource.Remote(response.audioUrl),
                coverUrl = response.coverUrl,
                tier = Tier.AI_GENERATED,
                category = Category.AI_EXCLUSIVE,
                durationSeconds = response.durationSeconds,
                createdAtEpochMs = System.currentTimeMillis(),
            )
            soundDao.upsert(SoundEntity.fromDomain(sound))
            generationDao.insert(
                GenerationEntity(
                    id = response.id,
                    prompt = prompt,
                    soundId = sound.id,
                    createdAtEpochMs = sound.createdAtEpochMs,
                )
            )
            emit(GenerationStage.Done(DreamGeneration(response.id, prompt, sound, sound.createdAtEpochMs)))
        } catch (t: Throwable) {
            Timber.w(t, "generate failed")
            emit(GenerationStage.Failed(t))
        }
    }.flowOn(ioDispatcher)

    private companion object {
        const val MIN_STAGE_VISIBILITY_MS = 600L
    }
}
