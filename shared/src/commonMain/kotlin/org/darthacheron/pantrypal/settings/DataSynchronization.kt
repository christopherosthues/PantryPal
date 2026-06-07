package org.darthacheron.pantrypal.settings

import org.jetbrains.compose.resources.StringResource
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.settings_no_synchronization
import pantrypal.shared.generated.resources.settings_only_download
import pantrypal.shared.generated.resources.settings_only_upload
import pantrypal.shared.generated.resources.settings_upload_and_download

enum class DataSynchronization {
    NO_SYNCHRONIZATION,
    ONLY_UPLOAD,
    ONLY_DOWNLOAD,
    UPLOAD_AND_DOWNLOAD;

    fun toStringResource(): StringResource {
        return when (this) {
            NO_SYNCHRONIZATION -> Res.string.settings_no_synchronization
            ONLY_UPLOAD -> Res.string.settings_only_upload
            ONLY_DOWNLOAD -> Res.string.settings_only_download
            UPLOAD_AND_DOWNLOAD -> Res.string.settings_upload_and_download
        }
    }
}
