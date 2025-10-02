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

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.gemalto.eziomobilesampleapp.Configuration
import com.gemalto.eziomobilesampleapp.R
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentQRCodeReader
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentQRCodeReader.QRCodeReaderDelegate
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols.QRCodeManagerHandler
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenManager.ProvisionerHandler
import com.gemalto.idp.mobile.core.util.SecureByteArray
import com.gemalto.idp.mobile.core.util.SecureString
import com.gemalto.idp.mobile.otp.oath.OathToken

/**
 * Enrolling and provisioning of new token using user id + registration code, or QR code.
 */
class FragmentProvision : AbstractMainFragment(), QRCodeReaderDelegate {
    //region Defines
    private var mButtonEnrollWithQr: Button? = null
    private var mButtonEnrollManually: Button? = null
    private var mTextUserId: EditText? = null
    private var mTextRegCode: EditText? = null

    //endregion
    //region Life cycle
    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val retValue = inflater.inflate(R.layout.fragment_provision, null)

        mButtonEnrollWithQr = retValue.findViewById<Button?>(R.id.button_enroll_with_qr)
        mButtonEnrollWithQr?.setOnClickListener(View.OnClickListener { sender: View? ->
            this.onButtonPressedEnrollQr(
                sender
            )
        })

        mButtonEnrollManually = retValue.findViewById<Button?>(R.id.button_enroll)
        mButtonEnrollManually?.setOnClickListener(View.OnClickListener { sender: View? ->
            this.onButtonPressedEnrollManually(
                sender
            )
        })

        val domainTextView = retValue.findViewById<TextView?>(R.id.tv_fragment_description)
        if (domainTextView != null) {
            domainTextView.setText(String.format("Domain: %s", Configuration.CFG_OOB_DOMAIN))
        }

        mTextUserId = retValue.findViewById<EditText?>(R.id.text_amount)
        mTextUserId?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Unused
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                onTextChangedUserId()
            }

            override fun afterTextChanged(editable: Editable?) {
                // Unused
            }
        })

        mTextRegCode = retValue.findViewById<EditText?>(R.id.text_beneficiary)
        mTextRegCode?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Unused
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                onTextChangedRegCode()
            }

            override fun afterTextChanged(editable: Editable?) {
                // Unused
            }
        })

        return retValue
    }

    //endregion
    //region MainFragment
    /**
     * {@inheritDoc}
     */
    public override fun reloadGUI() {
        if (mainActivity?.isOverlayViewVisible == true) {
            disableGUI()
        } else {
            updateUIAvailability()
        }
    }

    /**
     * {@inheritDoc}
     */
    public override fun disableGUI() {
        mButtonEnrollManually?.setEnabled(false)
        mButtonEnrollWithQr?.setEnabled(false)
        mTextUserId?.setEnabled(false)
        mTextRegCode?.setEnabled(false)
    }

    //endregion
    //region Helpers
    /**
     * Updates the UI.
     */
    private fun updateUIAvailability() {
        // Enable provision button only when both user id and registration code are provided.
        mTextUserId?.getText()?.length?.let { mTextRegCode?.getText()?.length?.let { it1 -> mButtonEnrollManually?.setEnabled((it > 0) && (it1 > 0)) } }
        mButtonEnrollWithQr?.setEnabled(true)
        mTextUserId?.setEnabled(true)
        mTextRegCode?.setEnabled(true)
    }

    /**
     * Enrolls the user.
     *
     * @param userId           User id.
     * @param registrationCode Registration code.
     */
    private fun enrollWithUserId(
        userId: String?,
        registrationCode: SecureString?
    ) {
        val main = Main.sharedInstance()

        // Disable whole UI and display loading indicator.
        mainActivity?.hideKeyboard()
        mainActivity?.loadingIndicatorShow(Main.getString(R.string.LOADING_MESSAGE_ENROLLING))

        // Do provisioning and wait for response.
        main?.managerToken?.provisionWithUserId(
            userId,
            registrationCode,
            object : ProvisionerHandler {
                override fun onProvisionerFinished(token: OathToken?, error: String?) {
                    // Hide loading indicator and reload GUI.
                    mainActivity?.loadingIndicatorHide()

                    // Token was created. Switch tabs.
                    if (token != null) {
                        if (Build.VERSION.SDK_INT > 32) {
                            main.currentListener?.let {
                                Main.checkPermissions(
                                    it,
                                    true,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        }
                        mainActivity?.showAuthenticationFragment()
                    } else {
                        mainActivity?.showErrorIfExists(error)
                    }
                }
            })
    }

    //endregion
    //region QRCodeReaderDelegate
    /**
     * {@inheritDoc}
     */
    override fun onQRCodeFinished(
        sender: FragmentQRCodeReader?,
        qrCodeData: SecureByteArray?,
        error: String?
    ) {
        // First check parser result.

        if (qrCodeData == null) {
            mainActivity?.showErrorIfExists(error)
            return
        }

        // Try to parse data from provided QR Code. Actual operation is synchronous. We can use self in block directly.
        Main.sharedInstance()?.managerQRCode?.parseQRCode(
            qrCodeData,
            object : QRCodeManagerHandler {
                override fun onParseFinished(success: Boolean, userId: String?, regCode: SecureString?, error1: String?) {
                    if (success) {
                        enrollWithUserId(userId, regCode)
                    } else {
                        mainActivity?.showErrorIfExists(error1)
                        updateUIAvailability()
                    }
                }
            })
    }

    //endregion
    //region  User Interface
    /**
     * Enrolls the user using the QR code.
     */
    private fun onButtonPressedEnrollQr(sender: View?) {
        val fragment = FragmentQRCodeReader()
        fragment.init(this@FragmentProvision, 0)

        if (getActivity() != null) requireActivity().getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment, null)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Enrolls the user with user name and registration code.
     */
    private fun onButtonPressedEnrollManually(sender: View?) {
        enrollWithUserId(
            mTextUserId?.getText().toString(), Main.sharedInstance()?.secureStringFromString(
                mTextRegCode?.getText().toString()
            )
        )
    }

    /**
     * Updates the UI after text input.
     */
    private fun onTextChangedUserId() {
        // Provision button is enabled only with both textboxes filled.
        updateUIAvailability()
    }

    /**
     * Updates the UI after text input.
     */
    private fun onTextChangedRegCode() {
        // Provision button is enabled only with both textboxes filled.
        updateUIAvailability()
    } //endregion
}
