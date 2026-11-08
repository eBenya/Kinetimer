package ru.projectworkout.kneetempo.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject
import ru.projectworkout.kneetempo.domain.model.AppRelease

class GitHubUpdateRepository(private val context: Context) {
    fun currentVersion(): String =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            ?: "unknown"

    fun fetchLatestRelease(): AppRelease {
        val connection = URL(RELEASE_API_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MILLIS
        connection.readTimeout = NETWORK_TIMEOUT_MILLIS
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "KneeTempo/${currentVersion()}")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Не удалось проверить обновления (HTTP ${connection.responseCode}).")
            }
            val response =
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val release = JSONObject(response)
            val tag = release.getString("tag_name")
            val pageUrl = release.getString("html_url")
            if (!pageUrl.startsWith(RELEASE_PAGE_PREFIX)) {
                throw IOException("Ссылка на выпуск не принадлежит официальному репозиторию.")
            }
            val assets = release.getJSONArray("assets")
            val apkAsset =
                (0 until assets.length())
                    .map(assets::getJSONObject)
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                    ?: throw IOException("В последнем выпуске не найден APK.")
            val apkUrl = apkAsset.getString("browser_download_url")
            if (!apkUrl.startsWith(RELEASE_DOWNLOAD_PREFIX)) {
                throw IOException("Ссылка на APK не принадлежит официальному репозиторию.")
            }
            val digest =
                apkAsset.optString("digest")
                    .takeIf { it.startsWith("sha256:") }
                    ?.removePrefix("sha256:")
            if (digest != null && !digest.matches(Regex("[a-fA-F0-9]{64}"))) {
                throw IOException("Сервер обновлений вернул некорректную контрольную сумму APK.")
            }

            return AppRelease(
                tag = tag,
                name = release.optString("name").ifBlank { tag },
                pageUrl = pageUrl,
                apkUrl = apkUrl,
                apkSha256 = digest,
            )
        } finally {
            connection.disconnect()
        }
    }

    fun downloadApk(release: AppRelease): File {
        if (!release.apkUrl.startsWith(RELEASE_DOWNLOAD_PREFIX)) {
            throw IOException("Ссылка на APK не принадлежит официальному репозиторию.")
        }
        val directory = File(context.cacheDir, "updates")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Не удалось подготовить место для обновления.")
        }
        val apkFile = File(directory, "update.apk")
        val temporaryFile = File(directory, "update.apk.part")
        val connection = URL(release.apkUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MILLIS
        connection.readTimeout = NETWORK_TIMEOUT_MILLIS
        connection.instanceFollowRedirects = true

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Не удалось скачать обновление (HTTP ${connection.responseCode}).")
            }
            if (connection.contentLengthLong > MAX_APK_BYTES) {
                throw IOException("Размер APK превышает допустимый предел.")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var totalBytes = 0L
            connection.inputStream.use { input ->
                temporaryFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        totalBytes += read
                        if (totalBytes > MAX_APK_BYTES) {
                            throw IOException("Размер APK превышает допустимый предел.")
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (totalBytes == 0L) throw IOException("Сервер вернул пустой APK.")
            if (release.apkSha256 != null) {
                val actualDigest = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
                if (!actualDigest.equals(release.apkSha256, ignoreCase = true)) {
                    throw IOException("Контрольная сумма APK не совпадает с опубликованной.")
                }
            }
            if (apkFile.exists() && !apkFile.delete()) {
                throw IOException("Не удалось подготовить APK к установке.")
            }
            if (!temporaryFile.renameTo(apkFile)) {
                throw IOException("Не удалось сохранить загруженный APK.")
            }
            return apkFile
        } catch (exception: IOException) {
            temporaryFile.delete()
            throw exception
        } finally {
            connection.disconnect()
        }
    }

    fun openInstaller(apkFile: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent =
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return false
        }

        val apkUri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
        val installIntent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(installIntent)
        return true
    }

    private companion object {
        const val RELEASE_API_URL = "https://api.github.com/repos/eBenya/Kinetimer/releases/latest"
        const val RELEASE_PAGE_PREFIX = "https://github.com/eBenya/Kinetimer/releases/"
        const val RELEASE_DOWNLOAD_PREFIX = "https://github.com/eBenya/Kinetimer/releases/download/"
        const val NETWORK_TIMEOUT_MILLIS = 15_000
        const val MAX_APK_BYTES = 100L * 1024 * 1024
        const val COPY_BUFFER_SIZE = 8 * 1024
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
