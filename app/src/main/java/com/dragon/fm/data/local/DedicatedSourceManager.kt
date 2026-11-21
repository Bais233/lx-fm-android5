package com.dragon.fm.data.local

import com.dragon.fm.data.api.DedicatedRadioApi
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.StreamLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages high-reliability official dedicated broadcast streams.
 * Handles province-based fetching, in-memory caching, name normalization,
 * and dual-source line merging (Line 1: 官方专线, Line 2: 公网备用).
 */
object DedicatedSourceManager {

    private val api = DedicatedRadioApi()
    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    @Volatile
    var currentSourceMode: com.dragon.fm.data.model.StreamSourceMode = com.dragon.fm.data.model.StreamSourceMode.HYBRID

    // In-memory cache of dedicated stations: provinceCode -> list
    private val provinceCache = ConcurrentHashMap<Int, List<RadioStation>>()
    private val provinceCacheTime = ConcurrentHashMap<Int, Long>()
    private const val CACHE_TTL_MS = 3600_000L // 1 hour TTL

    private val REGEX_CNR = Regex("CNR[-\\s]*\\d*", RegexOption.IGNORE_CASE)
    private val REGEX_CCTV = Regex("CCTV[-\\s]*\\d*", RegexOption.IGNORE_CASE)
    private val REGEX_FM = Regex("FM\\s*\\d+(\\.\\d+)?", RegexOption.IGNORE_CASE)
    private val REGEX_AM = Regex("AM\\s*\\d+", RegexOption.IGNORE_CASE)
    private val REGEX_STATION_SUFFIX = Regex("中央人民广播电台|中央广播电视总台|(省|市|县|区)?(广播电视台|人民广播电台|融媒体中心|传媒集团|广播电台|人民广播|广播网|调频|频率|电台)")
    private val REGEX_PUNCTUATION = Regex("[()（）\\[\\]【】\\-\\s•·_:]")
    private val normalizedNameCache = ConcurrentHashMap<String, String>()

    /**
     * Common Chinese broadcast functional categories & channel types.
     * These terms describe channel format/genres rather than unique local stations.
     * To prevent local municipal/county stations (e.g. "怀集音乐之声", "抚顺音乐之声")
     * from being hijacked by national standalone stations (e.g. "音乐之声"),
     * any station matching these terms MUST NOT be fuzzy matched via contains.
     */
    val GENERIC_CHANNEL_TERMS = setOf(
        "音乐之声", "经济之声", "文艺之声", "中国之声", "大湾区之声", "台海之声",
        "神州之声", "香港之声", "民族之声", "老年之声", "经典音乐广播", "中国交通广播",
        "交通广播", "新闻广播", "音乐广播", "文艺广播", "经济广播", "交通之声",
        "新闻之声", "城市之声", "新闻综合", "新闻综合广播", "都市生活", "都市生活广播",
        "都市广播", "农村广播", "故事广播", "戏曲广播", "旅游广播", "健康广播",
        "生活广播", "少儿广播", "青年广播", "私家车广播", "私家车之声", "经典流行",
        "流行音乐", "流行音乐广播", "汽车广播", "综合广播", "综合台", "新闻台",
        "音乐台", "交通台", "经济台", "文艺台", "经典台", "经典音乐", "新闻资讯",
        "流行金曲", "流金岁月"
    )

    var onPoolRefreshedListener: (() -> Unit)? = null

    /**
     * Checks if a stream URL (specifically from official dedicated source like radio.cn)
     * contains timestamped anti-leech tokens and has expired or is nearing expiration.
     */
    fun isStreamUrlExpired(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        if (url.contains("radio.cn") && url.contains("time=") && url.contains("key=")) {
            try {
                val timeStr = url.substringAfter("time=").substringBefore("&")
                val tokenSec = timeStr.toLongOrNull(16) ?: timeStr.toLongOrNull(10) ?: return false
                val currentSec = System.currentTimeMillis() / 1000
                val ageSec = currentSec - tokenSec
                // If token was issued more than 60 minutes ago (3600s), or negative anomaly (> 10 min into future), mark expired
                if (ageSec > 3600L || ageSec < -600L) {
                    return true
                }
            } catch (_: Exception) {
                return false
            }
        }
        return false
    }

