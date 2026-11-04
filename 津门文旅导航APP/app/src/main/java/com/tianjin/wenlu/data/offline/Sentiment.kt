package com.tianjin.wenlu.data.offline

import com.tianjin.wenlu.data.CommentAnalysis
import com.tianjin.wenlu.data.CommentSample
import com.tianjin.wenlu.data.Scenic

/**
 * 游客评价智能分析（Kotlin 移植）：情感词典 + TF 关键词。
 */
object Sentiment {
    private val positiveWords = listOf(
        "好", "美", "漂亮", "赞", "满意", "推荐", "值得", "方便", "舒适", "干净", "热情",
        "丰富", "震撼", "精彩", "特色", "难忘", "喜欢", "棒", "惊喜", "悠闲", "惬意",
        "免费", "实惠", "超值", "精致", "大气", "壮观", "浓厚", "便捷", "周到",
        "有趣", "好玩", "适合", "贴心", "开心", "优美", "出色", "专业",
    )
    private val negativeWords = listOf(
        "差", "不好", "失望", "坑", "贵", "挤", "排队", "难", "脏", "乱", "吵", "慢",
        "累", "人多", "难走", "差劲", "糟糕", "后悔", "不值", "陈旧", "混乱",
        "设施旧", "管理乱", "停车难", "人太多", "拥挤", "态度差", "性价比低",
        "一般", "无聊", "破损", "异味", "宰客", "踩雷", "不推荐",
    )

    /**
     * 否定词。中文否定是前置的（"不/没/无" + 情感词），而原来的实现只做词频叠加：
     * "不好"会同时命中负面的"不好"和正面的"好" → pos == neg → 被误判为中性；
     * "不推荐""不贵""不太满意"同理。这里改为"先长词匹配、命中即挖掉、再看前面有没有否定词"。
     */
    private val negators = listOf("不", "没", "无", "未", "别", "莫", "非", "毫无")

    /** 否定词与情感词之间可能插入的程度副词（"不太好""不够满意"） */
    private val degreeWords = listOf(
        "太", "够", "是", "算", "很", "挺", "怎么", "什么", "咋",
        "那么", "这么", "特别", "十分", "非常",
    )

    /**
     * 整体语义固定、无法由"情感词 + 否定"规则推出的短语。
     * "不错""没白来"里并没有任何情感词典中的字，只能整条匹配；"没意思""不值得"同理（按负面计）。
     */
    private val fixedPhrases = mapOf(
        "不错" to true,
        "不虚此行" to true,
        "没白来" to true,
        "没白等" to true,
        "没意思" to false,
        "不值得" to false,
    )

    private val topicWords = setOf(
        "环境", "停车", "建筑", "门票", "交通", "服务", "排队", "夜景", "历史", "文化",
        "美食", "亲子", "设施", "卫生", "讲解", "价格", "位置", "风景",
        "特色", "氛围", "展品", "小吃", "拍照", "灯光", "时间", "公交", "地铁",
        "船", "塔", "桥", "街", "馆", "园", "区",
    )
    private val stopwords = setOf(
        "的", "了", "是", "很", "也", "都", "和", "与", "就", "但是", "有点",
        "非常", "比较", "感觉", "觉得", "一个", "没有", "不是", "还是", "因为", "所以",
        "可以", "应该", "这个", "那个", "我们", "他们", "自己", "一下", "起来", "地方",
        "时候", "真的", "可能", "不过", "而且", "当时", "后来", "现在",
    )

    private fun sentimentOf(text: String): String {
        var pos = 0
        var neg = 0
        var masked = text

        // ① 固定短语优先，命中后从待扫文本中移除
        fixedPhrases.forEach { (phrase, isPositive) ->
            var idx = masked.indexOf(phrase)
            var counted = false
            while (idx >= 0) {
                if (!counted) {
                    if (isPositive) pos++ else neg++
                    counted = true
                }
                masked = masked.removeRange(idx, idx + phrase.length)
                idx = masked.indexOf(phrase)
            }
        }

        // ② 长词优先，命中即从待扫文本中挖掉 —— 修复核心：
        //    "不好"先被整条识别并移走，后面的"好"就不会再计一次正面；
        //    "不推荐"也不会被"推荐"抵消。
        val words = (positiveWords.map { it to true } + negativeWords.map { it to false })
            .sortedByDescending { it.first.length }
        words.forEach { (word, isPositive) ->
            var idx = masked.indexOf(word)
            var counted = false
            while (idx >= 0) {
                // ③ 被前置否定词修饰时极性翻转："不好"→负面、"不贵"→正面、"不太满意"→负面
                // ④ 同一个词只计一次，与原来"词表命中数"的口径保持一致：
                //    否则叠词会被放大（"适合慢慢逛"里的"慢"会被算成两次负面）
                if (!counted) {
                    val flipped = isNegatedBefore(masked, idx)
                    if (isPositive != flipped) pos++ else neg++
                    counted = true
                }
                masked = masked.removeRange(idx, idx + word.length)
                idx = masked.indexOf(word)
            }
        }

        return when {
            pos > neg -> "正面"
            neg > pos -> "负面"
            else -> "中性"
        }
    }

