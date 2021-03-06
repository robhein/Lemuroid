package com.swordfish.lemuroid.lib.saves

import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.File

// TODO Since states are core related we should not put them in the same folder. This break previous versions states
// so I decided to manage a transition phase reading also the old directory. We should safely remove it in a few weeks.

class SavesManager(private val directoriesManager: DirectoriesManager) {

    fun getSaveRAM(game: Game): Maybe<ByteArray> = Maybe.fromCallable {
        val saveFile = getSaveFile(getSaveRAMFileName(game))
        if (saveFile.exists()) {
            saveFile.readBytes()
        } else {
            null
        }
    }

    fun setSaveRAM(game: Game, data: ByteArray): Completable = Completable.fromCallable {
        val saveFile = getSaveFile(getSaveRAMFileName(game))
        saveFile.writeBytes(data)
    }

    fun getSlotSave(game: Game, system: GameSystem, index: Int): Maybe<ByteArray> {
        assert(index in 0 until MAX_STATES)
        return getSaveState(getSlotSaveFileName(game, index), system.coreName)
    }

    fun setSlotSave(game: Game, data: ByteArray, system: GameSystem, index: Int): Completable {
        assert(index in 0 until MAX_STATES)
        return setSaveState(getSlotSaveFileName(game, index), system.coreName, data)
    }

    fun getAutoSave(game: Game, system: GameSystem) =
            getSaveState(getAutoSaveFileName(game), system.coreName)

    fun setAutoSave(game: Game, system: GameSystem, data: ByteArray) =
            setSaveState(getAutoSaveFileName(game), system.coreName, data)

    fun getSavedSlotsInfo(game: Game, coreName: String): Single<List<SaveInfos>> = Single.fromCallable {
        (0 until MAX_STATES)
                .map { getStateFileOrDeprecated(getSlotSaveFileName(game, it), coreName) }
                .map { SaveInfos(it.exists(), it.lastModified()) }
                .toList()
    }

    private fun getSaveState(fileName: String, coreName: String): Maybe<ByteArray> = Maybe.fromCallable {
        val saveFile = getStateFileOrDeprecated(fileName, coreName)
        if (saveFile.exists()) {
            saveFile.readBytes()
        } else {
            null
        }
    }

    private fun setSaveState(fileName: String, coreName: String, data: ByteArray) = Completable.fromCallable {
        val saveFile = getStateFile(fileName, coreName)
        saveFile.writeBytes(data)
    }

    private fun getSaveFile(fileName: String): File {
        val savesDirectory = directoriesManager.getSavesDirectory()
        return File(savesDirectory, fileName)
    }

    @Deprecated("Using this folder collisions might happen across different systems.")
    private fun getStateFileOrDeprecated(fileName: String, coreName: String): File {
        val stateFile = getStateFile(fileName, coreName)
        val deprecatedStateFile = getDeprecatedStateFile(fileName)
        return if (stateFile.exists() || !deprecatedStateFile.exists()) {
            stateFile
        } else {
            deprecatedStateFile
        }
    }

    private fun getStateFile(fileName: String, coreName: String): File {
        val statesDirectories = File(directoriesManager.getStatesDirectory(), coreName)
        statesDirectories.mkdirs()
        return File(statesDirectories, fileName)
    }

    @Deprecated("Using this folder collisions might happen across different systems.")
    private fun getDeprecatedStateFile(fileName: String): File {
        val statesDirectories = directoriesManager.getInternalStatesDirectory()
        return File(statesDirectories, fileName)
    }

    /** This name should make it compatible with RetroArch so that users can freely sync saves across the two application. */
    private fun getSaveRAMFileName(game: Game) = "${game.fileName.substringBeforeLast(".")}.srm"
    private fun getAutoSaveFileName(game: Game) = "${game.fileName}.state"
    private fun getSlotSaveFileName(game: Game, index: Int) = "${game.fileName}.slot${index + 1}"

    data class SaveInfos(val exists: Boolean, val date: Long)

    companion object {
        const val MAX_STATES = 4
    }
}
