package com.marksnd.foldstandby

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.marksnd.foldstandby.data.NightMode
import com.marksnd.foldstandby.data.PhotoAlbum
import com.marksnd.foldstandby.data.hasPhotoAccess
import com.marksnd.foldstandby.data.listAlbums
import com.marksnd.foldstandby.data.photoPermissions
import com.marksnd.foldstandby.data.SettingsStore
import com.marksnd.foldstandby.data.hasCalendarPermission
import com.marksnd.foldstandby.data.notificationAccessGranted
import com.marksnd.foldstandby.data.rememberSettings
import com.marksnd.foldstandby.data.rememberSettingsStore
import com.marksnd.foldstandby.service.NotifListener
import com.marksnd.foldstandby.ui.AccentNames

/** 설정·권한 안내 화면. 화면 보호기 설정의 톱니바퀴에서도 열린다. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SettingsStore(this).update { putBoolean(SettingsStore.SETUP_SEEN, true) }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFF5C542),
                    onPrimary = Color.Black,
                    secondaryContainer = Color(0xFF3A3020),
                    background = Color(0xFF111111),
                    surface = Color(0xFF111111),
                ),
            ) { SetupScreen() }
        }
    }
}

@Composable
private fun SetupScreen() {
    val context = LocalContext.current
    val store = rememberSettingsStore()
    val settings by rememberSettings(store)

    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val calendarOk = remember(refresh) { hasCalendarPermission(context) }
    val locationOk = remember(refresh) {
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val notifOk = remember(refresh) { notificationAccessGranted(context) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }
    val photoOk = remember(refresh) { hasPhotoAccess(context) }
    var showAlbums by remember { mutableStateOf(false) }
    val photoPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        refresh++
        if (result.values.any { it }) showAlbums = true
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Fold StandBy", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "폴드8을 가로로 세워 충전하면 시계·달력·위젯·음악을 크게 보여줘요. " +
                        "좌우로 밀면 보기가, 시계에서 위아래로 밀면 스타일이 바뀌어요. 이 설정 화면은 스탠바이 화면을 길게 누르면 다시 열 수 있어요. 폰의 화면 회전 잠금과 상관없이 폰 방향대로 돌아가요.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = {
                        context.startActivity(Intent(context, StandbyActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        (context as? ComponentActivity)?.finish()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("스탠바이로 돌아가기") }

                SectionTitle("충전할 때 자동으로 켜기")
                StepCard(
                    title = "화면 보호기로 지정",
                    body = "설정 › 디스플레이 › 화면 보호기에서 Fold StandBy를 고르고, 시작 조건을 '충전 중'으로 바꿔 주세요. " +
                        "메뉴가 안 보이면 설정 검색창에서 '화면 보호기'를 찾으세요.",
                    action = "화면 보호기 설정 열기",
                ) { openDreamSettings(context) }

                SectionTitle("권한")
                PermissionCard("캘린더", "오늘 일정 위젯과 아래 일정 띠", calendarOk) {
                    permissions.launch(arrayOf(Manifest.permission.READ_CALENDAR))
                }
                PermissionCard("대략적 위치", "현재 위치 날씨. 허용하지 않으면 서울 날씨를 보여줘요", locationOk) {
                    permissions.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                PermissionCard("알림 접근", "재생 중 음악과 새 알림 표시", notifOk) { openNotificationAccess(context) }
                if (!notifOk) {
                    Text(
                        "목록에서 켜지지 않으면 설정 › 애플리케이션 › Fold StandBy › 오른쪽 위 ⋮ › '제한된 설정 허용'을 먼저 누르세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SectionTitle("표시")
                ChoiceRow("언어", listOf("English", "한국어"), if (settings.korean) 1 else 0) {
                    store.update { putBoolean(SettingsStore.KOREAN, it == 1) }
                }
                ChoiceRow("주 시작", listOf("월요일", "일요일"), if (settings.mondayFirst) 0 else 1) {
                    store.update { putBoolean(SettingsStore.MONDAY_FIRST, it == 0) }
                }
                ChoiceRow("강조색", AccentNames, settings.accent) {
                    store.update { putInt(SettingsStore.ACCENT, it) }
                }
                ChoiceRow("야간 모드", listOf("자동 (어두우면)", "항상", "끄기"), settings.nightMode.ordinal) {
                    store.update { putInt(SettingsStore.NIGHT, NightMode.entries[it].ordinal) }
                }

                SectionTitle("펼친 화면 오른쪽 스택")
                Text(
                    "펼친 화면을 가로로 두면 오른쪽 반을 위아래로 밀어 사진 · 시계 · 날씨 · 재생 중 카드를 넘길 수 있어요. 음악을 틀면 재생 중 카드로 넘어가요.",
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow("사진 카드", listOf("넣기", "빼기"), if (settings.unfoldedPhotos) 0 else 1) {
                    store.update { putBoolean(SettingsStore.UNFOLDED_PHOTOS, it == 0) }
                }
                StepCard(
                    title = if (settings.photoAlbumId == null) "앨범을 골라 주세요" else "앨범: ${settings.photoAlbumName}",
                    body = "고른 앨범 사진을 섞어서 30초마다 바꿔 보여줘요. 앨범을 고르지 않았거나 사진이 없으면 사진 카드는 빠져요.",
                    action = if (photoOk) "앨범 고르기" else "사진 접근 허용하고 앨범 고르기",
                ) {
                    if (photoOk) showAlbums = true else photoPermission.launch(photoPermissions)
                }
                if (showAlbums) {
                    AlbumDialog(
                        onPick = { album ->
                            store.update {
                                putLong(SettingsStore.PHOTO_ALBUM_ID, album.id)
                                putString(SettingsStore.PHOTO_ALBUM_NAME, album.name)
                            }
                            showAlbums = false
                        },
                        onDismiss = { showAlbums = false },
                    )
                }

                SectionTitle("침대 옆 시계")
                ChoiceRow("알람 15분 전부터 은은한 빛", listOf("켜기", "끄기"), if (settings.alarmGlow) 0 else 1) {
                    store.update { putBoolean(SettingsStore.ALARM_GLOW, it == 0) }
                }
                ChoiceRow("밤에 가만히 두면 어둡게, 움직이면 깨우기", listOf("켜기", "끄기"), if (settings.wakeOnMotion) 0 else 1) {
                    store.update { putBoolean(SettingsStore.WAKE_ON_MOTION, it == 0) }
                }
                Text(
                    "빛은 폰 시계 앱의 다음 알람을 따라가요. 알람이 울리는 동안 빛을 유지하려면 알림 접근이 필요해요. " +
                        "화면이 켜져 있어야 하므로 충전 중에 쓰세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AlbumDialog(onPick: (PhotoAlbum) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val albums by produceState<List<PhotoAlbum>?>(null) { value = listAlbums(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        title = { Text("앨범 고르기") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                when {
                    albums == null -> Text("앨범을 읽는 중…")
                    albums!!.isEmpty() -> Text("볼 수 있는 사진이 없어요. ‘선택한 사진만’ 허용했다면 사진을 더 골라 주세요.")
                    else -> albums!!.forEach { album ->
                        TextButton(onClick = { onPick(album) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${album.name}  ·  ${album.count}장", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun StepCard(title: String, body: String, action: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun PermissionCard(title: String, why: String, granted: Boolean, onAllow: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(why, style = MaterialTheme.typography.bodySmall)
            }
            if (granted) {
                Text("허용됨", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            } else {
                OutlinedButton(onClick = onAllow) { Text("허용") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, name ->
                FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(name) })
            }
        }
    }
}

private fun openDreamSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
    }
}

private fun openNotificationAccess(context: Context) {
    val component = ComponentName(context, NotifListener::class.java).flattenToString()
    try {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component),
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}
