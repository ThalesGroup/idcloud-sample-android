package android.support.v4.app;

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
