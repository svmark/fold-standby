package com.marksnd.foldstandby.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max

data class PhotoAlbum(val id: Long, val name: String, val count: Int)

/** 사진 읽기 권한 이름. Android 13부터 사진 전용 권한을 쓴다. */
val photoPermissions: Array<String>
    get() = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

/** 전체 허용이든 “선택한 사진만” 허용이든 읽을 수 있으면 true. */
fun hasPhotoAccess(context: Context) =
    photoPermissions.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

/** 폰의 사진 앨범(갤러리 폴더) 목록. 사진이 많은 순. */
suspend fun listAlbums(context: Context): List<PhotoAlbum> = withContext(Dispatchers.IO) {
    if (!hasPhotoAccess(context)) return@withContext emptyList()
    val projection = arrayOf(MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
    val albums = linkedMapOf<Long, PhotoAlbum>()
    context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { c ->
        while (c.moveToNext()) {
            val id = c.getLong(0)
            val name = c.getString(1) ?: "(이름 없음)"
            albums[id] = albums[id]?.let { it.copy(count = it.count + 1) } ?: PhotoAlbum(id, name, 1)
        }
    }
    albums.values.sortedByDescending { it.count }
}

/** 앨범의 사진 주소. 최근 찍은 순으로 최대 [limit]장. */
suspend fun albumPhotos(context: Context, albumId: Long, limit: Int = 500): List<Uri> = withContext(Dispatchers.IO) {
    if (!hasPhotoAccess(context)) return@withContext emptyList()
    val base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    context.contentResolver.query(
        base, arrayOf(MediaStore.Images.Media._ID),
        "${MediaStore.Images.Media.BUCKET_ID} = ?", arrayOf(albumId.toString()),
        "${MediaStore.Images.Media.DATE_TAKEN} DESC",
    )?.use { c ->
        buildList {
            while (c.moveToNext() && size < limit) add(ContentUris.withAppendedId(base, c.getLong(0)))
        }
    } ?: emptyList()
}

/** 고른 앨범의 사진 목록. 앨범에 새로 들어온 사진을 반영하도록 30분마다 다시 읽는다. */
@Composable
fun rememberAlbumPhotos(albumId: Long?): State<List<Uri>> {
    val context = LocalContext.current
    return produceState(emptyList(), albumId) {
        if (albumId == null) return@produceState
        while (true) {
            value = runCatching { albumPhotos(context, albumId) }.getOrDefault(emptyList())
            delay(30 * 60_000L)
        }
    }
}

/** 화면에 맞는 크기로 줄여서 읽는다. 사진 방향(EXIF)은 ImageDecoder가 맞춰 준다. */
suspend fun loadPhoto(context: Context, uri: Uri, maxSide: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longest = max(info.size.width, info.size.height)
            if (longest > maxSide) {
                val scale = maxSide.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }.asImageBitmap()
    }.getOrNull()
}
