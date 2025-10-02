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
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import com.gemalto.eziomobilesampleapp.Configuration
import com.gemalto.eziomobilesampleapp.R
import com.gemalto.eziomobilesampleapp.gui.overlays.CircleTimer
import com.gemalto.eziomobilesampleapp.gui.overlays.CircleTimerAnimation
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols.OTPDelegate
import com.gemalto.idp.mobile.authentication.AuthInput
import com.gemalto.idp.mobile.core.util.SecureString
import java.util.Timer
import java.util.TimerTask

/**
 * OTP generation Fragment.
 */
class FragmentOtp : AbstractMainFragmentWithAuthSolver(), OTPDelegate {
    //region Defines
    private var mLastOTP: SecureString? = null
    private var mTimer: Timer? = null
    private var mOtp: TextView? = null
    private var mLifespan: TextView? = null
    private var mCircleTimer: CircleTimer? = null
    private var mCircleTimerAnimation: CircleTimerAnimation? = null

    private var mAmount: String? = null
    private var mBeneficiary: String? = null

    private val mTextChangeAnim = AlphaAnimation(1f, .0f)

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val retValue = inflater.inflate(R.layout.fragment_otp, null)

        val caption = retValue.findViewById<TextView>(R.id.tv_fragment_caption)
        caption.setText(if (mAmount != null && mBeneficiary != null) R.string.ui_button_otp_sign else R.string.ui_button_otp_auth)

        val domainTextView = retValue.findViewById<TextView?>(R.id.tv_fragment_description)
        if (domainTextView != null) {
            domainTextView.setText(String.format("Domain: %s", Configuration.CFG_OOB_DOMAIN))
        }

        mOtp = retValue.findViewById<TextView>(R.id.text_view_otp)
        mLifespan = retValue.findViewById<TextView>(R.id.text_view_countdown)
        mCircleTimer = retValue.findViewById<CircleTimer>(R.id.circleTimer)

        // Animation for OTP text change
        mTextChangeAnim.setDuration(200)
        mTextChangeAnim.setRepeatCount(1)
        mTextChangeAnim.setRepeatMode(Animation.REVERSE)
        mTextChangeAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                // nothing to do
            }

            override fun onAnimationEnd(animation: Animation?) {
                // nothing to do
            }

            override fun onAnimationRepeat(animation: Animation?) {
                // Format longer OTP's
                val otpValue = StringBuilder(mLastOTP.toString())
                if (otpValue.length > 4) {
                    otpValue.insert(otpValue.length / 2, " ")
                }
                mOtp?.setText(otpValue.toString())
            }
        })

        return retValue
    }

    override fun onStop() {
        super.onStop()

        if (mTimer != null) {
            mTimer?.cancel()
        }
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
        }

        mainActivity?.enableDrawer(false)
        mainActivity?.reloadGui()
    }

    /**
     * {@inheritDoc}
     */
    public override fun disableGUI() {
    }

    //endregion
    //region OTPDelegate
    override fun onOTPDelegateFinished(
        otp: SecureString?,
        error: String?,
        authInput: AuthInput?,
        serverChallenge: SecureString?
    ) {
        if (otp != null && error == null) {
            setLastOTP(otp)
            val lifespan = Main.sharedInstance()?.managerToken?.tokenDevice?.device
                ?.getLastOtpLifespan()
            if (lifespan != null) {
                if (lifespan > 0) {
                    scheduleAnimation(lifespan)
                }
            }

            Handler(Looper.getMainLooper()).post(Runnable {
                // Animate OTP text change
                mOtp?.startAnimation(mTextChangeAnim)

                // Schedule timer to check lifetime and recalculate OTP.
                if (mTimer == null) {
                    mTimer = Timer()
                    mTimer?.schedule(object : TimerTask() {
                        override fun run() {
                            checkOTPLifespan(authInput, serverChallenge)
                        }
                    }, 0, 500)
                }
            })
        } else {
            mainActivity?.showErrorIfExists(error)
        }
    }

    //endregion
    //region Private Helpers
    protected fun checkOTPLifespan(
        authInput: AuthInput?,
        serverChallenge: SecureString?
    ) {
        // Read last otp lifespan from device.
        val lifeSpan = Main.sharedInstance()?.managerToken?.tokenDevice?.device
            ?.getLastOtpLifespan()

        // OTP is still valid.
        if (lifeSpan != null) {
            if (lifeSpan <= 0) {
                Main.sharedInstance()?.managerToken?.tokenDevice
                    ?.totpWithAuthInput(authInput, serverChallenge, this)
            }
        }
    }

    private fun scheduleAnimation(lifeSpan: Int) {
        Handler(Looper.getMainLooper()).post(Runnable {
            if (mCircleTimerAnimation != null) {
                mCircleTimerAnimation?.cancel()
            }
            mCircleTimerAnimation = CircleTimerAnimation(
                mCircleTimer,
                mLifespan,
                Configuration.CFG_OTP_LIFESPAN,
                lifeSpan
            )
            mCircleTimer?.startAnimation(mCircleTimerAnimation)
        })
    }

    protected fun setLastOTP(otp: SecureString?) {
        if (mLastOTP != null) {
            mLastOTP?.wipe()
        }

        mLastOTP = if (otp != null) otp.clone() else null
    } //endregion

    companion object {
        //endregion
        //region Life Cycle
        fun authentication(authInput: AuthInput?): FragmentOtp {
            val retValue = FragmentOtp()

            // Calculate OTP.
            Main.sharedInstance()?.managerToken?.tokenDevice
                ?.totpWithAuthInput(authInput, null, retValue)

            return retValue
        }

        fun transactionSign(
            authInput: AuthInput?,
            serverChallenge: SecureString?,
            amount: String?,
            beneficiary: String?
        ): FragmentOtp {
            val retValue = FragmentOtp()
            retValue.mAmount = amount
            retValue.mBeneficiary = beneficiary

            // Calculate OTP.
            Main.sharedInstance()?.managerToken?.tokenDevice
                ?.totpWithAuthInput(authInput, serverChallenge, retValue)

            return retValue
        }
    }
}
