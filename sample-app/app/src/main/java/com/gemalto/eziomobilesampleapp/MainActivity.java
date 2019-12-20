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

package com.gemalto.eziomobilesampleapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.gemalto.eziomobilesampleapp.gui.FragmentMissingPermissions;
import com.gemalto.eziomobilesampleapp.gui.MainFragment;
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentLoadingIndicator;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.core.util.SecureString;

import java.util.HashMap;
import java.util.Map;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * App main activity and entry point.
 */
public class MainActivity extends AppCompatActivity {

    //region Defines

    // UI Elements
    private TextView mLabelCaption = null;
    private BottomNavigationView mTabBar = null;
    private FragmentLoadingIndicator mLoadingBar = null;
    private MainFragment mLastFragment = null;
    private Toolbar mToolbar = null;

    // Helpers
    protected EzioSampleApp mMyApp = null;
    private boolean mExitConfirmed = false;

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
        CMain.sharedInstance().init(this);

        // Load basic ui components like tab bar etc...
        initGui();

        // Check for permissions or display fragment with informations.
        if (!checkMandatoryPermissions(true)) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container, new FragmentMissingPermissions(), null)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mMyApp.setCurrentActivity(this);

        if (!CMain.sharedInstance().isInited()) {
            checkPermissionsAndInit();
        } else {
            processIncommiongNotifications();
        }
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

    /**
     * Requests runtime permissions for in Android 6.0+.
     */
    private void checkPermissionsAndInit() {
        // In case we don't have permissions yet, simple wait for another call.
        // FragmentMissingPermissions will take care of that.
        if (!checkMandatoryPermissions(false)) {
            return;
        }

        // SDK init take some time.. display loading bar.
        loadingIndicatorShow(CMain.getString(R.string.LOADING_MESSAGE_INIT));

        // Init rest of the stuff with dependency on permissions.
        CMain.sharedInstance().initWithPermissions(new Protocols.GenericHandler() {
            @Override
            public void onFinished(final boolean success, @Nullable final String error) {

                // Hide loading indicator.
                loadingIndicatorHide();

                if (success) {
                    tabBarSwitchToCurrentState();

                    // Process possible incoming notification.
                    processIncommiongNotifications();
                } else {
                    // Error in such case mean, that we have broken configuration or some internal state of SDK.
                    // Most probably wrong license, different fingerprint etc. We should not continue at that point.
                    System.exit(0);
                }
            }
        });
    }

    /**
     * Shows or hides the loading indicator.
     * @param show {@code True} if the loading indicator should be showed, {@code false} to hide the loading indicator.
     */
    private void loadingIndicatorShow(final boolean show) {
        // Avoid switch to same state.
        if ((mLoadingBar != null && show) || (mLoadingBar == null && !show)) {
            return;
        }

        if (show) {
            mLoadingBar = new FragmentLoadingIndicator();

            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container_loading_bar, mLoadingBar, null)
                    .commit();
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
     * Sets the caption for the loading indicator.
     * @param caption Caption.
     */
    private void loadingIndicatorSetCaption(final String caption) {
        if (mLoadingBar != null) {
            mLoadingBar.setCaption(caption);
        }
    }

    /**
     * Disables screenshots.
     */
    private void disableScreenShot() {
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }

    /**
     * Initializes the GUI of the application.
     */
    private void initGui() {
        setContentView(R.layout.activity_main);

        mLabelCaption = findViewById(R.id.label_caption);
        mTabBar = findViewById(R.id.navigation);
        mToolbar = findViewById(R.id.toolbar);

        mTabBar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
                return handleNavigationItemSelected(item);
            }
        });
    }

    /**
     * Clears the reference to the current {@code Activity}.
     */
    private void clearReferences() {
        final Activity currActivity = mMyApp.getCurrentActivity();
        if (this.equals(currActivity)) {
            mMyApp.setCurrentActivity(null);
        }
    }

    /**
     * Processes incoming notification.
     */
    private void processIncommiongNotifications() {
        // Process possible incoming notification.
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final Map<String, String> data = new HashMap<>();
            for (final String key : extras.keySet()) {
                data.put(key, extras.getString(key));
            }
            CMain.sharedInstance().getManagerPush().processIncommingPush(data);
        }
    }

    //endregion

    //region User Interface

    /**
     * Handles the selected navigation item.
     * @param item Selected item.
     * @return {@code True} if new {@code Fragment} was added, else {@code false}.
     */
    private boolean handleNavigationItemSelected(@NonNull final MenuItem item) {
        // Ignore click on same fragment.
        if (FragmentSolver.getFragmentId(mLastFragment) == item.getItemId()) {
            return false;
        }

        mLabelCaption.setText(item.getTitle());

        final MainFragment newFragment = FragmentSolver.getFragmentById(item.getItemId());

        if (newFragment != null) {
            // In case of transition to new tab. We want to clean out any stacked objects.
            hideLastStackFragment();

            mLastFragment = newFragment;
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container, newFragment, null)
                    .commit();
        }

        return newFragment != null;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
            return;
        }

        if (mExitConfirmed) {
            finish();
        } else {
            Toast.makeText(this, getString(R.string.toast_exit), Toast.LENGTH_SHORT).show();
            mExitConfirmed = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mExitConfirmed = false;
                }
            }, 3000);
        }
    }


    //endregion

    //region Public API

    /**
     * Checks for mandatory permission.
     * @param askForThem {@code True} if application should request missing permission.
     * @return {@code True} if permissions were acquired successfully.
     */
    public boolean checkMandatoryPermissions(final boolean askForThem) {
        return CMain.checkPermissions(this, askForThem,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET);
    }

    /**
     * Checks if the loading indicator is currently shown.
     * @return {@code True} if loading indicator is currently shown, else {@code false}.
     */
    public boolean loadingIndicatorIsPresent() {
        return mLoadingBar != null;
    }

    /**
     * Shows the loading indicator with caption.
     * @param caption Caption to show in loading indicator.
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
     * Updates the tab bar.
     */
    public void tabBarUpdate() {
        final boolean tokenEnrolled = CMain.sharedInstance().getManagerToken().getTokenDevice() != null;
        for (int index = 0; index < mTabBar.getMenu().size(); index++) {
            mTabBar.getMenu().getItem(index).setEnabled(tokenEnrolled ? index != 0 : index == 0);
        }
    }

    /**
     * Disables the tap bar.
     */
    public void tabBarDisable() {
        for (int index = 0; index < mTabBar.getMenu().size(); index++) {
            mTabBar.getMenu().getItem(index).setEnabled(false);
        }
    }

    /**
     * Switch the tab bar to current state based on if token is enrolled.
     */
    public void tabBarSwitchToCurrentState() {
        // Unlike on iOS we have to reload tab state first. Otherwise switch is not triggered.
        tabBarUpdate();

        if (CMain.sharedInstance().getManagerToken().getTokenDevice() != null) {
            mTabBar.setSelectedItemId(R.id.navigation_authentication);
        } else {
            mTabBar.setSelectedItemId(R.id.navigation_enroll);
        }
    }

    /**
     * Reloads the GUI.
     */
    public void updateFaceIdSupport() {
        // Performance is not an issue. Call unified method to reload GUI.
        if (mLastFragment != null) {
            mLastFragment.reloadGUI();
        }
    }

    /**
     * Updates the push registration status.
     */
    public void updatePushRegistrationStatus() {
        if (mLastFragment != null) {
            mLastFragment.updatePushRegistrationStatus();
        }
    }

    /**
     * Approves the OTP.
     * @param message Message to show.
     * @param serverChallenge Server challenge.
     * @param handler OTP calculation callback.
     */
    public void approveOTP(@NonNull final String message, @Nullable final SecureString serverChallenge, @NonNull final Protocols.OTPDelegate handler) {
        if (mLastFragment != null) {
            mLastFragment.approveOTP(message, serverChallenge, handler);
        }
    }

    /**
     * Removes the last {@code Fragment} from the back stack.
     */
    public void hideLastStackFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
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
     * @param error Error message to show.
     */
    public void showErrorIfExists(final String error) {
        if (error != null) {
            showMessage(CMain.getString(R.string.COMMON_MESSAGE_ERROR_CAPTION), error);
        }
    }

    /**
     * Show dialog.
     * @param caption Caption.
     * @param description Description.
     */
    public void showMessage(final String caption, final String description) {
        final Context ctx = this;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ctx);
                dialogBuilder.setTitle(caption);
                dialogBuilder.setMessage(description);
                dialogBuilder.setPositiveButton(getString(R.string.COMMON_MESSAGE_OK), null);
                dialogBuilder.setCancelable(true);
                dialogBuilder.create().show();
            }
        });
    }

    /**
     * Hides the toolbar.
     */
    public void hideToolbar() {
        if(mToolbar != null) {
            mToolbar.setVisibility(View.GONE);
        }
    }

    /**
     * Shows the toolbar.
     */
    public void showToolbar() {
        if(mToolbar != null) {
            mToolbar.setVisibility(View.VISIBLE);
        }
    }

    //endregion
}