    // Province Name -> Province Code mapping
    val provinceCodeMap = mapOf(
        "全国" to 0, "国家" to 0, "央广" to 0,
        "北京" to 110000,
        "河北" to 130000,
        "山西" to 140000,
        "内蒙古" to 150000,
        "辽宁" to 210000,
        "吉林" to 220000,
        "黑龙江" to 230000,
        "上海" to 310000,
        "江苏" to 320000,
        "浙江" to 330000,
        "安徽" to 340000,
        "福建" to 350000,
        "江西" to 360000,
        "山东" to 370000,
        "河南" to 410000,
        "湖北" to 420000,
        "湖南" to 430000,
        "广东" to 440000,
        "广西" to 450000,
        "海南" to 460000,
        "重庆" to 500000,
        "四川" to 510000,
        "贵州" to 520000,
        "云南" to 530000,
        "西藏" to 540000,
        "陕西" to 610000,
        "甘肃" to 620000,
        "青海" to 630000,
        "宁夏" to 640000,
        "新疆" to 650000,
        "新疆兵团" to 660000
    )

    val cityToProvinceMap = mapOf(
        "广州" to "广东", "深圳" to "广东", "珠海" to "广东", "佛山" to "广东", "东莞" to "广东",
        "成都" to "四川", "绵阳" to "四川",
        "武汉" to "湖北", "宜昌" to "湖北",
        "长沙" to "湖南", "株洲" to "湖南",
        "杭州" to "浙江", "宁波" to "浙江", "温州" to "浙江",
        "南京" to "江苏", "苏州" to "江苏", "无锡" to "江苏", "常州" to "江苏",
        "西安" to "陕西",
        "济南" to "山东", "青岛" to "山东",
        "郑州" to "河南", "洛阳" to "河南",
        "沈阳" to "辽宁", "大连" to "辽宁",
        "哈尔滨" to "黑龙江",
        "长春" to "吉林",
        "太原" to "山西", "大同" to "山西",
        "石家庄" to "河北", "唐山" to "河北",
        "福州" to "福建", "厦门" to "福建",
        "南昌" to "江西",
        "合肥" to "安徽",
        "海口" to "海南", "三亚" to "海南",
        "南宁" to "广西", "桂林" to "广西",
        "贵阳" to "贵州",
        "昆明" to "云南",
        "拉萨" to "西藏",
        "兰州" to "甘肃",
        "西宁" to "青海",
        "银川" to "宁夏",
        "乌鲁木齐" to "新疆",
        "呼和浩特" to "内蒙古", "包头" to "内蒙古"
    )

    init {
        startBackgroundWarmup()
    }

