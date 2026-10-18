package app.toil.musicbridge.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.session.MediaSessionManager
import app.toil.musicbridge.service.MusicBridgeListenerService
import java.text.Collator

internal data class SelectableApp(val packageName: String, val label: String)

@Suppress("DEPRECATION")
internal fun loadSelectableApps(context: Context, excludedPackages: Set<String>): List<SelectableApp> {
    val packageManager = context.packageManager
    val packages = mutableSetOf<String>()
    packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .forEach { packages += it.activityInfo.packageName }
    listOf(
        "androidx.media3.session.MediaSessionService",
        "androidx.media3.session.MediaLibraryService",
        "android.media.browse.MediaBrowserService",
    ).forEach { action ->
        packageManager.queryIntentServices(Intent(action), 0).forEach { packages += it.serviceInfo.packageName }
    }
    try {
        context.getSystemService(MediaSessionManager::class.java)
            ?.getActiveSessions(ComponentName(context, MusicBridgeListenerService::class.java))
            ?.forEach { packages += it.packageName }
    } catch (_: SecurityException) {
        // Launcher apps and saved exclusions remain editable without notification listener access.
    }
    packages += excludedPackages
    packages -= context.packageName
    val collator = Collator.getInstance(context.resources.configuration.locales[0])
    return packages.map { packageName ->
        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
        SelectableApp(packageName, label)
    }.sortedWith(compareBy<SelectableApp> { it.packageName !in excludedPackages }
        .thenComparator { left, right -> collator.compare(left.label, right.label) }
        .thenBy { it.packageName })
}
