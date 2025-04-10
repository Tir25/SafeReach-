package com.example.safereach.data.mapper

import com.example.safereach.data.model.Alert as DataAlert
import com.example.safereach.domain.model.Alert as DomainAlert
import org.junit.Test
import org.junit.Assert.*
import java.util.Date

class AlertMapperTest {

    @Test
    fun `test domain to data model conversion`() {
        // Create domain model
        val domainAlert = DomainAlert(
            id = "test-id-123",
            userId = "user-456",
            alertType = "FIRE",
            location = DomainAlert.Location(
                latitude = 37.7749,
                longitude = -122.4194
            ),
            timestamp = Date(1625097600000), // 2021-07-01
            resolved = false
        )
        
        // Convert to data model
        val dataAlert = domainAlert.toDataModel()
        
        // Assert all fields are correctly mapped
        assertEquals("test-id-123", dataAlert.alertId)
        assertEquals("user-456", dataAlert.userId)
        assertEquals("FIRE", dataAlert.type)
        assertEquals(37.7749, dataAlert.latitude, 0.0001)
        assertEquals(-122.4194, dataAlert.longitude, 0.0001)
        assertEquals(1625097600000, dataAlert.timestamp)
        assertEquals(false, dataAlert.resolved)
        assertEquals(true, dataAlert.offlineSynced) // Default value
    }
    
    @Test
    fun `test data to domain model conversion`() {
        // Create data model
        val dataAlert = DataAlert(
            alertId = "test-id-123",
            userId = "user-456",
            type = "AMBULANCE",
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = 1625097600000, // 2021-07-01
            resolved = true,
            offlineSynced = false
        )
        
        // Convert to domain model
        val domainAlert = dataAlert.toDomainModel()
        
        // Assert all fields are correctly mapped
        assertEquals("test-id-123", domainAlert.id)
        assertEquals("user-456", domainAlert.userId)
        assertEquals("AMBULANCE", domainAlert.alertType)
        assertEquals(40.7128, domainAlert.location.latitude, 0.0001)
        assertEquals(-74.0060, domainAlert.location.longitude, 0.0001)
        assertEquals(1625097600000, domainAlert.timestamp.time)
        assertEquals(true, domainAlert.resolved)
    }
    
    @Test
    fun `test round trip conversion preserves values`() {
        // Create original domain model
        val originalDomainAlert = DomainAlert(
            id = "test-id-789",
            userId = "user-abc",
            alertType = "POLICE",
            location = DomainAlert.Location(
                latitude = 51.5074,
                longitude = -0.1278
            ),
            timestamp = Date(1625097600000), // 2021-07-01
            resolved = true
        )
        
        // Convert domain -> data -> domain
        val roundTripDomainAlert = originalDomainAlert.toDataModel().toDomainModel()
        
        // Assert all fields match the original
        assertEquals(originalDomainAlert.id, roundTripDomainAlert.id)
        assertEquals(originalDomainAlert.userId, roundTripDomainAlert.userId)
        assertEquals(originalDomainAlert.alertType, roundTripDomainAlert.alertType)
        assertEquals(originalDomainAlert.location.latitude, roundTripDomainAlert.location.latitude, 0.0001)
        assertEquals(originalDomainAlert.location.longitude, roundTripDomainAlert.location.longitude, 0.0001)
        assertEquals(originalDomainAlert.timestamp.time, roundTripDomainAlert.timestamp.time)
        assertEquals(originalDomainAlert.resolved, roundTripDomainAlert.resolved)
    }
} 