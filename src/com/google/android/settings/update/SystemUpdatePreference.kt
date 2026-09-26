package com.google.android.settings.update

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.android.settings.system.ClientInitiatedActionRepository
import com.android.settings.system.SystemUpdateRepository
import com.android.settingslib.DeviceInfoUtils
import com.google.android.settings.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SystemUpdatePreference(status: Int) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    PreferenceItem(
        title = stringResource(R.string.system_update_entry_title),
        summary = { updateInfo(context, status) },
        imageVector = ImageVector.vectorResource(R.drawable.software_update_system_update),
        status = status,
        onClick = { coroutineScope.launch(Dispatchers.Default) { onSystemUpdateClick(context) } },
    )
}

private fun onSystemUpdateClick(context: Context) {
    ClientInitiatedActionRepository(context).onSystemUpdate()
    val intent = SystemUpdateRepository(context).getSystemUpdateIntent()
    if (intent != null) {
        context.startActivity(intent)
    }
}

private fun updateInfo(context: Context, status: Int): String {
    if (status == STATUS_UP_TO_DATE) {
        val securityPatch = DeviceInfoUtils.getSecurityPatch()
        return if (securityPatch != null) {
            context.getString(R.string.software_update_up_to_specific_date_summary, securityPatch)
        } else {
            ""
        }
    }
    return context.getString(R.string.software_update_pending_update_summary)
}
