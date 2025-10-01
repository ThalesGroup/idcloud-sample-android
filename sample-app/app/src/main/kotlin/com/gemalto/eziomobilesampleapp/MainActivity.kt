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
package com.gemalto.eziomobilesampleapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.gemalto.eziomobilesampleapp.gui.AbstractMainFragment
import com.gemalto.eziomobilesampleapp.gui.AbstractMainFragmentWithAuthSolver
import com.gemalto.eziomobilesampleapp.gui.FragmentAuthentication
import com.gemalto.eziomobilesampleapp.gui.FragmentMissingPermissions
import com.gemalto.eziomobilesampleapp.gui.FragmentOtp
import com.gemalto.eziomobilesampleapp.gui.FragmentProvision
import com.gemalto.eziomobilesampleapp.gui.FragmentSign
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentIncomingMessage
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentLoadingIndicator
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols
import com.gemalto.eziomobilesampleapp.helpers.ezio.PushManager
import com.gemalto.idp.mobile.authentication.AuthInput
import com.gemalto.idp.mobile.authentication.AuthenticationModule
import com.gemalto.idp.mobile.authentication.mode.biometric.BiometricAuthService
import com.gemalto.idp.mobile.core.IdpCore
import com.gemalto.idp.mobile.core.util.SecureString
import com.google.android.material.navigation.NavigationView
import java.net.MalformedURLException
import java.util.HashMap

