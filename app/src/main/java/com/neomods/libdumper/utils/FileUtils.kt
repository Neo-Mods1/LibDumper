package com.neomods.libdumper.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun getFileSize(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    return it.getLong(sizeIndex)
                }
            }
        }
        return 0L
    }

    fun getFileName(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }
        return "unknown.so"
    }

    fun getFilePath(context: Context, uri: Uri): String {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val pathIndex = it.getColumnIndex("_data")
                    if (pathIndex != -1) {
                        return it.getString(pathIndex)
                    }
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    fun copyFileToInternal(context: Context, uri: Uri, fileName: String): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputFile = File(context.filesDir, fileName)
        
        inputStream?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return outputFile
    }

    fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            sizeInBytes < 1024 * 1024 * 1024 -> "${sizeInBytes / (1024 * 1024)} MB"
            else -> "${sizeInBytes / (1024 * 1024 * 1024)} GB"
        }
    }

    fun getDefaultDumpLocation(): String {
        return File(
            Environment.getExternalStorageDirectory(),
            "Dumper"
        ).absolutePath
    }

    fun findNextAvailableDumpIndex(basePath: String): Int {
        val baseDir = File(basePath)
        if (!baseDir.exists()) {
            return 0
        }
        
        var index = 0
        while (true) {
            val dumpDir = File(baseDir, "Dump$index")
            if (!dumpDir.exists()) {
                return index
            }
            index++
        }
    }

    fun createDumpDirectory(basePath: String, index: Int): File {
        val baseDir = File(basePath)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        
        val dumpDir = File(baseDir, "Dump$index")
        dumpDir.mkdirs()
        return dumpDir
    }

    fun writeToFile(file: File, content: String) {
        file.writeText(content, Charsets.UTF_8)
    }

    fun isExternalStorageAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun isFileReadable(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
