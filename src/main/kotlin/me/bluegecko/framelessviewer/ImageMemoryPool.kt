package me.bluegecko.framelessviewer

import org.slf4j.LoggerFactory
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import javax.imageio.ImageIO

object ImageMemoryPool {
    private val logger = LoggerFactory.getLogger(ImageMemoryPool::class.java)
    private val imageCache = Collections.synchronizedMap(HashMap<String, WeakReference<BufferedImage>>())
    private val scaledImageCache = Collections.synchronizedMap(HashMap<String, WeakReference<Image>>())
    private const val MEMORY_THRESHOLD = 0.8

    fun getImage(path: String): BufferedImage? {
        return imageCache[path]?.get() ?: loadImage(path)
    }

    fun getScaledImage(path: String, width: Int, height: Int, originalImage: BufferedImage): Image? {
        val cacheKey = "$path-$width-$height"
        return scaledImageCache[cacheKey]?.get() ?: createScaledImage(cacheKey, width, height, originalImage)
    }

    private fun loadImage(path: String): BufferedImage? {
        checkMemoryUsage()
        return try {
            ImageIO.read(File(path))?.also { image ->
                imageCache[path] = WeakReference(image)
            }
        } catch (e: Exception) {
            logger.error("Failed to load image: $path", e)
            null
        }
    }

    private fun createScaledImage(cacheKey: String, width: Int, height: Int, originalImage: BufferedImage): Image? {
        checkMemoryUsage()
        return try {
            originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH)?.also { scaled ->
                scaledImageCache[cacheKey] = WeakReference(scaled)
            }
        } catch (e: Exception) {
            logger.error("Failed to create scaled image", e)
            null
        }
    }

    private fun checkMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()

        if (usedMemory > maxMemory * MEMORY_THRESHOLD) {
            clearCache()
            System.gc()
            logger.info("Memory threshold exceeded. Cache cleared.")
        }
    }

    fun clearCache() {
        imageCache.clear()
        scaledImageCache.clear()
    }

    fun getMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        return "Used Memory: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB"
    }

    fun getCacheSize(): String {
        return "Image Cache Size: ${imageCache.size} / Scaled Image Cache Size: ${scaledImageCache.size}"
    }
}