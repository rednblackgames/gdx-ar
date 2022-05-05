package games.rednblack.gdxar.android.util;

import android.Manifest;
import android.content.Context;
import android.util.Log;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import games.rednblack.gdxar.android.CameraPermissionHelper;

/**
 * ARCore session creation is a complex flow of exception handling, permission requesting, and
 * possible downloading of the ARCore services APK.  This class encapsulates all this.
 * <p>
 * To use this class create an instance in onCreate().
 *
 * @author claywilkinson
 */
public class ARSessionSupport implements DefaultLifecycleObserver {
    private static final String TAG = "ARSessionSupport";
    private final FragmentActivity activity;
    private Session session;
    private ARStatus status;
    private StatusChangeListener statusListener;
    private int textureId;
    private int rotation;
    private int width;
    private int height;
    private boolean mUserRequestedInstall;

    public ARSessionSupport(FragmentActivity activity, Lifecycle lifecycle, StatusChangeListener listener) {
        this.activity = activity;
        setStatus(ARStatus.Uninitialized);
        lifecycle.addObserver(this);

        // Handle graphics initialization during the ARCore startup.
        textureId = -1;
        rotation = -1;
        width = -1;
        height = -1;
        statusListener = listener;
        mUserRequestedInstall = true;
    }

    /**
     * Gets the current status of the ARCore Session.
     */
    public ARStatus getStatus() {
        return status;
    }

    private void setStatus(ARStatus newStatus) {
        status = newStatus;
        if (statusListener != null) {
            statusListener.onStatusChanged();
        }
    }

    @Nullable
    public Session getSession() {
        return session;
    }

    private void initializeARCore() {
        Exception exception = null;
        String message = null;

        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(activity);
        Log.d(TAG, "Availability is " + availability);

        try {

            if (ArCoreApk.getInstance().requestInstall(activity, mUserRequestedInstall) ==
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                // Ensures next invocation of requestInstall() will either return
                // INSTALLED or throw an exception.
                mUserRequestedInstall = false;
                return;
            }

            session = new Session(activity);
        } catch (UnavailableArcoreNotInstalledException e) {
            setStatus(ARStatus.ARCoreNotInstalled);
            message = "Please install ARCore";
            exception = e;
        } catch (UnavailableApkTooOldException e) {
            setStatus(ARStatus.ARCoreTooOld);
            message = "Please update ARCore";
            exception = e;
        } catch (UnavailableSdkTooOldException e) {
            message = "Please update this app";
            setStatus(ARStatus.SDKTooOld);
            exception = e;
        } catch (UnavailableDeviceNotCompatibleException e) {
            setStatus(ARStatus.DeviceNotSupported);
            message = "This device does not support AR";
            exception = e;
        } catch (Exception e) {
            setStatus(ARStatus.UnknownException);
            message = "This device does not support AR";
            exception = e;
        }

        if (message != null) {
            Log.e(TAG, "Exception creating session", exception);
            return;
        }

        // Set the graphics information if it was already passed in.
        if (textureId >= 0) {
            setCameraTextureName(textureId);
        }
        if (width > 0) {
            setDisplayGeometry(rotation, width, height);
        }

        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Exception resuming session", e);
            return;
        }
        setStatus(ARStatus.Ready);
    }

    /**
     * Handle the onResume event.  This checks the permissions and
     * initializes ARCore.
     */
    public void onResume(@NonNull LifecycleOwner owner) {
        if (CameraPermissionHelper.hasCameraPermission(activity)) {
            if (session == null) {
                initializeARCore();
            } else {
                try {
                    session.resume();
                } catch (CameraNotAvailableException e) {
                    Log.e(TAG, "Exception resuming session", e);
                }
            }
        } else {
            requestCameraPermission();
        }
    }

    public void onPause(@NonNull LifecycleOwner owner) {
        if (session != null) {
            session.pause();
        }
    }

    public void onStop(@NonNull LifecycleOwner owner) {
        statusListener = null;
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {

    }

    /**
     * Start a fragment to handle the permissions.  This is done in a fragment to
     * avoid entangling it with the base Activity.
     */
    private void requestCameraPermission() {
        PermissionFragment fragment = new PermissionFragment();
        fragment.setArSessionSupport(this);
        FragmentManager mgr = activity.getSupportFragmentManager();
        FragmentTransaction trans = mgr.beginTransaction();
        trans.add(fragment, PermissionFragment.TAG);
        trans.commit();
    }

    /**
     * Handle setting the display geometry.  The values are cached
     * if they are set before the session is available.
     */
    public void setDisplayGeometry(int rotation, int width, int height) {
        if (session != null) {
            session.setDisplayGeometry(rotation, width, height);
        } else {
            this.rotation = rotation;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Handle setting the texture ID for the background image.  The value is cached
     * if it is called before the session is available.
     */
    public void setCameraTextureName(int textureId) {
        if (session != null) {
            session.setCameraTextureName(textureId);
            this.textureId = -1;
        } else {
            this.textureId = textureId;
        }
    }

    @Nullable
    public Frame update() {
        if (session != null) {
            try {
                return session.update();
            } catch (CameraNotAvailableException e) {
                Log.e(TAG, "Exception resuming session", e);
            }
        }
        return null;
    }

    public enum ARStatus {
        ARCoreNotInstalled,
        ARCoreTooOld,
        SDKTooOld,
        UnknownException,
        NeedCameraPermission,
        Ready,
        DeviceNotSupported,
        Uninitialized
    }

    /**
     * Interface for listening for status changes. This can be used for showing error messages
     * to the user.
     */
    public interface StatusChangeListener {
        void onStatusChanged();
    }

    /**
     * PermissionFragment handles requesting the camera permission.
     */
    public static class PermissionFragment extends Fragment {
        static final String TAG = "PermissionFragment";
        private ARSessionSupport arSessionSupport;

        private final ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean isGranted) {
                        if (!CameraPermissionHelper.hasCameraPermission(PermissionFragment.this.getActivity())) {
                            arSessionSupport.setStatus(ARStatus.NeedCameraPermission);
                        } else {
                            arSessionSupport.initializeARCore();
                        }

                        FragmentManager mgr = PermissionFragment.this.getParentFragmentManager();
                        FragmentTransaction trans = mgr.beginTransaction();
                        trans.remove(PermissionFragment.this);
                        trans.commit();
                    }
                });

        public void setArSessionSupport(ARSessionSupport arSessionSupport) {
            this.arSessionSupport = arSessionSupport;
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
}
