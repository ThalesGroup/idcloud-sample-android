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

package com.gemalto.eziomobilesampleapp;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.gemalto.eziomobilesampleapp.gui.AbstractMainFragment;
import com.gemalto.eziomobilesampleapp.gui.AbstractMainFragmentWithAuthSolver;
import com.gemalto.eziomobilesampleapp.gui.FragmentAuthentication;
import com.gemalto.eziomobilesampleapp.gui.FragmentMissingPermissions;
import com.gemalto.eziomobilesampleapp.gui.FragmentOtp;
import com.gemalto.eziomobilesampleapp.gui.FragmentProvision;
import com.gemalto.eziomobilesampleapp.gui.FragmentSign;
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentIncomingMessage;
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentLoadingIndicator;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.eziomobilesampleapp.helpers.ezio.PushManager;
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenDevice;
import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.authentication.AuthenticationModule;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintAuthService;
import com.gemalto.idp.mobile.core.IdpCore;
import com.gemalto.idp.mobile.core.util.SecureString;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * App main activity and enter point.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    //region Defines

    // UI Elements
    private FragmentLoadingIndicator mLoadingBar = null;
    private FragmentIncomingMessage mIncomingMessage = null;


    private AbstractMainFragment mLastFragment = null;
    private SwitchCompat mFaceIdSwitch = null;
    private SwitchCompat mTouchIdSwitch = null;
    private DrawerLayout mDrawer;
    private Toolbar mToolbar;

    // Helpers
    protected EzioSampleApp mMyApp = null;
    private boolean mExitConfirmed = false;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof FragmentAuthentication) {
                final PushManager managerPush = Main.sharedInstance().getManagerPush();
                if (managerPush.isIncomingMessageInQueue()) {
                    // Incoming push notification on main screen while is still visible and no loading bar is in front
                    // can be processed automatically.
                    if (!isOverlayViewVisible()) {
                        managerPush.fetchMessage();
                    }
                } else {
                    // No stored ID mean, that it was processed and removed.
                    if (mLastFragment != null) {
                        mLastFragment.reloadGUI();
                    }
                }

            } else {
                showMessage(R.string.PUSH_APPROVE_QUESTION);
                if (mLastFragment != null) {
                    mLastFragment.reloadGUI();
                }
            }
        }
    };

    //endregion

    //region Life Cycle

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Security Guideline: AND01. Sensitive data leaks
        //
        // Prevents screenshots of the app
        disableScreenShot();

        mMyApp = (EzioSampleApp) getApplicationContext();

        // Initialise basic stuff that does not require all permissions.
        Main.sharedInstance().init(this);

        // Load basic ui components like tab bar etc...
        initGui();


        // Check for permissions or display fragment with information.
        if (!checkMandatoryPermissions(true)) {
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                                       .replace(R.id.fragment_container, new FragmentMissingPermissions(), null)
                                       .commit();
        }

        // Make application fullscreen
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().setFlags(uiOptions, uiOptions);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mMyApp.setCurrentActivity(this);

        // Register for incoming message change.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(PushManager.NOTIFICATION_ID_INCOMING_MESSAGE));

        if (!Main.sharedInstance().isInited()) {
            checkPermissionsAndInit();
        } else {
            try {
                Main.sharedInstance().getManagerPush().initWithPermissions();
            } catch (MalformedURLException e) {
                // this should not happen
                throw new IllegalStateException(e);
            }

            if (Main.sharedInstance().getManagerToken().getTokenDevice() != null) {
                showAuthenticationFragment();
            } else {
                showProvisioningFragment();
            }

            processIncomingNotifications();
        }
    }

    @Override
    protected void onPause() {
        // Unregister from incoming message change handler.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        super.onPause();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        // For demo purposes we don't need any queue. Simple process last intent.
        setIntent(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();

        clearReferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        clearReferences();
    }

    //endregion

    //region Private Helpers

    private void showFragment(final AbstractMainFragment fragment) {
        mLastFragment = fragment;
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fade_in,
                                                                           R.anim.fade_out,
                                                                           R.anim.fade_in,
                                                                           R.anim.fade_out)
                                   .replace(R.id.fragment_container, mLastFragment, null).addToBackStack(null).commit();
    }

    /**
     * Checks the required runtime permissions and initializes the main SDK.
     */
    private void checkPermissionsAndInit() {
        // In case we don't have permissions yet, simple wait for another call.
        // FragmentMissingPermissions will take care of that.
        if (!checkMandatoryPermissions(false)) {
            return;
        }

        // SDK init take some time.. display loading bar.
        loadingIndicatorShow(Main.getString(R.string.LOADING_MESSAGE_INIT));

        // Init rest of the stuff with dependency on permissions.
        Main.sharedInstance().initWithPermissions((success, error) -> {

            // Hide loading indicator.
            loadingIndicatorHide();

            if (success) {
                if (Main.sharedInstance().getManagerToken().getTokenDevice() != null) {
                    showAuthenticationFragment();
                } else {
                    showProvisioningFragment();
                }

                // Process possible incoming notification.
                processIncomingNotifications();
            } else {
                // Error in such case mean, that we have broken configuration or some internal state of SDK.
                // Most probably wrong license, different fingerprint etc. We should not continue at that point.
                throw new IllegalStateException();
            }
        });
    }

    /**
     * Shows or hides the loading indicator.
     *
     * @param show
     *         {@code True} to show the loading indicator, else {@code false}.
     */
    private void loadingIndicatorShow(final boolean show) {
        // Avoid switch to same state.
        if ((mLoadingBar != null && show) || (mLoadingBar == null && !show)) {
            return;
        }

        if (show) {
            mLoadingBar = new FragmentLoadingIndicator();

            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                                       .replace(R.id.fragment_container_loading_bar, mLoadingBar, null).commit();
        } else {
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                                       .remove(mLoadingBar).commit();
            mLoadingBar = null;
        }

        if (mLastFragment != null) {
            mLastFragment.reloadGUI();
        }
    }

    /**
     * Sets the caption of the loading indicator.
     *
     * @param caption
     *         Caption of the loading indicator.
     */
    private void loadingIndicatorSetCaption(final String caption) {
        if (mLoadingBar != null) {
            mLoadingBar.setCaption(caption);
        }
    }

    /**
     * Disables screenshots of the application.
     */
    private void disableScreenShot() {
//        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
//                             android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }

    /**
     * Initializes the UI.
     */
    private void initGui() {
        setContentView(R.layout.activity_main_with_drawer);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mDrawer = findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                                                                       mDrawer,
                                                                       mToolbar,
                                                                       R.string.navigation_drawer_open,
                                                                       R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = findViewById(R.id.nav_view);
        mFaceIdSwitch = (SwitchCompat) navigationView.getMenu().findItem(R.id.nav_face_id).getActionView();
        mFaceIdSwitch.setOnClickListener(this::onSwitchPressedFaceId);

        mTouchIdSwitch = (SwitchCompat) navigationView.getMenu().findItem(R.id.nav_touch_id).getActionView();
        mTouchIdSwitch.setOnClickListener(this::onSwitchPressedTouchId);

        navigationView.setNavigationItemSelectedListener(this);

        final TextView versionApp = findViewById(R.id.textViewAppVersion);
        versionApp.setText(String.format("App Version: %s", BuildConfig.VERSION_NAME));

        final TextView versionSdk = findViewById(R.id.textViewSdkVersion);
        versionSdk.setText(String.format("SDK Version: %s", IdpCore.getVersion()));

        final TextView privacyPolicy = findViewById(R.id.textViewPrivacy);
        privacyPolicy.setOnClickListener(this::onTextPressedPrivacyPolicy);
    }

    /**
     * Clears all references.
     */
    private void clearReferences() {
        final Activity currActivity = mMyApp.getCurrentActivity();
        if (this.equals(currActivity)) {
            mMyApp.setCurrentActivity(null);
        }

        Main.sharedInstance().unregisterUiHandler();
        mLoadingBar = null;
    }

    /**
     * Processes incoming push notifications.
     */
    private void processIncomingNotifications() {
        // Process possible incoming notification.
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final Map<String, String> data = new HashMap<>();
            for (final String key : extras.keySet()) {
                data.put(key, extras.getString(key));
            }
            Main.sharedInstance().getManagerPush().processIncomingPush(data);
            // Mark intent as processed.
            getIntent().removeExtra( PushManager.PUSH_MESSAGE_TYPE);
        }
    }

    //endregion

    //region User Interface

    private void onSwitchPressedFaceId(final View sender) {
        mFaceIdSwitch.setChecked(!mFaceIdSwitch.isChecked());
        mLastFragment.toggleFaceId();
    }

    private void onSwitchPressedTouchId(final View sender) {
        mTouchIdSwitch.setChecked(!mTouchIdSwitch.isChecked());
        mLastFragment.toggleTouchId();
    }

    private void onTextPressedPrivacyPolicy(final View sender) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Configuration.CFG_PRIVACY_POLICY_URL);
        startActivity(browserIntent);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();

            final int indexOfTopFragment = getSupportFragmentManager().getFragments().size() - 1;
            if (indexOfTopFragment >= 0) {
                mLastFragment = (AbstractMainFragment) getSupportFragmentManager().getFragments()
                                                                                  .get(indexOfTopFragment);
                mLastFragment.reloadGUI();
            }

            return;
        }

        if (mExitConfirmed) {
            finish();
        } else {
            showMessage(getString(R.string.toast_exit));
            mExitConfirmed = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mExitConfirmed = false;
                }
            }, 3000);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        final int identification = item.getItemId();

        if (identification == R.id.nav_change_pin) {
            mLastFragment.changePin();
        } else if (identification == R.id.nav_delete_token) {
            mLastFragment.deleteToken();
        }

        return true;
    }

    //endregion

    //region Public API

    /**
     * Checks the required runtime permissions.
     *
     * @param askForThem
     *         {@code True} if dialog application should request missing permissions, else {@code false}.
     * @return {@code True} if all permissions are present, else {@code false}.
     */
    public boolean checkMandatoryPermissions(final boolean askForThem) {
        return Main.checkPermissions(this,
                                      askForThem,
                                      Manifest.permission.READ_PHONE_STATE,
                                      Manifest.permission.CAMERA,
                                      Manifest.permission.INTERNET);
    }

    /**
     * Checks if loading indicator or incoming message  is present.
     *
     * @return {@code True} if present, else {@code false}.
     */
    public boolean isOverlayViewVisible() {
        return mLoadingBar != null || mIncomingMessage != null;
    }

    /**
     * Shows the loading indicator with a given caption.
     *
     * @param caption
     *         Caption.
     */
    public void loadingIndicatorShow(final String caption) {
        loadingIndicatorShow(true);
        loadingIndicatorSetCaption(caption);
    }

    /**
     * Hides the loading indicator.
     */
    public void loadingIndicatorHide() {
        loadingIndicatorShow(false);
    }

    /**
     * Updates the FaceId status.
     */
    public void updateFaceIdSupport() {
        // Performance is not an issue. Call unified method to reload GUI.
        if (mLastFragment != null) {
            mLastFragment.reloadGUI();
        }
    }

    /**
     * Approves the OTP.
     *
     * @param message
     *         Message.
     * @param serverChallenge
     *         Server challenge.
     * @param handler
     *         Handler.
     */
    public void approveOTP(@NonNull final String message,
                           @Nullable final SecureString serverChallenge,
                           @NonNull final Protocols.OTPDelegate handler) {
        // Current fragment must be auth solver child.
        if (!(mLastFragment instanceof AbstractMainFragmentWithAuthSolver)) {
            return;
        }

        // Result is handed by main activity approveOTP_Result Approve/Reject
        final AbstractMainFragmentWithAuthSolver authSolver = (AbstractMainFragmentWithAuthSolver) mLastFragment;
        mIncomingMessage = new FragmentIncomingMessage();
        mIncomingMessage.setApproveClickListener(view -> {
            mIncomingMessage = null;
            if (mLastFragment != null) {
                mLastFragment.reloadGUI();
            }
            authSolver.totpWithMostComfortableOne(serverChallenge, handler);
        });
        mIncomingMessage.setRejectClickListener(view -> {
            mIncomingMessage = null;
            if (mLastFragment != null) {
                mLastFragment.reloadGUI();
            }
            handler.onOTPDelegateFinished(null, null, null, null);
        });

        final Bundle agrs = new Bundle();
        agrs.putString(FragmentIncomingMessage.FRAGMENT_ARGUMENT_CAPTION, message);
        mIncomingMessage.setArguments(agrs);

        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container_loading_bar, mIncomingMessage, null).commit();
    }

    /**
     * Enables or disables the action bar.
     *
     * @param enable
     *         {@code True} to enable, else {@code false}.
     */
    public void enableDrawer(final boolean enable) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        if (enable) {
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            mToolbar.setVisibility(View.VISIBLE);
        } else {
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            mToolbar.setVisibility(View.GONE);
        }
    }

    public void closeDrawer() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        mDrawer.closeDrawer(Gravity.LEFT);
    }

    /**
     * Removes the top most fragment from the back-stack.
     */
    public void hideLastStackFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * Clears the fragment stack.
     */
    public void clearFragmentStack() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        for (int i = 0; i < fragmentManager.getBackStackEntryCount(); ++i) {
            fragmentManager.popBackStack();
        }
    }

    /**
     * Hides the keyboard.
     */
    public void hideKeyboard() {
        final View view = findViewById(android.R.id.content);
        if (view != null) {
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Shows an error dialog.
     *
     * @param error
     *         Error the show.
     */
    public void showErrorIfExists(final String error) {
        if (error != null) {
            showMessage(error);
        }
    }

    /**
     * Shows a message dialog.
     *
     * @param message
     *         Message to display.
     */
    public void showMessage(@StringRes final int message) {
        showErrorIfExists(Main.getString(message));
    }

    /**
     * Shows a message dialog.
     *
     * @param message
     *         Message to display.
     */
    public void showMessage(final String message) {
        final Context ctx = this;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Reloads the UI.
     */
    public void reloadGui() {
        final TokenDevice tokenDevice = Main.sharedInstance().getManagerToken().getTokenDevice();
        if (tokenDevice != null) {
            mFaceIdSwitch.setChecked(tokenDevice.getTokenStatus().isFaceEnabled);

            final BioFingerprintAuthService service = BioFingerprintAuthService.create(AuthenticationModule.create());
            if (service.isSupported() && service.isConfigured()) {
                mTouchIdSwitch.setEnabled(true);
                mTouchIdSwitch.setChecked(tokenDevice.getTokenStatus().isTouchEnabled);
            } else {
                mTouchIdSwitch.setEnabled(false);
            }
        }
    }

    /**
     * Shows the provisioning fragment.
     */
    public void showProvisioningFragment() {
        // In case of transition to new tab. We want to clean out any stacked objects.
        clearFragmentStack();

        showFragment(new FragmentProvision());
    }

    /**
     * Shows the authentication fragment.
     */
    public void showAuthenticationFragment() {
        // In case of transition to new tab. We want to clean out any stacked objects.
        clearFragmentStack();

        showFragment(new FragmentAuthentication());
    }

    /**
     * On pressed OTP button.
     *
     * @param authInput
     *         Auth input used to calculate OTP. Used for recalculation.
     * @param challenge
     *         Challenge to be signed.
     * @param amount
     *         Amount.
     * @param beneficiary
     *         Beneficiary.
     */

    public void showOtpFragment(final AuthInput authInput,
                                final SecureString challenge,
                                final String amount,
                                final String beneficiary) {
        if (challenge != null) {
            showFragment(FragmentOtp.transactionSign(authInput, challenge, amount, beneficiary));
        } else {
            showFragment(FragmentOtp.authentication(authInput));
        }
    }

    /**
     * On pressed Sign button.
     */
    public void showSignFragment() {
        showFragment(new FragmentSign());
    }

    //endregion
}
