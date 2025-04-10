package com.example.safereach.data.mapper

import com.example.safereach.data.model.Alert as DataAlert
import com.example.safereach.domain.model.Alert as DomainAlert
import com.example.safereach.domain.model.LatLng
import java.util.Date

/**
 * Convert domain model to data model
 */
fun DomainAlert.toDataModel(): DataAlert {
    return DataAlert(
        alertId = id,
        userId = userId,
        type = alertType,
        latitude = location.latitude,
        longitude = location.longitude,
        timestamp = timestamp.time,
        resolved = false  // New alerts are never resolved by default
    )
}

/**
 * Convert data model to domain model
 */
fun DataAlert.toDomainModel(): DomainAlert {
    return DomainAlert(
        id = alertId,
        userId = userId,
        alertType = type,
        location = LatLng(
            latitude = latitude,
            longitude = longitude
        ),
        timestamp = Date(timestamp)
    )
} 