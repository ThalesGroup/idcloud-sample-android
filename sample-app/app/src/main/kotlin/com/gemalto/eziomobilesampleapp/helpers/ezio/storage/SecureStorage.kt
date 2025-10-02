/*
 * MIT License
 *
 * Copyright (c) 2020 Thales DIS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * IMPORTANT: This source code is intended to serve training information purposes only.
 *            Please make sure to review our IdCloud documentation, including security guidelines.
 */
package com.gemalto.eziomobilesampleapp.helpers.ezio.storage

import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols.StorageProtocol
import com.gemalto.idp.mobile.core.devicefingerprint.DeviceFingerprintException
import com.gemalto.idp.mobile.core.passwordmanager.PasswordManagerException
import com.gemalto.idp.mobile.core.util.SecureByteArray
import com.gemalto.idp.mobile.securestorage.IdpSecureStorageException
import com.gemalto.idp.mobile.securestorage.PropertyStorage
import com.gemalto.idp.mobile.securestorage.SecureStorageManager
import com.gemalto.idp.mobile.securestorage.SecureStorageModule
import java.nio.ByteBuffer

/**
 * SecureStorage example usage.
 */
class SecureStorage : StorageProtocol {
    private val mManager: SecureStorageManager

    //endregion
    //region Life Cycle
    init {
        mManager = SecureStorageModule.create().getSecureStorageManager()
    }

    //endregion
    //region StorageProtocol
    override fun writeString(value: String?, key: String?): Boolean {
        return writeValue(key, Main.sharedInstance()?.secureStringFromString(value))
    }

    override fun writeInteger(value: Int, key: String?): Boolean {
        val bytes = ByteBuffer.allocate(4).putInt(value).array()
        return writeValue(key, Main.sharedInstance()?.secureByteArrayFromBytes(bytes, false))
    }

    override fun readString(key: String?): String? {
        val value = readValue(key)
        return if (value != null) String(value.toByteArray()) else null
    }

    override fun readInteger(key: String?): Int {
        val value = readValue(key)
        return if (value != null) ByteBuffer.wrap(value.toByteArray()).getInt() else 0
    }

    override fun removeValue(key: String?): Boolean {
        return writeValue(key, null)
    }

    //endregion
    //region Private Helpers
    /**
     * Reads a value.
     *
     * @param key Key.
     * @return Read value, or `null` in not present.
     */
    private fun readValue(key: String?): SecureByteArray? {
        var retValue: SecureByteArray? = null
        var storage: PropertyStorage? = null
        try {
            // Try to get common storage.
            storage = mManager.getPropertyStorage(SAMPLE_STORAGE)
            // Try to open given storage.
            storage.open()
            // Try to read property for given key.
            retValue = storage.readProperty(key?.toByteArray())
        } catch (e: PasswordManagerException) {
            // Ignore
        } catch (e: DeviceFingerprintException) {
        } catch (e: IdpSecureStorageException) {
        } finally {
            // In all cases try to close storage.
            if (storage != null) {
                try {
                    storage.close()
                } catch (e: IdpSecureStorageException) {
                    // Ignore
                }
            }
        }

        return retValue
    }

    /**
     * Writes a value.
     *
     * @param key   Key.
     * @param value Value.
     * @return the status
     */
    private fun writeValue(key: String?, value: SecureByteArray?): Boolean {
        var retValue = false
        var storage: PropertyStorage? = null
        try {
            // Try to get common storage.
            storage = mManager.getPropertyStorage(SAMPLE_STORAGE)
            // Try to open given storage.
            storage.open()
            // Try to write property for given key.
            if (value != null) {
                storage.writeProperty(key?.toByteArray(), value, false)
            } else {
                storage.deleteProperty(key?.toByteArray())
            }

            retValue = true
        } catch (e: PasswordManagerException) {
            // Ignore
        } catch (e: DeviceFingerprintException) {
        } catch (e: IdpSecureStorageException) {
        } finally {
            // In all cases try to close storage.
            if (storage != null) {
                try {
                    storage.close()
                } catch (e: IdpSecureStorageException) {
                    // Ignore
                }
            }
        }

        return retValue
    } //endregion


    companion object {
        //region Defines
        private const val SAMPLE_STORAGE = "SampleStorage"
    }
}