    /**
     * Asynchronously pre-warms national and core provincial broadcast stations.
     * Replicates LX FM PC background warmup mechanism to provide 0ms instant discovery.
     */
    fun startBackgroundWarmup() {
        scope.launch {
            try {
                getNationalStations()
                val popularProvinces = listOf(
                    "北京", "广东", "上海", "浙江", "江苏", "四川", "河南", "湖北", "湖南", "山东", "山西", "陕西", "河北"
                )
                for (p in popularProvinces) {
                    if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) break
                    delay(120)
                    getStationsByProvince(p)
                }
            } catch (_: Exception) {}
        }
    }

    fun resolveProvinceCode(provinceName: String?): Int? {
        if (provinceName.isNullOrBlank()) return null
        val clean = provinceName.trim()
            .removeSuffix("省")
            .removeSuffix("市")
            .removeSuffix("自治区")
            .removeSuffix("特别行政区")
            .removeSuffix("壮族自治区")
            .removeSuffix("回族自治区")
            .removeSuffix("维吾尔自治区")
        return provinceCodeMap[clean]
    }

    /**
     * Get dedicated stations for national channels (中国之声, 经济之声, 音乐之声, etc.)
     */
    suspend fun getNationalStations(forceRefresh: Boolean = false): List<RadioStation> = withContext(Dispatchers.IO) {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return@withContext emptyList()
        val cached = provinceCache[0]
        val cacheTime = provinceCacheTime[0] ?: 0L
        val isFresh = (System.currentTimeMillis() - cacheTime) < CACHE_TTL_MS && !isStreamUrlExpired(cached?.firstOrNull()?.playUrl)
        if (!forceRefresh && cached != null && isFresh) {
            return@withContext cached
        }
        val res = api.getBroadcastList(categoryId = 0, provinceCode = 0, provinceName = "全国")
        val list = res.getOrDefault(emptyList())
        if (list.isNotEmpty()) {
            provinceCache[0] = list
            provinceCacheTime[0] = System.currentTimeMillis()
            try {
                onPoolRefreshedListener?.invoke()
            } catch (_: Exception) {}
        }
        list
    }

    /**
     * Instant synchronous access to in-memory cached national stations (0ms).
     */
    fun getCachedNationalStations(): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return emptyList()
        return provinceCache[0] ?: emptyList()
    }

    /**
     * Instant synchronous access to in-memory cached province stations (0ms).
     */
    fun getCachedStationsByProvince(provinceName: String): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return emptyList()
        val code = resolveProvinceCode(provinceName) ?: return emptyList()
        return provinceCache[code] ?: emptyList()
    }

    /**
     * Instant synchronous search in cached dedicated station pool by multi-terms (0ms).
     * Supports matching by title keywords, tags, or current program.
     */
    fun searchCachedDedicatedStations(terms: List<String>): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return emptyList()
        val cleanTerms = terms.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        if (cleanTerms.isEmpty()) return emptyList()
        val candidatePool = provinceCache.values.flatten().distinctBy { it.id }
        return candidatePool.filter { st ->
            val name = st.name.lowercase()
            val norm = normalizeName(st.name).lowercase()
            val tags = st.tags.lowercase()
            val prog = (st.currentProgram ?: "").lowercase()
            cleanTerms.any { t ->
                name.contains(t) || norm.contains(t) || tags.contains(t) || prog.contains(t)
            }
        }
    }

    /**
     * Instant synchronous search in cached dedicated station pool by single query (0ms).
     */
    fun searchCachedDedicatedStations(query: String): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return emptyList()
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return emptyList()
        return searchCachedDedicatedStations(listOf(cleanQ))
    }

    fun clearCache() {
        provinceCache.clear()
        provinceCacheTime.clear()
    }

    /**
     * Fetch dedicated stations for a specific province (e.g. 河南 -> 410000, 广东 -> 440000).
     */
    suspend fun getStationsByProvince(provinceName: String, forceRefresh: Boolean = false): List<RadioStation> = withContext(Dispatchers.IO) {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return@withContext emptyList()
        val code = resolveProvinceCode(provinceName) ?: return@withContext emptyList()
        val cached = provinceCache[code]
        val cacheTime = provinceCacheTime[code] ?: 0L
        val isFresh = (System.currentTimeMillis() - cacheTime) < CACHE_TTL_MS && !isStreamUrlExpired(cached?.firstOrNull()?.playUrl)
        if (!forceRefresh && cached != null && isFresh) {
            return@withContext cached
        }

        val res = api.getBroadcastList(categoryId = 0, provinceCode = code, provinceName = provinceName)
        val list = res.getOrDefault(emptyList())
        if (list.isNotEmpty()) {
            provinceCache[code] = list
            provinceCacheTime[code] = System.currentTimeMillis()
            try {
                onPoolRefreshedListener?.invoke()
            } catch (_: Exception) {}
        }
        list
    }

    /**
     * Applies latest fresh dedicated stream lines and program data to a target station,
     * while preserving user custom name and non-dedicated public backup lines.
     */
    fun applyDedicatedToStation(target: RadioStation, freshDedicated: RadioStation): RadioStation {
        val combinedLines = mutableListOf<StreamLine>()
        // Line 1: Fresh high-reliability dedicated stream
        freshDedicated.lines.firstOrNull()?.let { dedicatedLine ->
            combinedLines.add(dedicatedLine.copy(name = "高品质专线", priority = 1))
        } ?: run {
            combinedLines.add(StreamLine("line-dedicated-1", "高品质专线", freshDedicated.playUrl, isStable = true, priority = 1))
        }

        // Line 2: Original public stream from base station (if distinct from radio.cn dedicated stream)
        val originalPublicUrl = if (!target.urlResolved.isNullOrBlank() && !target.urlResolved.contains("radio.cn")) {
            target.urlResolved
        } else if (target.url.isNotBlank() && !target.url.contains("radio.cn")) {
            target.url
        } else {
            target.lines.find { !it.url.contains("radio.cn") }?.url
        }

        if (!originalPublicUrl.isNullOrBlank() && originalPublicUrl != freshDedicated.playUrl) {
            combinedLines.add(StreamLine("line-backup-public", "公网备用", originalPublicUrl, isStable = false, priority = 2))
        }

        // Additional lines if present
        for (extra in freshDedicated.lines.drop(1)) {
            if (combinedLines.none { it.url == extra.url }) {
                combinedLines.add(extra.copy(priority = combinedLines.size + 1))
            }
        }

        return target.copy(
            url = freshDedicated.url,
            urlResolved = freshDedicated.urlResolved ?: freshDedicated.url,
            favicon = freshDedicated.favicon?.takeIf { it.isNotBlank() } ?: target.favicon,
            lines = combinedLines,
            activeLineIndex = 0,
            currentProgram = freshDedicated.currentProgram ?: target.currentProgram,
            hasDedicatedLine = true
        )
    }

    /**
     * Instant synchronous resolver (0ms): Matches station against in-memory dedicated pool.
     * If matched and in-memory pool has fresh stream, returns station with updated stream lines.
     */
    fun resolveFreshStation(station: RadioStation): RadioStation {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return station
        val allCached = provinceCache.values.flatten()
        if (allCached.isEmpty()) return station

        var freshDedicated = allCached.find { it.id == station.id }

        if (freshDedicated == null) {
            freshDedicated = allCached.firstOrNull { candidate ->
                isSameStation(station.name, candidate.name)
            }
        }

        if (freshDedicated != null) {
            if (!isStreamUrlExpired(freshDedicated.playUrl)) {
                return applyDedicatedToStation(station, freshDedicated)
            }
        }

        return station
    }

    /**
     * Asynchronously refreshes broadcast stream URL for a given radio station on-demand.
     * Proactively resolves province/national lines if stream has expired or is invalid.
     */
    suspend fun resolveFreshStationAsync(
        station: RadioStation,
        forceRefresh: Boolean = false
    ): RadioStation = withContext(Dispatchers.IO) {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return@withContext station

        val streamExpired = isStreamUrlExpired(station.playUrl)
        val isDedicatedChannel = station.id.startsWith("dedicated_") || station.hasDedicatedLine || station.playUrl.contains("radio.cn")

        if (!forceRefresh && !streamExpired && !isDedicatedChannel) {
            return@withContext station
        }

        // 1. Try fast in-memory resolution first
        if (!forceRefresh) {
            val synced = resolveFreshStation(station)
            if (synced !== station && !isStreamUrlExpired(synced.playUrl)) {
                return@withContext synced
            }
        }

        // 2. Identify province and fetch from cloud
        val targetProvince = station.state?.takeIf { it.isNotBlank() && it != "全国" }
            ?: provinceCodeMap.keys.firstOrNull { it != "全国" && station.name.contains(it) }
            ?: cityToProvinceMap.entries.firstOrNull { station.name.contains(it.key) }?.value

        if (targetProvince != null) {
            getStationsByProvince(targetProvince, forceRefresh = forceRefresh)
        }
        getNationalStations(forceRefresh = forceRefresh)

        // 3. Match again from refreshed pool
        val allCached = provinceCache.values.flatten()
        var freshDedicated: RadioStation? = if (station.id.startsWith("dedicated_")) {
            allCached.find { it.id == station.id }
        } else null

        if (freshDedicated == null) {
            freshDedicated = allCached.firstOrNull { isSameStation(station.name, it.name) }
        }

        if (freshDedicated != null) {
            return@withContext applyDedicatedToStation(station, freshDedicated)
        }

        station
    }

    /**
     * Batch refreshes a list of radio stations (e.g. favorites or scenario playlist).
     * High-speed optimized with 0ms in-memory map matching, eliminating repetitive flattening and regex compilation.
     */
    fun refreshStationList(stations: List<RadioStation>): List<RadioStation> {
        if (stations.isEmpty() || currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return stations
        val allCached = provinceCache.values.flatten()
        if (allCached.isEmpty()) return stations

        val cachedById = allCached.associateBy { it.id }
        return stations.map { station ->
            var freshDedicated = cachedById[station.id]
            if (freshDedicated == null) {
                freshDedicated = allCached.firstOrNull { candidate ->
                    isSameStation(station.name, candidate.name)
                }
            }
            if (freshDedicated != null && !isStreamUrlExpired(freshDedicated.playUrl)) {
                applyDedicatedToStation(station, freshDedicated)
            } else {
                station
            }
        }
    }

    /**
     * Search among all loaded/cached dedicated stations or fetch on-demand.
     * Auto-detects province/city keywords in the search query and proactively fetches them.
     */
    suspend fun searchDedicatedStations(query: String, currentProvince: String? = null): List<RadioStation> = withContext(Dispatchers.IO) {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) return@withContext emptyList()
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return@withContext emptyList()

        // 1. Ensure national stations are loaded
        val national = getNationalStations()

        // 2. Identify target provinces mentioned in query or specified by user
        val targetProvinces = mutableSetOf<String>()
        currentProvince?.takeIf { it.isNotBlank() }?.let { targetProvinces.add(it) }

        for (prov in provinceCodeMap.keys) {
            if (prov != "全国" && (cleanQ.contains(prov) || prov.contains(cleanQ))) {
                targetProvinces.add(prov)
            }
        }
        for ((city, prov) in cityToProvinceMap) {
            if (cleanQ.contains(city)) {
                targetProvinces.add(prov)
            }
        }

        // Proactively fetch any detected province
        for (p in targetProvinces) {
            getStationsByProvince(p)
        }

        // 3. Match against the unified candidate pool
        val candidatePool = (national + provinceCache.values.flatten()).distinctBy { it.id }
        val normQ = normalizeName(cleanQ)
        candidatePool.filter { st ->
            val normName = normalizeName(st.name)
            val nameMatch = st.name.contains(cleanQ, ignoreCase = true) ||
                (normQ.isNotBlank() && normName.contains(normQ, ignoreCase = true)) ||
                isSameStation(cleanQ, st.name)
            val stateMatch = st.state?.equals(cleanQ, ignoreCase = true) == true ||
                st.state?.contains(cleanQ, ignoreCase = true) == true
            val tagMatch = st.tags.split(",", " ", ";", "，", "；").any { it.trim().equals(cleanQ, ignoreCase = true) }
            nameMatch || stateMatch || tagMatch
        }
    }

    /**
     * Strips noise words, prefixes, frequencies to allow fuzzy matching
     * (e.g. "CNR-1 中国之声" -> "中国之声", "河南广播电视台交通广播 FM104.1" -> "河南交通广播")
     * Cached with precompiled regular expressions for zero-allocation sub-millisecond execution.
     */
    fun normalizeName(raw: String): String {
        if (raw.isBlank()) return ""
        return normalizedNameCache.getOrPut(raw) {
            raw.uppercase()
                .replace(REGEX_CNR, "")
                .replace(REGEX_CCTV, "")
                .replace(REGEX_FM, "")
                .replace(REGEX_AM, "")
                .replace(REGEX_STATION_SUFFIX, "")
                .replace(REGEX_PUNCTUATION, "")
                .trim()
        }
    }

    /**
     * Checks if two station names refer to the same station with strict geographic & category protection.
     */
    fun isSameStation(nameA: String, nameB: String): Boolean {
        val normA = normalizeName(nameA)
        val normB = normalizeName(nameB)
        if (normA.isEmpty() || normB.isEmpty()) return false
        if (normA == normB) return true

        // 核心保护：若任意一方是广播通用频道定位词（如"音乐之声"、"交通广播"等），
        // 严禁单向包含匹配！必须完全一致才判定为同台。
        // 这彻底杜绝了地方台（如“怀集音乐之声”、“抚顺音乐之声”）被国家台（“音乐之声”）劫持的问题。
        if (normA in GENERIC_CHANNEL_TERMS || normB in GENERIC_CHANNEL_TERMS) {
            return false
        }

        // 双方均非纯分类词时，若存在包含关系，进一步比对差异部分
        val (longer, shorter) = if (normA.length >= normB.length) normA to normB else normB to normA
        if (!longer.contains(shorter)) return false

        val diff = longer.replace(shorter, "").trim()
        if (diff.isEmpty()) return true

        // 地理行政区划防护：若差异部分包含了省份、城市或常见行政区划关键字，说明是不同级别的电台，坚决不视为同台
        val hasGeoConflict = provinceCodeMap.keys.any { prov -> prov != "全国" && diff.contains(prov) } ||
            cityToProvinceMap.keys.any { city -> diff.contains(city) } ||
            diff.contains("省") || diff.contains("市") || diff.contains("县") || diff.contains("区")
        if (hasGeoConflict) {
            return false
        }

        // 若差异部分仅为允许的修饰性冗余词（如广播、频道、频率、之声、网络、在线等），可视为同台
        val cleanDiff = diff.replace(Regex("广播|频道|频率|之声|网络|在线|官方|融媒|卫视|电台|台"), "").trim()
        return cleanDiff.isEmpty()
    }

    /**
     * Merges a base station list (from presets or RadioBrowser) with dedicated high-availability stations.
     * When a match is found:
     * - The dedicated stream becomes Line 1 (isStable = true, priority = 1)
     * - The original stream becomes Line 2 (backup line, priority = 2)
     * - Retains HD artwork and real-time EPG program title
     * - Appends unmatched dedicated stations to the top/front of the list for instant visibility
     */
    fun mergeWithDedicatedStations(
        baseStations: List<RadioStation>,
        dedicatedStations: List<RadioStation>
    ): List<RadioStation> {
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) {
            return baseStations
        }
        if (currentSourceMode == com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
            if (dedicatedStations.isEmpty()) return emptyList()
            val matchedDedicatedIds = mutableSetOf<String>()
            val resultList = mutableListOf<RadioStation>()
            for (base in baseStations) {
                val matchingDedicated = dedicatedStations.firstOrNull { dedicated ->
                    isSameStation(base.name, dedicated.name)
                }
                if (matchingDedicated != null) {
                    matchedDedicatedIds.add(matchingDedicated.id)
                    resultList.add(base.copy(
                        favicon = matchingDedicated.favicon?.takeIf { it.isNotBlank() } ?: base.favicon,
                        lines = matchingDedicated.lines,
                        activeLineIndex = 0,
                        currentProgram = matchingDedicated.currentProgram ?: base.currentProgram,
                        hasDedicatedLine = true
                    ))
                }
            }
            val unmatchedDedicated = dedicatedStations.filter { it.id !in matchedDedicatedIds }
            resultList.addAll(0, unmatchedDedicated)
            return resultList.distinctBy { it.id }
        }

        if (dedicatedStations.isEmpty()) return baseStations
        if (baseStations.isEmpty()) return dedicatedStations

        val matchedDedicatedIds = mutableSetOf<String>()
        val resultList = mutableListOf<RadioStation>()

        for (base in baseStations) {
            val matchingDedicated = dedicatedStations.firstOrNull { dedicated ->
                isSameStation(base.name, dedicated.name)
            }

            if (matchingDedicated != null) {
                matchedDedicatedIds.add(matchingDedicated.id)

                // Build unified redundant lines
                val combinedLines = mutableListOf<StreamLine>()
                // Line 1: Primary Dedicated official high-quality stream (from dedicated station)
                matchingDedicated.lines.firstOrNull()?.let { dedicatedLine ->
                    combinedLines.add(dedicatedLine.copy(name = "高品质专线", priority = 1))
                } ?: run {
                    combinedLines.add(StreamLine("line-dedicated-1", "高品质专线", matchingDedicated.playUrl, isStable = true, priority = 1))
                }

                // Line 2: Original public stream from base station
                val baseLineUrl = base.playUrl
                if (baseLineUrl.isNotBlank() && baseLineUrl != matchingDedicated.playUrl) {
                    combinedLines.add(StreamLine("line-backup-public", "公网备用", baseLineUrl, isStable = false, priority = 2))
                }

                // Additional lines if present
                for (extra in matchingDedicated.lines.drop(1)) {
                    if (combinedLines.none { it.url == extra.url }) {
                        combinedLines.add(extra.copy(priority = combinedLines.size + 1))
                    }
                }

                val merged = base.copy(
                    favicon = matchingDedicated.favicon?.takeIf { it.isNotBlank() } ?: base.favicon,
                    lines = combinedLines,
                    activeLineIndex = 0,
                    currentProgram = matchingDedicated.currentProgram ?: base.currentProgram,
                    hasDedicatedLine = true
                )
                resultList.add(merged)
            } else {
                resultList.add(base)
            }
        }

        // Dedicated stations that were not in baseStations can be placed at the top (e.g. key official channels)
        val unmatchedDedicated = dedicatedStations.filter { it.id !in matchedDedicatedIds }
        if (unmatchedDedicated.isNotEmpty()) {
            // Prepend key unmatched dedicated stations
            resultList.addAll(0, unmatchedDedicated)
        }

        return resultList.distinctBy { it.id }
    }
}
