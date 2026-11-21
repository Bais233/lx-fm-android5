package com.dragon.fm.player

/**
 * 端侧语音识别智能标点符号与自然断句引擎 (歌词级律动排版)
 *
 * 核心优化：
 * 1. 严格清洗首尾杂质标点与连续重复标点符号（，，、。。、，。等）；
 * 2. 具备 120+ 广播常用语义连词、主播问候语、电台台呼呼号、从句谓语停顿词判定；
 * 3. 具备中文语气助词自然呼吸停顿判定（"呢", "吧", "啊", "呀", "啦", "哦", "嘛"）；
 * 4. 严苛防撕裂保护：绝不在助词（的/地/得/了/着/过）、方位词（中/上/下/里/内/外）、量词（年/月/日/点/分/元/次/个）与数字前后乱插逗号；
 * 5. 歌词级韵律断句：句号/问号/感叹号强行断行，短从句智能合并（8~16字/行），自动剔除行尾悬挂逗号。
 */
object AsrPunctuationFormatter {

    // 1. 主播问候语与称呼（后置加逗号）
    private val SALUTATIONS = listOf(
        "各位听众朋友", "各位观众朋友", "亲爱的听众朋友", "亲爱的朋友们",
        "听众朋友们", "观众朋友们", "朋友们",
        "大家晚上好", "大家上午好", "大家下午好", "大家早上好", "大家中午好",
        "大家周末好", "各位晚上好", "各位好", "大家好"
    )

    // 2. 常见电台台呼与栏目开场引语（后置加逗号）
    private val BROADCAST_LEADS = listOf(
        "中央人民广播电台", "中国之声", "经济之声", "音乐之声", "大湾区之声",
        "经典音乐广播", "广播电视台", "人民广播电台", "国际广播电台",
        "欢迎收听", "欢迎锁定", "正在为您直播", "正在直播",
        "这里是", "我是主持人", "我是主播", "感谢收听", "感谢关注",
        "欢迎回来", "现在是北京时间", "北京时间"
    )

    // 3. 常见转折词、连词、承接短语（前置加逗号）
    private val CONJUNCTIONS = listOf(
        "但是", "然而", "不过", "可是", "因此", "所以", "同时",
        "此外", "而且", "虽然", "尽管", "即使", "如果", "只要",
        "也就是说", "据了解", "据报道", "专家表示", "记者获悉",
        "总台记者", "随后", "目前", "此前", "另外", "以及", "并且",
        "一方面", "另一方面", "与此同时", "不仅如此", "总的来看",
        "事实上", "特别是", "尤其是", "换句话说", "比如", "例如",
        "其实", "当然", "那么", "毋庸置疑", "总而言之", "近年来",
        "众所周知", "总体而言", "显而易见", "毫无疑问"
    )

    // 4. 从句与谓语停顿词（后置加逗号）
    private val CLAUSE_END_MARKERS = listOf(
        "之后", "之前", "期间", "以来", "来看", "来讲", "而言",
        "方面", "之中", "之下", "之上", "阶段", "过程", "显示",
        "表明", "指出", "强调", "宣布", "表示", "认为", "透露",
        "证实", "说明", "提醒", "建议", "介绍", "分享", "播报"
    )

    // 5. 中文语气助词与叹词（自然发音换气停顿，后置加逗号）
    private val MODAL_PARTICLES = listOf(
        "呢", "吧", "啊", "呀", "啦", "哦", "嘛"
    )

    // 6. 介词与代词（长句安全切分时优先在此字符前断开）
    private val BREAK_CANDIDATE_PREFIXES = listOf(
        "在", "从", "对", "为", "和", "跟", "由", "向", "把", "被", "让", "给",
        "我", "你", "他", "她", "它", "我们", "你们", "他们", "这", "那",
        "因为", "如果", "并且", "以及", "通过", "经过", "关于", "对于"
    )

    // 严禁在其前方插入逗号的字符集合（杜绝 "重大的，历史意义"、"完成，了"、"2024，年" 等撕裂现象）
    private val FORBIDDEN_BREAK_CHARS = setOf(
        '的', '地', '得', '了', '着', '过',
        '中', '上', '下', '里', '内', '外', '前', '后', '时', '初', '末',
        '年', '月', '日', '号', '点', '分', '秒', '元', '块', '角', '分',
        '万', '亿', '%', '‰', '个', '位', '次', '件', '条', '只', '支', '本', '张',
        '，', '。', '？', '！', '、', '：', '；', ' ', '…'
    )

