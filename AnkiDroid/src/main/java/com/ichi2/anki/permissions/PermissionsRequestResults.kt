/*
 *  Copyright (c) 2023 David Allison <davidallisongithub@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.anki.permissions

import android.app.Activity
import androidx.core.app.ActivityCompat
import com.ichi2.anki.permissions.PermissionsRequestResults.PermissionRequestResult.*
import com.ichi2.anki.permissions.PermissionsRequestResults.PermissionRequestResult.Companion.toPermissionRequestResult

/**
 * Data to build a [PermissionsRequestResults], when there is no [Activity] to create it
 * Maps from a permission to whether it was granted
 */
data class PermissionsRequestRawResults(val permissions: Map<String, Boolean>, val requestedPermissions: Boolean)

/**
 * The result of requesting permissions
 * After we perform a request, starting at API M, we can know if a permission is temporarily or permanently denied.
 */
class PermissionsRequestResults(permissions: Map<String, PermissionRequestResult>, requestedPermissions: Boolean) {
    // If the permissions request interaction with the user is interrupted.
    // then we get an empty results array
    // https://developer.android.com/reference/androidx/core/app/ActivityCompat.OnRequestPermissionsResultCallback
    val cancelled = requestedPermissions && !permissions.any()

    val allGranted = !cancelled && permissions.all { it.value == GRANTED }

    val hasRejectedPermissions = permissions.any { it.value.isDenied() }

    val hasPermanentlyDeniedPermissions = permissions.any { it.value == PERMANENTLY_DENIED }

    val hasTemporarilyDeniedPermissions = permissions.any { it.value == TEMPORARILY_DENIED }

    companion object {
        fun from(activity: Activity, rawResults: PermissionsRequestRawResults): PermissionsRequestResults {
            val permissions = rawResults.permissions.mapValues { toPermissionRequestResult(activity, it.key, it.value) }
            return PermissionsRequestResults(permissions, rawResults.requestedPermissions)
        }
    }

    enum class PermissionRequestResult {
        GRANTED,

        TEMPORARILY_DENIED,

        PERMANENTLY_DENIED;

        fun isDenied(): Boolean = this != GRANTED

        companion object {
            fun toPermissionRequestResult(activity: Activity, permission: String, granted: Boolean): PermissionRequestResult {
                if (granted) {
                    return GRANTED
                }

                // Android doesn't let us easily determine if a permission was denied permanently or temporarily
                // Use shouldShowRequestPermissionRationale to handle this

                // Note: shouldShowRequestPermissionRationale will return FALSE if a permission dialog has not
                // been shown. This may not happen here as we call getPermissionResult after we have dialog results
                val isPermanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                return if (isPermanentlyDenied) PERMANENTLY_DENIED else TEMPORARILY_DENIED
            }
        }
    }
}
