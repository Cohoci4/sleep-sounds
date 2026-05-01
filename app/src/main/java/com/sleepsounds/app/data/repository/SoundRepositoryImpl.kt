package com.sleepsounds.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sleepsounds.app.BuildConfig
import com.sleepsounds.app.data.local.PresetCatalog
import com.sleepsounds.app.data.local.db.dao.FavoriteDao
import com.sleepsounds.app.data.local.db.dao.SoundDao
import com.sleepsounds.app.data.local.db.entity.FavoriteEntity
import com.sleepsounds.app.data.local.db.entity.SoundEntity
import com.sleepsounds.app.di.IoDispatcher
import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.Tier
import com.sleepsounds.app.domain.repository.SoundRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SoundRepositoryImpl @Inject constructor(
    private val soundDao: SoundDao,
    private val favoriteDao: FavoriteDao,
    private val firestoreProvider: dagger.Lazy<FirebaseFirestore>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SoundRepository {

    init {
        // Bundled presets are always available offline. We seed them lazily
        // on first observation; see [observeSounds].
    }

    override fun observeSounds(): Flow<List<Sound>> {
        return combine(
            soundDao.observeAll(),
            kotlinx.coroutines.flow.flowOf(PresetCatalog.builtIn),
        ) { remote, presets ->
            // Merge presets with remote/generated entries by id, preferring
            // local entity data when present.
            val remoteIds = remote.map { it.id }.toSet()
            val merged = presets.filter { it.id !in remoteIds } + remote.map { it.toDomain() }
            merged.sortedByDescending { it.createdAtEpochMs }
        }.flowOn(ioDispatcher)
    }

    override fun observeByCategory(category: Category): Flow<List<Sound>> {
        return observeSounds().map { list -> list.filter { it.category == category } }
    }

    override suspend fun refreshCatalog(): Result<Unit> = withContext(ioDispatcher) {
        if (!BuildConfig.FIREBASE_ENABLED) {
            return@withContext Result.success(Unit)
        }
        runCatching {
            val snapshot = firestoreProvider.get()
                .collection("sounds")
                .get()
                .await()
            val sounds = snapshot.documents.mapNotNull { doc ->
                val audioUrl = doc.getString("audioUrl") ?: return@mapNotNull null
                val tier = Tier.valueOf(doc.getString("tier") ?: "FREE")
                Sound(
                    id = doc.id,
                    title = doc.getString("title") ?: doc.id,
                    description = doc.getString("description"),
                    source = SoundSource.Remote(audioUrl),
                    coverUrl = doc.getString("coverUrl"),
                    coverAsset = null,
                    tier = tier,
                    category = Category.fromSlug(doc.getString("category")),
                    durationSeconds = doc.getLong("durationSeconds")?.toInt(),
                    createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: 0L,
                )
            }
            soundDao.upsertAll(sounds.map(SoundEntity::fromDomain))
        }.onFailure { Timber.w(it, "refreshCatalog failed") }
    }

    override suspend fun toggleFavorite(soundId: String, favorite: Boolean) {
        withContext(ioDispatcher) {
            if (favorite) {
                favoriteDao.add(FavoriteEntity(soundId, System.currentTimeMillis()))
            } else {
                favoriteDao.remove(soundId)
            }
        }
    }

    override fun observeFavorites(): Flow<Set<String>> =
        favoriteDao.observeIds().map { it.toSet() }
}