    // 7. 疑问助词与疑问代词（句末判定问号）
    private val QUESTION_WORDS = listOf(
        "吗", "呢", "怎么", "为什么", "是否", "能否", "谁", "哪里",
        "哪儿", "是不是", "何以", "多少", "几时", "怎样", "可否", "有没有"
    )

    // 8. 感叹词与强烈语气词（句末判定感叹号）
    private val EXCLAMATION_WORDS = listOf(
        "太棒了", "恭喜", "加油", "注意", "小心", "祝贺", "精彩", "太好了", "非常棒", "再见"
    )

    // 常用保持大写的电台、科技与广播英文缩略词
    private val UPPERCASE_ACRONYMS = setOf(
        "FM", "AM", "CCTV", "CNR", "CRI", "BBC", "CNN", "VOI", "CD", "DVD", "USB",
        "GPS", "WIFI", "APP", "AI", "KTV", "DJ", "MC", "VIP", "OK", "TV", "NBA",
        "CBA", "5G", "4G", "3G", "LIVE", "EPG", "DIY", "ATM", "SOS", "CEO", "CTO",
        "MP3", "MP4", "ID", "IP", "PM", "EQ", "DSP", "UFO", "PR", "HR", "IT", "LED"
    )

    /**
     * 英文大小写自然化排版：
     * 解决 Sherpa-ONNX 默认全大写输出（如 "HELLO WORLD FM RADIO"）的不合理问题。
     * - 常用缩略词与电台台标（FM, AM, BBC, CCTV, DJ, LIVE等）保持大写；
     * - 独立代词 "I" 及 "I'm", "I'll", "I've", "I'd" 保持大写；
     * - 句首首字母大写；
     * - 其余常规英文字母自然转为小写。
     */
    fun normalizeEnglishCasing(text: String): String {
        if (!text.any { it in 'A'..'Z' }) return text

        val englishWordRegex = Regex("""(?:\b|(?<=[^a-zA-Z]))([a-zA-Z]+(?:'[a-zA-Z]+)?)(?:\b|(?=[^a-zA-Z]))""")
        var isSentenceStart = true

        return englishWordRegex.replace(text) { matchResult ->
            val word = matchResult.value
            val upper = word.uppercase()

            val formatted = when {
                upper in UPPERCASE_ACRONYMS -> upper
                upper == "I" || upper.startsWith("I'") -> {
                    "I" + word.substring(1).lowercase()
                }
                isSentenceStart -> {
                    word.substring(0, 1).uppercase() + word.substring(1).lowercase()
                }
                else -> {
                    word.lowercase()
                }
            }

            // 判断该单词后方是否有句末结束标点，若有则下一单词为新句句首
            val endIdx = matchResult.range.last + 1
            if (endIdx < text.length) {
                val nextSnippet = text.substring(endIdx, minOf(endIdx + 4, text.length))
                if (nextSnippet.any { it in "。？！.?!;" }) {
                    isSentenceStart = true
                } else if (nextSnippet.any { it in "，,、: \t" }) {
                    isSentenceStart = false
                }
            } else {
                isSentenceStart = false
            }

            formatted
        }
    }

    /**
     * 对实时流式输出的文本进行标点符号智能注入与自然断句排版
     */
    fun formatStreamingText(raw: String): String {
        val cleaned = sanitizeRawInput(raw)
        if (cleaned.isEmpty()) return ""

        val cased = normalizeEnglishCasing(cleaned)
        val normalized = normalizeBilingualSpacing(cased)
        return insertSmartPunctuation(normalized, isFinal = false)
    }

    /**
     * 当主播一句话发音完毕（Endpoint 触发或达到分句点）时，补全句末标点并拆分为歌词行
     */
    fun formatAndSegmentSentence(raw: String): List<String> {
        val cleaned = sanitizeRawInput(raw)
        if (cleaned.isEmpty()) return emptyList()

        val cased = normalizeEnglishCasing(cleaned)
        val normalized = normalizeBilingualSpacing(cased)
        val punctuated = insertSmartPunctuation(normalized, isFinal = true)
        val finalized = appendTerminalPunctuation(punctuated)

        return splitIntoLyricLines(finalized)
    }

    /**
     * 清洗原始识别流中的首部杂质标点、多重空格与重复符号
     */
    private fun sanitizeRawInput(raw: String): String {
        var s = raw.trim().trimStart('，', '。', '？', '！', '、', ',', '.', '?', '!', ' ', '…')
        if (s.isEmpty()) return ""

        // 合并重复标点符号
        s = s.replace(Regex("[，,]{2,}"), "，")
        s = s.replace(Regex("[。.]+"), "。")
        s = s.replace(Regex("[？！?!]{2,}"), "！")
        s = s.replace(Regex("[，,][。.]"), "。")
        s = s.replace(Regex("[。.][，,]"), "。")
        s = s.replace(Regex("[、]{2,}"), "、")
        s = s.replace(Regex("[，,][、]"), "，")
        s = s.replace(Regex("[、][，,]"), "，")

        // 移除中文字符与标点之间的多余空白
        s = s.replace(Regex("\\s+([，。？！、])"), "$1")
        s = s.replace(Regex("([，。？！、])\\s+"), "$1")

        return s
    }

