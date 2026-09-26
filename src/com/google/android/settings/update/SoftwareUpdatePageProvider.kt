package com.google.android.settings.update

import android.content.Context
import android.os.Bundle
import android.os.SystemUpdateManager
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.system.getSystemUpdateInfo
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.widget.card.SettingsCard
import com.android.settingslib.spa.widget.illustration.Illustration
import com.android.settingslib.spa.widget.illustration.ResourceType
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.SettingsBody
import com.android.settingslib.spa.widget.ui.SettingsIcon
import com.android.settingslib.spa.widget.ui.SettingsTitle
import com.google.android.settings.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

const val STATUS_UP_TO_DATE = 0
const val STATUS_PENDING_UPDATE = 1

object SoftwareUpdatePageProvider : SettingsPageProvider {
    override val name = "SoftwareUpdate"

    @Composable
    override fun Page(arguments: Bundle?) {
        val status by rememberSystemUpdateStatus()
        SoftwareUpdatePage(status)
    }

    @Composable
    fun rememberSystemUpdateStatus(): State<Int> {
        val context = LocalContext.current
        val flow = remember {
            flow { emit(getSystemUpdateStatus(context)) }.flowOn(Dispatchers.Default)
        }
        return flow.collectAsStateWithLifecycle(initialValue = STATUS_UP_TO_DATE)
    }

    private suspend fun getSystemUpdateStatus(context: Context): Int {
        val bundle = context.getSystemUpdateInfo()
        val status = bundle?.getInt(SystemUpdateManager.KEY_STATUS)
        Log.d(name, "status $status")
        return when (status) {
            SystemUpdateManager.STATUS_WAITING_DOWNLOAD,
            SystemUpdateManager.STATUS_IN_PROGRESS,
            SystemUpdateManager.STATUS_WAITING_INSTALL,
            SystemUpdateManager.STATUS_WAITING_REBOOT -> STATUS_PENDING_UPDATE
            else -> STATUS_UP_TO_DATE
        }
    }
}

@Composable
fun getUpdateHeader(status: Int): String {
    val resId =
        if (status == STATUS_UP_TO_DATE) {
            R.string.software_update_up_to_date_header
        } else {
            R.string.software_update_can_be_updated_header
        }
    return stringResource(resId)
}

@Composable
fun SoftwareUpdatePage(status: Int) {
    RegularScaffold(title = getUpdateHeader(status)) {
        Column {
            Illustration(
                resId =
                    if (status == STATUS_UP_TO_DATE) {
                        R.drawable.software_update_illustration_updated
                    } else {
                        R.drawable.software_update_illustration_pending
                    },
                resourceType = ResourceType.IMAGE,
                modifier =
                    Modifier.testTag(
                            if (status == STATUS_UP_TO_DATE) "illustration_updated"
                            else "illustration_pending"
                        )
                        .padding(bottom = 12.dp),
            )
            SettingsCard {
                SystemUpdatePreference(status)
                AppUpdatesPreference()
            }
        }
    }
}

@Composable
fun PreferenceItem(
    enabled: Boolean = true,
    title: String,
    summary: () -> CharSequence,
    imageVector: ImageVector,
    status: Int = -2,
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(width = 46.dp, height = 54.dp).padding(top = 8.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                MainIcon(imageVector)
                when (status) {
                    STATUS_UP_TO_DATE ->
                        StatusIcon(
                            imageVector = Icons.Outlined.Done,
                            color = colorResource(R.color.security_green),
                            testTag = "system_update_updated",
                        )
                    STATUS_PENDING_UPDATE ->
                        StatusIcon(
                            imageVector = Icons.Outlined.Download,
                            color = colorResource(R.color.security_yellow),
                            testTag = "system_update_pending",
                        )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsTitle(title)
                SettingsBody(summary())
            }
        }
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            SettingsIcon(Icons.AutoMirrored.Outlined.NavigateNext)
        }
    }
}

@Composable
private fun MainIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(44.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun StatusIcon(imageVector: ImageVector, color: Color, testTag: String) {
    Box(modifier = Modifier.fillMaxSize().padding(1.dp), contentAlignment = Alignment.BottomEnd) {
        Surface(modifier = Modifier.size(18.dp).padding(1.dp), shape = CircleShape, color = color) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.padding(1.dp).testTag(testTag),
                tint = MaterialTheme.colorScheme.surface,
            )
        }
    }
}