/**
 * App main activity and enter point.
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    //region Defines
    // UI Elements
    private var mLoadingBar: FragmentLoadingIndicator? = null
    private var mIncomingMessage: FragmentIncomingMessage? = null
    private var mLastFragment: AbstractMainFragment? = null
    private var mTouchIdSwitch: SwitchCompat? = null
    private var mDrawer: DrawerLayout? = null
    private var mToolbar: Toolbar? = null

    // Helpers
    protected var mMyApp: EzioSampleApp? = null
    private var mExitConfirmed = false
    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is FragmentAuthentication) {
                val managerPush = Main.sharedInstance()?.managerPush
                if (managerPush?.isIncomingMessageInQueue == true) {
                    // Incoming push notification on main screen while is still visible and no loading bar is in front
                    // can be processed automatically.
                    if (!isOverlayViewVisible) {
                        managerPush?.fetchMessage()
                    }
                } else {
                    // No stored ID mean, that it was processed and removed.
                    if (mLastFragment != null) {
                        mLastFragment?.reloadGUI()
                    }
                }
            } else {
                showMessage(R.string.PUSH_APPROVE_QUESTION)
                if (mLastFragment != null) {
                    mLastFragment?.reloadGUI()
                }
            }
        }
    }

    //endregion
    //region Life Cycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Security Guideline: AND01. Sensitive data leaks
        //
        // Prevents screenshots of the app
        disableScreenShot()
        mMyApp = applicationContext as EzioSampleApp

        // Initialise basic stuff that does not require all permissions.
        Main.sharedInstance()?.init(this)

        // Load basic ui components like tab bar etc...
        initGui()


        // Check for permissions or display fragment with information.
        if (!checkMandatoryPermissions(true)) {
            supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, FragmentMissingPermissions(), null)
                .commit()
        }

        // Make application fullscreen
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        window.setFlags(uiOptions, uiOptions)
    }

    override fun onResume() {
        super.onResume()
        mMyApp?.currentActivity = this

        // Register for incoming message change.
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter(PushManager.NOTIFICATION_ID_INCOMING_MESSAGE)
        )
        if (Main.sharedInstance()?.isInited == false) {
            checkPermissionsAndInit()
        } else {
            try {
                Main.sharedInstance()?.managerPush?.initWithPermissions()
            } catch (e: MalformedURLException) {
                // this should not happen
                throw IllegalStateException(e)
            }
            if (Main.sharedInstance()?.managerToken?.tokenDevice != null) {
                showAuthenticationFragment()
            } else {
                showProvisioningFragment()
            }
            processIncomingNotifications()
        }
    }

    override fun onPause() {
        // Unregister from incoming message change handler.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // For demo purposes we don't need any queue. Simple process last intent.
        setIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        clearReferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearReferences()
    }

    //endregion
    //region Private Helpers
    private fun showFragment(fragment: AbstractMainFragment) {
        mLastFragment = fragment
        mLastFragment?.let {
            supportFragmentManager.beginTransaction().setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
                .replace(R.id.fragment_container, it, null)
        }?.addToBackStack(null)?.commit()
    }

    /**
     * Checks the required runtime permissions and initializes the main SDK.
     */
    private fun checkPermissionsAndInit() {
        // In case we don't have permissions yet, simple wait for another call.
        // FragmentMissingPermissions will take care of that.
        if (!checkMandatoryPermissions(false)) {
            return
        }

        // SDK init take some time.. display loading bar.
        loadingIndicatorShow(Main.getString(R.string.LOADING_MESSAGE_INIT))

        // Init rest of the stuff with dependency on permissions.
        Main.sharedInstance()?.initWithPermissions(object : Protocols.GenericHandler {
            override fun onFinished(success: Boolean, error: String?) {

                // Hide loading indicator.
                loadingIndicatorHide()
                if (success) {
                    if (Main.sharedInstance()?.managerToken?.tokenDevice != null) {
                        showAuthenticationFragment()
                    } else {
                        showProvisioningFragment()
                    }
                    // Process possible incoming notification.
                    processIncomingNotifications()
                } else {
                    // Error in such case mean, that we have broken configuration or some internal state of SDK.
                    // Most probably wrong license, different fingerprint etc. We should not continue at that point.
                    throw IllegalStateException()
                }
            }
        })

    }

    /**
     * Shows or hides the loading indicator.
     *
     * @param show `True` to show the loading indicator, else `false`.
     */
    private fun loadingIndicatorShow(show: Boolean) {
        // Avoid switch to same state.
        if (mLoadingBar != null && show || mLoadingBar == null && !show) {
            return
        }
        if (show) {
            mLoadingBar = FragmentLoadingIndicator()
            mLoadingBar?.let {
                supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container_loading_bar, it, null)
            }?.commit()
        } else {
            mLoadingBar?.let {
                supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .remove(it)
            }?.commit()
            mLoadingBar = null
        }
        if (mLastFragment != null) {
            mLastFragment?.reloadGUI()
        }
    }

    /**
     * Sets the caption of the loading indicator.
     *
     * @param caption Caption of the loading indicator.
     */
    private fun loadingIndicatorSetCaption(caption: String) {
        if (mLoadingBar != null) {
            mLoadingBar?.setCaption(caption)
        }
    }

    /**
     * Disables screenshots of the application.
     */
    private fun disableScreenShot() {
//        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
//                             android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }

    /**
     * Initializes the UI.
     */
    private fun initGui() {
        setContentView(R.layout.activity_main_with_drawer)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        if (supportActionBar != null) {
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
        mDrawer = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this,
            mDrawer,
            mToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        mDrawer?.addDrawerListener(toggle)
        toggle.syncState()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        mTouchIdSwitch = navigationView.menu.findItem(R.id.nav_touch_id).actionView as SwitchCompat?
        navigationView.setNavigationItemSelectedListener(this)
        val versionApp = findViewById<TextView>(R.id.textViewAppVersion)
        versionApp.text = String.format("App Version: %s", BuildConfig.VERSION_NAME)
        val versionSdk = findViewById<TextView>(R.id.textViewSdkVersion)
        versionSdk.text = String.format("SDK Version: %s", IdpCore.getVersion())
        val privacyPolicy = findViewById<TextView>(R.id.textViewPrivacy)
        if (Configuration.CFG_PRIVACY_POLICY_URL != null) {
            privacyPolicy.setOnClickListener { onTextPressedPrivacyPolicy() }
        } else {
            privacyPolicy.visibility = View.INVISIBLE
        }
    }

    /**
     * Clears all references.
     */
    private fun clearReferences() {
        val currActivity = mMyApp?.currentActivity
        if (this == currActivity) {
            mMyApp?.currentActivity = null
        }
        Main.sharedInstance()?.unregisterUiHandler()
        mLoadingBar = null
    }

    /**
     * Processes incoming push notifications.
     */
    private fun processIncomingNotifications() {
        // Process possible incoming notification.
        val extras = intent.extras
        if (extras != null) {
            val data: MutableMap<String?, String?>? = HashMap()
            for (key in extras.keySet()) {
                data?.set(key, extras.getString(key))
            }
            Main.sharedInstance()?.managerPush?.processIncomingPush(data)
            // Mark intent as processed.
            intent.removeExtra(PushManager.PUSH_MESSAGE_TYPE)
        }
    }

    //endregion
    //region User Interface
    private fun onTextPressedPrivacyPolicy() {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Configuration.CFG_PRIVACY_POLICY_URL
        )
        startActivity(browserIntent)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            super.onBackPressed()
            val indexOfTopFragment = supportFragmentManager.fragments.size - 1
            if (indexOfTopFragment >= 0) {
                mLastFragment = supportFragmentManager.fragments[indexOfTopFragment] as AbstractMainFragment
                mLastFragment?.reloadGUI()
            }
            return
        }
        if (mExitConfirmed) {
            finish()
        } else {
            showMessage(getString(R.string.toast_exit))
            mExitConfirmed = true
            Handler().postDelayed({ mExitConfirmed = false }, 3000)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val identification = item.itemId
        if (identification == R.id.nav_change_pin) {
            mLastFragment?.changePin()
        } else if (identification == R.id.nav_delete_token) {
            mLastFragment?.deleteToken()
        } else if (identification == R.id.nav_touch_id) {
            mLastFragment?.toggleTouchId()
            reloadGui()
        }
        return true
    }

    //endregion
    //region Public API
    /**
     * Checks the required runtime permissions.
     *
     * @param askForThem `True` if dialog application should request missing permissions, else `false`.
     * @return `True` if all permissions are present, else `false`.
     */
    fun checkMandatoryPermissions(askForThem: Boolean): Boolean {
        return Main.checkPermissions(
            this,
            askForThem,
            Manifest.permission.CAMERA
        )
    }

    /**
     * Checks if loading indicator or incoming message  is present.
     *
     * @return `True` if present, else `false`.
     */
    val isOverlayViewVisible: Boolean
        get() = mLoadingBar != null || mIncomingMessage != null

    /**
     * Shows the loading indicator with a given caption.
     *
     * @param caption Caption.
     */
    fun loadingIndicatorShow(caption: String) {
        loadingIndicatorShow(true)
        loadingIndicatorSetCaption(caption)
    }

    /**
     * Hides the loading indicator.
     */
    fun loadingIndicatorHide() {
        loadingIndicatorShow(false)
    }

    /**
     * Approves the OTP.
     *
     * @param message         Message.
     * @param serverChallenge Server challenge.
     * @param handler         Handler.
     */
    fun approveOTP(
        message: String,
        serverChallenge: SecureString?,
        handler: Protocols.OTPDelegate
    ) {
        // Current fragment must be auth solver child.
        if (mLastFragment !is AbstractMainFragmentWithAuthSolver) {
            return
        }

        // Result is handed by main activity approveOTP_Result Approve/Reject
        val authSolver = mLastFragment as AbstractMainFragmentWithAuthSolver?
        mIncomingMessage = FragmentIncomingMessage()
        mIncomingMessage?.setApproveClickListener(View.OnClickListener {
            mIncomingMessage = null
            if (mLastFragment != null) {
                mLastFragment?.reloadGUI()
            }
            authSolver?.totpWithMostComfortableOne(serverChallenge, handler)
        })
        mIncomingMessage?.setRejectClickListener(View.OnClickListener {
            mIncomingMessage = null
            if (mLastFragment != null) {
                mLastFragment?.reloadGUI()
            }
            handler.onOTPDelegateFinished(null, null, null, null)
        })
        val agrs = Bundle()
        agrs.putString(FragmentIncomingMessage.FRAGMENT_ARGUMENT_CAPTION, message)
        mIncomingMessage?.setArguments(agrs)
        mIncomingMessage?.let {
            supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container_loading_bar, it, null)
        }?.commit()
    }

    /**
     * Enables or disables the action bar.
     *
     * @param enable `True` to enable, else `false`.
     */
    fun enableDrawer(enable: Boolean) {
        val actionBar = supportActionBar ?: return
        if (enable) {
            mDrawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            mToolbar?.visibility = View.VISIBLE
        } else {
            mDrawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            mToolbar?.visibility = View.GONE
        }
    }

    /**
     * Removes the top most fragment from the back-stack.
     */
    fun hideLastStackFragment() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        }
    }

    /**
     * Clears the fragment stack.
     */
    fun clearFragmentStack() {
        val fragmentManager = supportFragmentManager
        for (i in 0 until fragmentManager.backStackEntryCount) {
            fragmentManager.popBackStack()
        }
    }

    /**
     * Hides the keyboard.
     */
    fun hideKeyboard() {
        val view = findViewById<View>(android.R.id.content)
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * Shows an error dialog.
     *
     * @param error Error the show.
     */
    fun showErrorIfExists(error: String?) {
        if (error != null) {
            showMessage(error)
        }
    }

    /**
     * Shows a message dialog.
     *
     * @param message Message to display.
     */
    fun showMessage(@StringRes message: Int) {
        showErrorIfExists(Main.getString(message))
    }

    /**
     * Shows a message dialog.
     *
     * @param message Message to display.
     */
    fun showMessage(message: String?) {
        val ctx: Context = this
        if (message != null && message.trim { it <= ' ' }.isNotEmpty()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    ctx,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Reloads the UI.
     */
    fun reloadGui() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val biometricsMenuItem = navigationView.menu.findItem(R.id.nav_touch_id)

        val tokenDevice = Main.sharedInstance()?.managerToken?.tokenDevice
        biometricsMenuItem.isVisible = true

        // Set title based on whether biometrics is enabled or disabled
        biometricsMenuItem.title = if (tokenDevice?.tokenStatus?.isTouchEnabled ?: true) {
                getString(R.string.disable_biometrics)
        } else {
                getString(R.string.enable_biometrics)
        }
    }


    /**
     * Shows the provisioning fragment.
     */
    fun showProvisioningFragment() {
        // In case of transition to new tab. We want to clean out any stacked objects.
        clearFragmentStack()
        showFragment(FragmentProvision())
    }

    /**
     * Shows the authentication fragment.
     */
    fun showAuthenticationFragment() {
        // In case of transition to new tab. We want to clean out any stacked objects.
        clearFragmentStack()
        showFragment(FragmentAuthentication())
    }

    /**
     * On pressed OTP button.
     *
     * @param authInput   Auth input used to calculate OTP. Used for recalculation.
     * @param challenge   Challenge to be signed.
     * @param amount      Amount.
     * @param beneficiary Beneficiary.
     */
    fun showOtpFragment(
        authInput: AuthInput?,
        challenge: SecureString?,
        amount: String?,
        beneficiary: String?
    ) {
        if (challenge != null) {
            showFragment(FragmentOtp.transactionSign(authInput, challenge, amount, beneficiary))
        } else {
            showFragment(FragmentOtp.authentication(authInput))
        }
    }

    /**
     * On pressed Sign button.
     */
    fun showSignFragment() {
        showFragment(FragmentSign())
    } //endregion
}
