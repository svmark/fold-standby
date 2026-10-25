package com.marksnd.foldstandby.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceProfileTest {

    @Test fun fold8RegionalModelsShareTheProfile() {
        for (model in listOf("SM-F971N", "SM-F971B", "SM-F971U")) {
            val camera = DeviceProfiles.forDevice("samsung", model).innerCamera
            assertEquals(HiddenCamera(2448, 1848, 1823, 18, 1901, 96), camera)
        }
        assertEquals(DeviceProfiles.forDevice("samsung", "SM-F971N"), DeviceProfiles.forDevice("Samsung", "SM-F971N"))
    }

    @Test fun otherDevicesAreGeneric() {
        assertEquals(DeviceProfiles.GENERIC, DeviceProfiles.forDevice("samsung", "SM-F766N"))
        assertEquals(DeviceProfiles.GENERIC, DeviceProfiles.forDevice("Google", "SM-F971N"))
        assertEquals(DeviceProfiles.GENERIC, DeviceProfiles.forDevice("Google", "sdk_gphone64_arm64"))
        assertNull(DeviceProfiles.GENERIC.innerCamera)
    }
}
