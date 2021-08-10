package com.hypertrack.android.ui.common

import com.hypertrack.android.models.local.LocalGeofence


class DataPage<T>(
    val items: List<T>,
    val paginationToken: String?
)