    /**
     * 中英文与数字之间的优雅空格排版
     */
    private fun normalizeBilingualSpacing(text: String): String {
        var s = text.replace(Regex("([\\u4e00-\\u9fa5])([a-zA-Z0-9])"), "$1 $2")
        s = s.replace(Regex("([a-zA-Z0-9])([\\u4e00-\\u9fa5])"), "$1 $2")
        return s.replace(Regex("\\s+"), " ")
    }

    /**
     * 智能标点符号注入核心算法
     */
    private fun insertSmartPunctuation(text: String, isFinal: Boolean): String {
        val sb = StringBuilder()
        var i = 0
        var charsSincePunct = 0

        while (i < text.length) {
            val c = text[i]

            // 若原文本身已有标点符号，保留并归零计数，杜绝连续重复插入
            if (c in listOf('，', '。', '？', '！', '、', '…')) {
                if (!sb.endsWithPunctuation()) {
                    sb.append(c)
                }
                charsSincePunct = 0
                i++
                continue
            }

            // 规则 1：检查主播称谓/问候语（后置加逗号）
            var matchedSalutation: String? = null
            for (sal in SALUTATIONS) {
                if (text.startsWith(sal, i)) {
                    matchedSalutation = sal
                    break
                }
            }
            if (matchedSalutation != null) {
                sb.append(matchedSalutation)
                i += matchedSalutation.length
                charsSincePunct = 0
                if (i < text.length && !sb.endsWithPunctuation() && text[i] !in FORBIDDEN_BREAK_CHARS) {
                    sb.append("，")
                }
                continue
            }

            // 规则 2：检查电台台呼/引语（后置加逗号）
            var matchedLead: String? = null
            for (lead in BROADCAST_LEADS) {
                if (text.startsWith(lead, i)) {
                    matchedLead = lead
                    break
                }
            }
            if (matchedLead != null && (charsSincePunct >= 3 || i == 0)) {
                sb.append(matchedLead)
                i += matchedLead.length
                charsSincePunct = 0
                if (i < text.length && !sb.endsWithPunctuation() && text[i] !in FORBIDDEN_BREAK_CHARS) {
                    sb.append("，")
                }
                continue
            }

            // 规则 3：检查连词与转折承接（前置加逗号）
            var matchedConj: String? = null
            for (conj in CONJUNCTIONS) {
                if (text.startsWith(conj, i)) {
                    matchedConj = conj
                    break
                }
            }
            if (matchedConj != null && charsSincePunct >= 4) {
                if (!sb.endsWithPunctuation()) {
                    sb.append("，")
                    charsSincePunct = 0
                }
            }

            sb.append(text[i])
            charsSincePunct++

            // 规则 4：语气助词自然换气停顿（后置加逗号，例如 "呢", "吧", "啊"）
            if (charsSincePunct >= 4 && i + 1 < text.length) {
                val curCharStr = text[i].toString()
                val nextChar = text[i + 1]
                if (curCharStr in MODAL_PARTICLES && !sb.endsWithPunctuation() && nextChar !in FORBIDDEN_BREAK_CHARS) {
                    sb.append("，")
                    charsSincePunct = 0
                    i++
                    continue
                }
            }

            // 规则 5：检查从句与谓语收尾词（后置加逗号）
            if (charsSincePunct >= 5 && i + 1 < text.length) {
                val nextChar = text[i + 1]
                if (nextChar !in FORBIDDEN_BREAK_CHARS) {
                    for (marker in CLAUSE_END_MARKERS) {
                        if (sb.endsWith(marker)) {
                            if (!sb.endsWithPunctuation()) {
                                sb.append("，")
                                charsSincePunct = 0
                                break
                            }
                        }
                    }
                }
            }

            // 规则 6：律动安全网（当连续无标点字数达到 9~14 字时，敏捷在合理断点注入逗号）
            if (charsSincePunct >= 9 && i + 1 < text.length) {
                val nextChar = text[i + 1]
                val curChar = text[i]
                val remaining = text.substring(i + 1)

                // 优先在语义连接词、介词或代词前断句
                var breakBeforePrefix = false
                for (pref in BREAK_CANDIDATE_PREFIXES) {
                    if (remaining.startsWith(pref)) {
                        breakBeforePrefix = true
                        break
                    }
                }

                // 严格安全防线：绝对不在助词、量词、数字、英文词中撕裂
                val isMidEnglishWord = (curChar in 'a'..'z' || curChar in 'A'..'Z') && (nextChar in 'a'..'z' || nextChar in 'A'..'Z')
                val isMidNumber = (curChar in '0'..'9') && (nextChar in '0'..'9' || nextChar in FORBIDDEN_BREAK_CHARS)
                val isForbiddenTarget = nextChar in FORBIDDEN_BREAK_CHARS

                if (!isMidEnglishWord && !isMidNumber && !isForbiddenTarget && !sb.endsWithPunctuation()) {
                    if ((breakBeforePrefix && charsSincePunct >= 8) || charsSincePunct >= 13) {
                        sb.append("，")
                        charsSincePunct = 0
                    }
                }
            }

            i++
        }

        return sb.toString()
    }

