/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mybikeconfigurator.bsp

import java.util.HashMap

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
object SampleGattAttributes {
    var attributes: HashMap<String, String> = HashMap()
    val PAS_MODE_CHARACTERISTIC_WRITE = "0000155f-1212-efde-1523-785feabcd123"
    val PAS_MODE_CHARACTERISTIC_WRITE_UC = "0000155F-1212-EFDE-1523-785FEABCD123"
    val PAS_MODE_CHARACTERISTIC_READ  = "0000155e-1212-efde-1523-785feabcd123"
    val GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb"
    val DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb"
    val DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb"
    val MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb"
    val HARDWARE_REVISION_STRING = "00002a27-0000-1000-8000-00805f9b34fb"
    val FIRMWARE_REVISION_STRING = "00002a26-0000-1000-8000-00805f9b34fb"
    val SOFTWARE_REVISION_STRING = "00002a28-0000-1000-8000-00805f9b34fb"

    init {
        // Super73 Services.
        attributes.put(PAS_MODE_CHARACTERISTIC_WRITE, "Pedal Assist Service WRITE")
        attributes.put(PAS_MODE_CHARACTERISTIC_READ,  "Pedal Assist Service READ")
        attributes.put(GENERIC_ACCESS, "Generic Access")
        attributes.put(DEVICE_INFORMATION, "Device Information")
        attributes.put(DEVICE_NAME, "Device Name")
        attributes.put(MANUFACTURER_NAME_STRING, "Manufacture Name")
        attributes.put(HARDWARE_REVISION_STRING, "Hardware Revision String")
        attributes.put(FIRMWARE_REVISION_STRING, "Firmware Revision")
        attributes.put(SOFTWARE_REVISION_STRING, "SoftwareRevision")
    }


    fun lookup(uuid: String, defaultName: String): String {
        val name = attributes.get(uuid)
        return name ?: defaultName
    }
}
