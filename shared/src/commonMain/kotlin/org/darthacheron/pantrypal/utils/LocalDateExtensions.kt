package org.darthacheron.pantrypal.utils

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import org.jetbrains.compose.resources.stringResource
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.date_format

@Composable
fun LocalDate.format(): String {
    val dateFormat = stringResource(Res.string.date_format)
    val dateFormatter = LocalDate.Format {
        byUnicodePattern(dateFormat)
    }
    return this.format(dateFormatter)
}