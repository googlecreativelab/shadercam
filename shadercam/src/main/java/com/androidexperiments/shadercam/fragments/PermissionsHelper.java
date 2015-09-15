package com.androidexperiments.shadercam.fragments;

import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Helper headless fragment for handling Android M permissions model, feel free to use this or
 * implement your own permissions strategy.
 *
 * More info on this strategy here:
 * https://plus.google.com/+JoshBrown42/posts/FzNghPbKk2s
 *
 */
public class PermissionsHelper extends Fragment {

    private static final String TAG = PermissionsHelper.class.getCanonicalName();
    private static final int PERMISSION_REQUEST_CODE = 100;

    private static PermissionsHelper __instance;
    private String[] mPermissions = null;

    public static PermissionsHelper getInstance()
    {
        if(__instance == null)
        {
            __instance = new PermissionsHelper();
            __instance.setRetainInstance(true);
        }
        return __instance;
    }

    /**
     * Set the permissions to be checked during  {@link #checkPermissions()}
     * @param permissions
     */
    public void setRequestedPermissions(String... permissions) {
        mPermissions = permissions;
    }

    public boolean checkPermissions() {
        if(mPermissions == null)
            throw new RuntimeException("String[] permissions is null, please call setRequestedPermissions!");

        //if it has the permissions, don't wait for async callback and
        //just return true so apps can continue as normal
        if(hasSelfPermission(mPermissions) && getParent() != null) {
            getParent().onPermissionsSatisfied();
            return true;
        }
        else
            requestPermissions(mPermissions, PERMISSION_REQUEST_CODE);

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult() " + Arrays.toString(permissions) + Arrays.toString(grantResults));

        if(requestCode == PERMISSION_REQUEST_CODE && getParent() != null) {
            if(verifyPermissions(grantResults)) {
                getParent().onPermissionsSatisfied();
            }
            else {
                getParent().onPermissionsFailed(getFailedPermissions(permissions, grantResults));
            }
        }
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private String[] getFailedPermissions(String[] permissions, int[] grantResults) {
        ArrayList<String> failedPermissions = new ArrayList<>();
        for(int i = 0; i < grantResults.length; i++) {
            int result = grantResults[i];
            if(result == PackageManager.PERMISSION_DENIED) {
                failedPermissions.add(permissions[i]);
            }
        }
        String[] failed = new String[failedPermissions.size()];
        failed = failedPermissions.toArray(failed);
        return failed;
    }

    /**
     * Interface to be called back on when all permissions are met
     */
    public interface PermissionsListener {
        void onPermissionsSatisfied();
        void onPermissionsFailed(String[] failedPermissions);
    }

    public static boolean isMorHigher(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static <ParentFrag extends Fragment & PermissionsListener> PermissionsHelper attach(ParentFrag parent) {
        return attach(parent.getChildFragmentManager());
    }

    public static <ParentActivity extends FragmentActivity & PermissionsListener> PermissionsHelper attach(ParentActivity parent) {
        return attach(parent.getSupportFragmentManager());
    }

    private static PermissionsHelper attach(FragmentManager fragmentManager) {
        PermissionsHelper frag = (PermissionsHelper) fragmentManager.findFragmentByTag(TAG);
        if (frag == null) {
            frag = getInstance();
            fragmentManager.beginTransaction().add(frag, TAG).commit();
        }
        return frag;
    }

    private PermissionsListener getParent() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof PermissionsListener) {
            return (PermissionsListener) parentFragment;
        } else {
            FragmentActivity activity = getActivity();
            if (activity instanceof PermissionsListener) {
                return (PermissionsListener) activity;
            }
        }
        return null;
    }

    /**
     * Check that all given permissions have been granted by verifying that each entry in the
     * given array is of the value {@link PackageManager#PERMISSION_GRANTED}.
     */
    public boolean verifyPermissions(int[] grantResults) {
        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the Activity has access to all given permissions.
     */
    public boolean hasSelfPermission(String[] permissions) {
        // Verify that all required permissions have been granted
        for (String permission : permissions) {
            if (getActivity().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the Activity has access to a given permission.
     * Always returns true on platforms below M.
     */
    public boolean hasSelfPermission(String permission) {
        // Below Android M all permissions are granted at install time and are already available.

        return getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

}
