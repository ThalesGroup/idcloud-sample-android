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
package com.gemalto.eziomobilesampleapp.gui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.gemalto.eziomobilesampleapp.Configuration
import com.gemalto.eziomobilesampleapp.R
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols.AuthInputHandler
import com.gemalto.eziomobilesampleapp.helpers.ezio.KeyValue
import com.gemalto.idp.mobile.authentication.AuthInput
import com.gemalto.idp.mobile.core.util.SecureString
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Fragment to retrieve the Amount and Beneficiary for transaction signing.
 */
class FragmentSign : AbstractMainFragmentWithAuthSolver() {
    private var mTextAmount: TextView? = null
    private var mTextBeneficiary: TextView? = null
    private var mButtonProceed: Button? = null

    //endregion
    //region Life Cycle
    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val retValue = inflater.inflate(R.layout.fragment_sign, null)

        mTextAmount = retValue.findViewById<TextView>(R.id.text_amount)
        mTextBeneficiary = retValue.findViewById<TextView>(R.id.text_beneficiary)
        mButtonProceed = retValue.findViewById<Button>(R.id.button_enroll)
        mButtonProceed?.setOnClickListener(View.OnClickListener { sender: View? ->
            this.onButtonPressedProceed(
                sender
            )
        })

        val domainTextView = retValue.findViewById<TextView?>(R.id.tv_fragment_description)
        if (domainTextView != null) {
            domainTextView.setText(String.format("Domain: %s", Configuration.CFG_OOB_DOMAIN))
        }

        if (Main.sharedInstance()?.managerToken != null
            && Main.sharedInstance()?.managerToken?.tokenDevice != null
        ) {
            val userName =
                Main.sharedInstance()?.managerToken?.tokenDevice?.token?.getName()
            val userNameTextView = retValue.findViewById<TextView?>(R.id.tv_fragment_caption)
            if (userNameTextView != null) {
                userNameTextView.setText(userName)
            }
        }

        return retValue
    }

    //endregion
    //region MainFragment methods
    /**
     * {@inheritDoc}
     */
    public override fun reloadGUI() {
        // No matter what was set. If there is a loading indicator, we should disable everything.
        if (mainActivity?.isOverlayViewVisible == true) {
            disableGUI()
        } else {
            mTextAmount?.setEnabled(true)
            mTextBeneficiary?.setEnabled(true)
            mButtonProceed?.setEnabled(true)
        }

        mainActivity?.enableDrawer(false)
        mainActivity?.reloadGui()
    }

    /**
     * {@inheritDoc}
     */
    public override fun disableGUI() {
        mTextAmount?.setEnabled(false)
        mTextBeneficiary?.setEnabled(false)
        mButtonProceed?.setEnabled(false)
    }

    //endregion
    //region Private Helpers
    private val serverChallenge: SecureString?
        /**
         * Creates the challenge to be signed.
         *
         * @return Challenge
         */
        get() {
            val values: MutableList<KeyValue?> =
                ArrayList<KeyValue?>()
            values.add(
                KeyValue(
                    "amount",
                    mTextAmount?.getText().toString()
                )
            )
            values.add(
                KeyValue(
                    "beneficiary",
                    mTextBeneficiary?.getText().toString()
                )
            )

            return Main.sharedInstance()
                ?.secureStringFromString(getOcraChallenge(values as MutableList<KeyValue>))
        }

    /**
     * Calculate OCRA Challenge from array of key value objects.
     *
     * @param values List of key values object we want to use for ocra calculation.
     * @return SecureString representation of challenge or null in case of error.
     */
    private fun getOcraChallenge(values: MutableList<KeyValue>): String? {
        var retValue: String? = null

        // Use builder to append TLV
        val buffer = ByteArrayOutputStream()

        // Go through all values, calculate and append TLV for each one of them.
        for (i in values.indices) {
            // Convert key-value to UTF8 string
            val keyValueUTF8 = values.get(i).keyValueUTF8

            // Build TLV.
            buffer.write(0xDF)
            buffer.write(0x71 + i)
            buffer.write(keyValueUTF8.size.toByte().toInt())
            buffer.write(keyValueUTF8, 0, keyValueUTF8.size)
        }

        // Try to calculate digest from final string and build retValue.
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(buffer.toByteArray())

            // Server challenge expect hex string not byte array.
            retValue = bytesToHex(hash)
        } catch (e: NoSuchAlgorithmException) {
            // Ignore. In worst case it will generate invalid ocra.
        }

        return retValue
    }

    /**
     * Creates hexa string from bytes.
     *
     * @param bytes Bytes from which to create the hexa string.
     * @return Hexa string.
     */
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val value = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = HEX_ARRAY[value ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[value and 0x0F]
        }
        return String(hexChars)
    }

    //endregion
    //region User Interface
    /**
     * On pressed proceed button.
     */
    private fun onButtonPressedProceed(sender: View?) {
        if (mTextAmount?.getText().toString().isEmpty() || mTextBeneficiary?.getText().toString()
                .isEmpty()
        ) {
            return
        }

        authInputGetMostComfortableOne(object : AuthInputHandler {
            override fun onFinished(authInput: AuthInput?, error: String?) {
                if (authInput != null) {
                    // Remove current fragment from stack before displaying OTP.
                    mainActivity?.hideLastStackFragment()

                    // Display OTP.
                    mainActivity?.showOtpFragment(
                        authInput,
                        serverChallenge,
                        mTextAmount?.text.toString(),
                        mTextBeneficiary?.text.toString()
                    )
                }
                mainActivity?.showErrorIfExists(error)
            }
        })
    } //endregion

    companion object {
        //region Defines
        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    }
}
