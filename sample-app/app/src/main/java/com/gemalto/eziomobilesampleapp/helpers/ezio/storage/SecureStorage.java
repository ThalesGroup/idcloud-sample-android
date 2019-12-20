/*
 *
 * MIT License
 *
 * Copyright (c) 2019 Thales DIS
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
 */

package com.gemalto.eziomobilesampleapp.helpers.ezio.storage;

import android.support.annotation.NonNull;

import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.core.devicefingerprint.DeviceFingerprintException;
import com.gemalto.idp.mobile.core.passwordmanager.PasswordManagerException;
import com.gemalto.idp.mobile.core.util.SecureByteArray;
import com.gemalto.idp.mobile.securestorage.IdpSecureStorageException;
import com.gemalto.idp.mobile.securestorage.PropertyStorage;
import com.gemalto.idp.mobile.securestorage.SecureStorageManager;
import com.gemalto.idp.mobile.securestorage.SecureStorageModule;

import java.nio.ByteBuffer;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * SecureStorage example usage.
 */
public class SecureStorage implements Protocols.StorageProtocol {

    //region Defines

    private static final String C_SAMPLE_STORAGE = "SampleStorage";

    private final SecureStorageManager mManager;

    //endregion

    //region Life Cycle

    public SecureStorage() {
        mManager = SecureStorageModule.create().getSecureStorageManager();
    }

    //endregion

    //region StorageProtocol

    @Override
    public boolean writeString(final String value, final String key) {
        return writeValue(key, CMain.secureStringFromString(value));
    }

    @Override
    public boolean writeInteger(final int value, final String key) {
        final byte[] bytes = ByteBuffer.allocate(4).putInt(value).array();
        return writeValue(key, CMain.secureByteArrayFromBytes(bytes, false));
    }

    @Override
    public String readString(final String key) {
        final SecureByteArray value = readValue(key);
        return value != null ? new String(value.toByteArray()) : null;
    }

    @Override
    public int readInteger(final String key) {
        final SecureByteArray value = readValue(key);
        return value != null ? ByteBuffer.wrap(value.toByteArray()).getInt() : 0;
    }

    @Override
    public boolean removeValue(final String key) {
        return writeValue(key, null);
    }

    //endregion

    //region Private Helpers

    private SecureByteArray readValue(@NonNull final String key) {
        SecureByteArray retValue = null;
        PropertyStorage storage = null;
        try {
            // Try to get common storage.
            storage = mManager.getPropertyStorage(C_SAMPLE_STORAGE);
            // Try to open given storage.
            storage.open();
            // Try to read property for given key.
            retValue = storage.readProperty(key.getBytes());
        } catch (PasswordManagerException | DeviceFingerprintException | IdpSecureStorageException e) {
            // Show generic error to end user and try again.
            // IdpSecureStorageException is thrown in case key is null.
        } finally {
            // In all cases try to close storage.
            if (storage != null) {
                try {
                    storage.close();
                } catch (IdpSecureStorageException e) {
                    // Can't do anything on this place, we have to ignore it.
                }
            }
        }

        return retValue;
    }

    private boolean writeValue(@NonNull final String key, final SecureByteArray value) {
        boolean retValue = false;
        PropertyStorage storage = null;
        try {
            // Try to get common storage.
            storage = mManager.getPropertyStorage(C_SAMPLE_STORAGE);
            // Try to open given storage.
            storage.open();
            // Try to write property for given key.
            if (value != null) {
                storage.writeProperty(key.getBytes(), value, false);
            } else {
                storage.deleteProperty(key.getBytes());
            }

            retValue = true;
        } catch (PasswordManagerException | DeviceFingerprintException | IdpSecureStorageException e) {
            // Show generic error to end user and try again.
            // IdpSecureStorageException is thrown in case key is null.
        } finally {
            // In all cases try to close storage.
            if (storage != null) {
                try {
                    storage.close();
                } catch (IdpSecureStorageException e) {
                    // Can't do anything on this place, we have to ignore it.
                }
            }
        }

        return retValue;
    }


    //endregion
}