    /** 判断 idx 处的情感词是否被前置否定词修饰（允许中间夹程度副词，如"不太好"） */
    private fun isNegatedBefore(text: String, idx: Int): Boolean {
        var prefix = text.substring(maxOf(0, idx - 6), idx)
        degreeWords.sortedByDescending { it.length }.forEach { d ->
            if (prefix.endsWith(d)) prefix = prefix.dropLast(d.length)
        }
        return negators.any { prefix.endsWith(it) }
    }

    private fun segment(text: String): Set<String> {
        val clean = text.filter { it in '\u4e00'..'\u9fa5' || it.isLetterOrDigit() }
        val tokens = mutableSetOf<String>()
        for (i in 0 until clean.length - 1) {
            val tok = clean.substring(i, i + 2)
            if (tok !in stopwords && tok.none { it.isDigit() }) tokens.add(tok)
        }
        (positiveWords + negativeWords + topicWords + fixedPhrases.keys).forEach { w ->
            if (w in text) tokens.add(w)
        }
        return tokens
    }

    private fun keywordExtract(comments: List<String>, top: Int = 6, exclude: Set<String> = emptySet()): List<String> {
        val freq = mutableMapOf<String, Int>()
        comments.forEach { c ->
            segment(c).forEach { t ->
                if (t !in exclude) freq[t] = (freq[t] ?: 0) + 1
            }
        }
        return freq.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> {
                    if (it.key in topicWords || it.key in positiveWords || it.key in negativeWords) 1 else 0
                }.thenByDescending { it.value }
            )
            .take(top)
            .map { it.key }
    }

    fun analyze(scenic: Scenic, comments: List<LocalComment>): CommentAnalysis {
        if (comments.isEmpty()) {
            return CommentAnalysis(
                scenicId = scenic.id, scenicName = scenic.name, total = 0,
                positive = 0, neutral = 0, negative = 0, satisfaction = 0,
                positiveKeywords = emptyList(), negativeKeywords = emptyList(),
                sampleComments = emptyList(),
            )
        }
        var pos = 0
        var neg = 0
        var neu = 0
        val posList = mutableListOf<String>()
        val negList = mutableListOf<String>()
        comments.forEach { c ->
            when (sentimentOf(c.content)) {
                "正面" -> { pos++; posList.add(c.content) }
                "负面" -> { neg++; negList.add(c.content) }
                else -> neu++
            }
        }
        val total = comments.size
        val satisfaction = ((pos + 0.5 * neu) / total * 100).toInt()
        val samples = comments.take(5).map { CommentSample(it.content, it.sentiment, it.rating) }
        return CommentAnalysis(
            scenicId = scenic.id,
            scenicName = scenic.name,
            total = total,
            positive = pos,
            neutral = neu,
            negative = neg,
            satisfaction = satisfaction,
            positiveKeywords = if (posList.isNotEmpty()) keywordExtract(posList, exclude = negativeWords.toSet()) else emptyList(),
            negativeKeywords = if (negList.isNotEmpty()) keywordExtract(negList, exclude = positiveWords.toSet()) else emptyList(),
            sampleComments = samples,
        )
    }

    fun overall(): Map<String, Int> {
        var pos = 0
        var neu = 0
        var neg = 0
        OfflineData.comments.forEach { c ->
            when (sentimentOf(c.content)) {
                "正面" -> pos++
                "负面" -> neg++
                else -> neu++
            }
        }
        val total = maxOf(OfflineData.comments.size, 1)
        return mapOf(
            "total" to OfflineData.comments.size,
            "positive" to pos,
            "neutral" to neu,
            "negative" to neg,
            "satisfaction" to ((pos + 0.5 * neu) / total * 100).toInt(),
        )
    }
}
