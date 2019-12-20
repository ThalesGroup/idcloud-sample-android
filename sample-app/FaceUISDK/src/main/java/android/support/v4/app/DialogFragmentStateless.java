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

package android.support.v4.app;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * A support dialog fragment that allow show in state loss mode
 */
public class DialogFragmentStateless extends DialogFragment
{

    /**
     * Display the dialog, adding the fragment using an existing transaction and then committing the
     * transaction whilst allowing state loss.<br>
     * 
     * I would recommend you use {@link #show(FragmentTransaction, String)} most of the time but
     * this is for dialogs you reallly don't care about. (Debug/Tracking/Adverts etc.)
     *
     * @param transaction
     *            An existing transaction in which to add the fragment.
     * @param tag
     *            The tag for this fragment, as per
     *            {@link FragmentTransaction#add(Fragment, String) FragmentTransaction.add}.
     * @return Returns the identifier of the committed transaction, as per
     *         {@link FragmentTransaction#commit() FragmentTransaction.commit()}.
     * @see DialogFragmentStateless#showAllowingStateLoss(FragmentManager, String)
     */
    public int showAllowingStateLoss(FragmentTransaction transaction, String tag)
    {
        mDismissed = false;
        mShownByMe = true;
        transaction.add(this, tag);
        mViewDestroyed = false;
        mBackStackId = transaction.commitAllowingStateLoss();
        return mBackStackId;
    }

    /**
     * Display the dialog, adding the fragment to the given FragmentManager. This is a convenience
     * for explicitly creating a transaction, adding the fragment to it with the given tag, and
     * committing it without careing about state. This does <em>not</em> add the transaction to the
     * back stack. When the fragment is dismissed, a new transaction will be executed to remove it
     * from the activity.<br>
     * 
     * I would recommend you use {@link #show(FragmentManager, String)} most of the time but this is
     * for dialogs you reallly don't care about. (Debug/Tracking/Adverts etc.)
     * 
     * 
     * @param manager
     *            The FragmentManager this fragment will be added to.
     * @param tag
     *            The tag for this fragment, as per
     *            {@link FragmentTransaction#add(Fragment, String) FragmentTransaction.add}.
     * @see DialogFragmentStateless#showAllowingStateLoss(FragmentTransaction, String)
     */
    public void showAllowingStateLoss(FragmentManager manager, String tag)
    {
        mDismissed = false;
        mShownByMe = true;
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }
}