    private fun StringBuilder.endsWithPunctuation(): Boolean {
        if (isEmpty()) return true
        val lastChar = last()
        return lastChar in listOf('，', '。', '？', '！', '、', '…', ' ')
    }

    /**
     * 句末标点判定（。/？/！）
     */
    fun appendTerminalPunctuation(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        val last = trimmed.last()
        if (last in listOf('。', '？', '！', '…')) return trimmed

        val isQuestion = QUESTION_WORDS.any { trimmed.contains(it) } &&
                (trimmed.endsWith("吗") || trimmed.endsWith("呢") || trimmed.endsWith("吧") ||
                        trimmed.contains("为什么") || trimmed.contains("怎么") || trimmed.contains("是否") ||
                        trimmed.contains("有没有"))

        val isExclamation = EXCLAMATION_WORDS.any { trimmed.contains(it) }

        val clean = if (last == '，' || last == '、') trimmed.dropLast(1) else trimmed

        return when {
            isQuestion -> "$clean？"
            isExclamation -> "$clean！"
            else -> "$clean。"
        }
    }

    /**
     * 将带有标点的长句切分为适合歌词界面的独立韵律行（8 ~ 16 字符）
     * 句号/问号/感叹号强制断行，短从句智能合并，剔除行尾悬挂逗号
     */
    fun splitIntoLyricLines(punctuatedText: String): List<String> {
        val trimmed = punctuatedText.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()

        // 1. 优先按强终止标点（句号、问号、感叹号、换行符）切分为独立语义句子
        val sentenceRegex = Regex("([^。？！\\n]+[。？！]?)")
        val sentenceMatches = sentenceRegex.findAll(trimmed)

        for (sm in sentenceMatches) {
            val sentence = sm.value.trim()
            if (sentence.isEmpty()) continue

            // 若单句长度适中（<= 16 字符），直接作为一行，剔除行尾悬挂逗号
            val cleanSentence = sentence.trimEnd('，', '、', ',', ' ')
            if (cleanSentence.length <= 16) {
                lines.add(cleanSentence)
                continue
            }

            // 若单句超过 16 字符，按逗号切分子从句并智能合并
            val clauseRegex = Regex("([^，、]+[，、]?)")
            val clauseMatches = clauseRegex.findAll(sentence)

            var curLine = StringBuilder()
            for (cm in clauseMatches) {
                val clause = cm.value.trim()
                if (clause.isEmpty()) continue

                // 移除单从句末尾的逗号便于测量与组合
                val clauseClean = clause.trimEnd('，', '、', ',', ' ')
                if (clauseClean.isEmpty()) continue

                if (curLine.isEmpty()) {
                    curLine.append(clauseClean)
                } else if (curLine.length + clauseClean.length + 1 <= 16) {
                    // 若合并后仍 <= 16 字符，以逗号优雅连接（例如 "中央广播电台，中国之声"）
                    curLine.append("，").append(clauseClean)
                } else {
                    lines.add(curLine.toString().trimEnd('，', '、', ',', ' '))
                    curLine = StringBuilder(clauseClean)
                }
            }

            if (curLine.isNotEmpty()) {
                val finalLine = curLine.toString().trimEnd('，', '、', ',', ' ')
                // 若末尾残留非常短（< 4 字符）且上一行可容纳，回并上一行
                if (lines.isNotEmpty() && finalLine.length < 4 && lines.last().length + finalLine.length + 1 <= 18) {
                    lines[lines.lastIndex] = lines.last() + "，" + finalLine
                } else {
                    lines.add(finalLine)
                }
            }
        }

        return if (lines.isEmpty()) listOf(trimmed.trimEnd('，', '、', ',', ' ')) else lines
    }
}
