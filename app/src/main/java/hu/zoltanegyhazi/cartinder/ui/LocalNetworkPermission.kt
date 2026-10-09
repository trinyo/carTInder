package hu.zoltanegyhazi.cartinder.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import hu.zoltanegyhazi.cartinder.data.isLocalNetworkUrl

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

/** Android 17 (API 37) óta a helyi hálózati címek eléréséhez futásidejű engedély kell. */
private const val LOCAL_NETWORK_PERMISSION_SDK = 37

fun needsLocalNetworkPermission(context: Context, serverUrl: String): Boolean =
    Build.VERSION.SDK_INT >= LOCAL_NETWORK_PERMISSION_SDK &&
        isLocalNetworkUrl(serverUrl) &&
        ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED

/**
 * Visszaad egy függvényt, ami szükség esetén elkéri az engedélyt, majd (az eredménytől függetlenül)
 * lefuttatja a műveletet; elutasításkor a kérés úgyis hálózati hibával tér vissza.
 */
@Composable
fun rememberLocalNetworkGate(): (serverUrl: String, action: () -> Unit) -> Unit {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<() -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending[0]?.invoke()
        pending[0] = null
    }
    return remember(launcher) {
        { serverUrl, action ->
            if (needsLocalNetworkPermission(context, serverUrl)) {
                pending[0] = action
                launcher.launch(ACCESS_LOCAL_NETWORK)
            } else {
                action()
            }
        }
    }
}
