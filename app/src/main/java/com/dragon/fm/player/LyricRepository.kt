package com.dragon.fm.player

import com.dragon.fm.data.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SongLyricResult(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val lines: List<LyricLine>
)

object LyricRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * 自动从网易云音乐与 Lrclib 检索并解析带时间轴的同步歌词 (LRC)
     */
    suspend fun fetchLyrics(title: String, artist: String, album: String = ""): SongLyricResult? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTrackName(title)
        val cleanArtist = cleanArtistName(artist)

        // 1. 优先尝试网易云音乐（华语流行、粤语、古典、现场曲库最全，带逐行翻译）
        val neteaseResult = fetchFromNetEase(cleanTitle, cleanArtist)
        if (neteaseResult != null && neteaseResult.lines.isNotEmpty()) {
            return@withContext neteaseResult
        }

        // 2. 备选尝试国际开源公有歌词库 Lrclib
        val lrclibResult = fetchFromLrclib(cleanTitle, cleanArtist)
        if (lrclibResult != null && lrclibResult.lines.isNotEmpty()) {
            return@withContext lrclibResult
        }

        // 3. 若使用艺术家+歌名未命中，仅用纯歌名做一次宽松搜索
        if (cleanArtist.isNotBlank()) {
            val fallbackResult = fetchFromNetEase(cleanTitle, "")
            if (fallbackResult != null && fallbackResult.lines.isNotEmpty()) {
                return@withContext fallbackResult
            }
        }

        return@withContext null
    }

    private fun fetchFromNetEase(title: String, artist: String): SongLyricResult? {
        try {
            val query = if (artist.isNotBlank()) "$title $artist" else title
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://music.163.com/api/search/get/web?s=$encodedQuery&type=1&offset=0&limit=3"

            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://music.163.com")
                .build()

            val searchBody = httpClient.newCall(searchReq).execute().use { it.body?.string().orEmpty() }
            if (searchBody.isBlank()) return null

            val searchJson = JSONObject(searchBody)
            val songs = searchJson.optJSONObject("result")?.optJSONArray("songs") ?: return null
            if (songs.length() == 0) return null

            val firstSong = songs.optJSONObject(0) ?: return null
            val songId = firstSong.optLong("id", 0L)
            if (songId <= 0L) return null

            val songName = firstSong.optString("name", title)
            val durationMs = firstSong.optLong("dt", 0L).takeIf { it > 0 }
                ?: firstSong.optLong("duration", 0L)

            // 获取歌词
            val lyricUrl = "https://music.163.com/api/song/lyric?os=pc&id=$songId&lv=-1&kv=-1&tv=-1"
            val lyricReq = Request.Builder()
                .url(lyricUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://music.163.com")
                .build()

            val lyricBody = httpClient.newCall(lyricReq).execute().use { it.body?.string().orEmpty() }
            if (lyricBody.isBlank()) return null

            val lyricJson = JSONObject(lyricBody)
            val rawLrc = lyricJson.optJSONObject("lrc")?.optString("lyric").orEmpty()
            val rawTrans = lyricJson.optJSONObject("tlyric")?.optString("lyric").orEmpty()

            if (rawLrc.isBlank() || rawLrc.contains("纯音乐，请欣赏")) return null

            val lrcLines = parseLrc(rawLrc)
            val transLines = if (rawTrans.isNotBlank()) parseLrc(rawTrans) else emptyList()

            val mergedLines = if (transLines.isNotEmpty()) {
                val transMap = transLines.associateBy { it.timeMs / 500 } // 容错 500ms 内对齐
                lrcLines.map { line ->
                    val trans = transMap[line.timeMs / 500]?.text
                    if (!trans.isNullOrBlank() && trans != line.text) {
                        line.copy(translation = trans)
                    } else {
                        line
                    }
                }
            } else {
                lrcLines
            }

            if (mergedLines.isEmpty()) return null

            val finalDuration = if (durationMs > 0) durationMs else (mergedLines.last().timeMs + 15_000L)
            return SongLyricResult(
                title = songName,
                artist = artist,
                durationMs = finalDuration,
                lines = mergedLines
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun fetchFromLrclib(title: String, artist: String): SongLyricResult? {
        try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val url = "https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist"

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "DragonFM/1.3.8 (Android; Mobile)")
                .build()

            val body = httpClient.newCall(req).execute().use { it.body?.string().orEmpty() }
            if (body.isBlank()) return null

            val json = JSONObject(body)
            val syncedLyrics = json.optString("syncedLyrics", "").trim()
            if (syncedLyrics.isBlank()) return null

            val durationSeconds = json.optDouble("duration", 0.0)
            val durationMs = (durationSeconds * 1000).toLong()

            val lines = parseLrc(syncedLyrics)
            if (lines.isEmpty()) return null

            val finalDuration = if (durationMs > 0) durationMs else (lines.last().timeMs + 15_000L)
            return SongLyricResult(
                title = json.optString("trackName", title),
                artist = json.optString("artistName", artist),
                durationMs = finalDuration,
                lines = lines
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * 高精度 LRC 标准解析器，支持多时间标签复合行、毫秒/厘秒自动换算与非法字符过滤
     */
    fun parseLrc(rawLrc: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        val timeTagRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")

        for (rawLine in rawLrc.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("[ti:") || line.startsWith("[ar:") ||
                line.startsWith("[al:") || line.startsWith("[by:") || line.startsWith("[offset:")
            ) {
                continue
            }

            val matches = timeTagRegex.findAll(line).toList()
            if (matches.isEmpty()) continue

            // 提取所有时间标签后的真实歌词文本
            val text = line.replace(timeTagRegex, "").trim()
            // 过滤无意义的标点符号或制作人员署名行
            if (text.isBlank()) continue

            for (match in matches) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val fracStr = match.groupValues[3]

                val ms = when (fracStr.length) {
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                    3 -> fracStr.toLongOrNull() ?: 0L
                    else -> 0L
                }

                val timestamp = min * 60_000L + sec * 1_000L + ms
                result.add(LyricLine(timeMs = timestamp, text = text))
            }
        }

        return result.sortedBy { it.timeMs }
    }

    private fun cleanTrackName(title: String): String {
        return title
            .replace(Regex("\\(.*?\\)"), "") // 去除括号内容如 (Live), (Remix)
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("【.*?】"), "")
            .replace(Regex("（.*?）"), "")
            .replace(Regex(" - .*"), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .split("/", "&", ",", "、", "feat.", "Feat.")
            .firstOrNull()?.trim() ?: artist.trim()
    }
}
