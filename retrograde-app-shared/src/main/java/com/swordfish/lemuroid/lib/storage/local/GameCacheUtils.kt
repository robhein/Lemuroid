package com.swordfish.lemuroid.lib.storage.local

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.io.File

object GameCacheUtils {

    fun getCacheFileForGame(folderName: String, context: Context, game: Game): File {
        val gamesCachePath = buildPath(folderName, game.systemId)
        val gamesCacheDir = File(context.cacheDir, gamesCachePath)
        gamesCacheDir.mkdirs()
        return File(gamesCacheDir, game.fileName)
    }

    private fun buildPath(vararg chunks: String): String {
        return chunks.joinToString(separator = File.separator)
    }
}
