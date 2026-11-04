package com.tianjin.wenlu.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 景点图片的本地资源加载器。
 *
 * 为什么不引 Coil / Glide：
 *  1. 本项目主打「完全离线」，图片必须随 APK 发布，走 assets 而不是网络 URL；
 *  2. 读本地 assets 用框架自带的 BitmapFactory + LruCache 就够，约 60 行；
 *  3. 少一个第三方依赖，`./gradlew --offline` 永远能编译——演示现场不会因为拉不到包翻车。
 *
 * 使用约定（给美术/素材的接口）：
 *   图片放到 `android/app/src/main/assets/images/` 下，
 *   文件名**以景点 id 开头**即可，`1.jpg` / `07.png` / `scenic_12.webp` 都认，
 *   扩展名支持 jpg / jpeg / png / webp / bmp。
 *   没有图和有图走的是同一条渲染路径，缺图自动降级为渐变占位封面，不会白屏。
 */
object LocalImageStore {
    private const val DIR = "images"
    private val EXTS = setOf("jpg", "jpeg", "png", "webp", "bmp")
    private val DIGITS = Regex("(\\d+)")

    @Volatile
    private var index: Map<Int, String>? = null

    /** 32MB 位图缓存：列表来回滚动不会反复解码 */
    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** 素材换过之后调用（开发期用，热更新图片索引） */
    fun reset() {
        index = null
        cache.evictAll()
    }

    private fun ensureIndex(ctx: Context): Map<Int, String> {
        index?.let { return it }
        val map = HashMap<Int, String>()
        runCatching {
            ctx.assets.list(DIR)?.forEach { fn ->
                val ext = fn.substringAfterLast('.', "").lowercase()
                if (ext !in EXTS) return@forEach
                val id = DIGITS.find(fn)?.value?.toIntOrNull() ?: return@forEach
                // 同一个 id 有两张图时按文件名排序取第一张，保证结果稳定
                val old = map[id]
                if (old == null || fn < old.substringAfterLast('/')) map[id] = "$DIR/$fn"
            }
        }
        index = map
        return map
    }

    /** 该景点是否有随包实拍图 */
    fun has(ctx: Context, scenicId: Int): Boolean = ensureIndex(ctx).containsKey(scenicId)

    /** 已收录实拍图的景点数量——首页/地图可以用来显示「已收录 N 张实拍图」 */
    fun count(ctx: Context): Int = ensureIndex(ctx).size

    /** 同步命中缓存（滚动复用，避免占位图闪一下） */
    fun peek(ctx: Context, scenicId: Int): Bitmap? =
        ensureIndex(ctx)[scenicId]?.let { cache.get(it) }

    suspend fun load(ctx: Context, scenicId: Int, targetW: Int = 1080): Bitmap? {
        val path = ensureIndex(ctx)[scenicId] ?: return null
        cache.get(path)?.let { return it }
        return withContext(Dispatchers.IO) {
            cache.get(path) ?: runCatching {
                val probe = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.assets.open(path).use { BitmapFactory.decodeStream(it, null, probe) }
                if (probe.outWidth <= 0) return@runCatching null
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(probe.outWidth, probe.outHeight, targetW)
                    // RGB_565：封面图不需要透明度，显存直接省一半
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                ctx.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
                    ?.also { cache.put(path, it) }
            }.getOrNull()
        }
    }

    private fun sampleSize(w: Int, h: Int, targetW: Int): Int {
        var s = 1
        while (w / (s * 2) >= targetW && h / (s * 2) >= targetW) s *= 2
        return s
    }
}

/**
 * 景点封面：有实拍图就渲染实拍图，没有就用渐变占位封面。
 * 两条路径宽度/圆角/裁切都一致，所以素材陆续到齐的过程中界面不会跳动。
 */
@Composable
fun ScenicCover(
    scenicId: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    subLabel: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val ctx = LocalContext.current
    var bmp by remember(scenicId) { mutableStateOf(LocalImageStore.peek(ctx, scenicId)) }
    LaunchedEffect(scenicId) {
        if (bmp == null) LocalImageStore.load(ctx, scenicId)?.let { bmp = it }
    }
    val b = bmp
    if (b != null) {
        Image(
            bitmap = b.asImageBitmap(),
            contentDescription = label,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        CoverPlaceholder(scenicId, label, subLabel, modifier)
    }
}

/** 缺图时的兜底封面：按 id 取一个稳定的配色，视觉上像「有设计」而不是「加载失败」 */
@Composable
private fun CoverPlaceholder(
    scenicId: Int,
    label: String?,
    subLabel: String?,
    modifier: Modifier = Modifier,
) {
    val palettes = listOf(
        Color(0xFF2C5F8A) to Color(0xFF74A9CF),
        Color(0xFF8C4A2F) to Color(0xFFC98A5E),
        Color(0xFF3D6B4A) to Color(0xFF7FB08B),
        Color(0xFF5A4A8C) to Color(0xFF9B8CC9),
        Color(0xFF8C3F5A) to Color(0xFFC97E99),
        Color(0xFF4A6B7C) to Color(0xFF8FB0BE),
    )
    val idx = ((scenicId % palettes.size) + palettes.size) % palettes.size
    val (c1, c2) = palettes[idx]
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center,
    ) {
        // 同心弧装饰，避免一眼看出是空占位
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                    )
                )
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 14.dp),
        ) {
            if (!label.isNullOrBlank()) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            if (!subLabel.isNullOrBlank()) {
                Text(
                    text = subLabel,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
