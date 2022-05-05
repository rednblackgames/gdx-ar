package games.rednblack.gdxar.android;

import android.Manifest;
import android.content.Context;
import android.os.Handler;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.ar.core.ArCoreApk;

import games.rednblack.gdxar.android.util.AcceptableFuture;

/**
 * A no-UI fragment that handles ARCore initialization.  This fragment handles
 * the camera permission, ARCore compatibility and version checks.
 * <p/>
 * To use this fragment, add it to the activity and call {@link ARSupportFragment#getArSupported}
 * to get the result determining if ARCore is supported or not.
 *
 * @author claywilkinson
 */
public class ARSupportFragment extends Fragment {
    public static final String TAG = "ARSupportFragment";

    private Handler handler;
    private AcceptableFuture<Boolean> future;
    private boolean userRequestedInstall = true;

    public ARSupportFragment() {
        // Required empty public constructor
        handler = new Handler();
        future = new AcceptableFuture<>(handler);
    }

    public AcceptableFuture<Boolean> getArSupported() {
        return future;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (checkCameraPermission()) {
            checkArCore();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        ARSupportFragment.this.checkArCore();
                    } else {
                        future.completeExceptionally(new IllegalStateException("Camera permission not granted"));
                    }
                }
            });


    private boolean checkCameraPermission() {
        if (CameraPermissionHelper.hasCameraPermission(requireActivity())) {
            return true;
        }

        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        return false;
    }

    private void checkArCore() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(getActivity());
        if (availability.isTransient() && !future.isCancelled()) {
            // re-query at 5Hz while we check compatibility.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkArCore();
                }
            }, 200);
            return;
        }
        switch (availability) {
            case SUPPORTED_INSTALLED:
                //Done!
                future.complete(true);
                break;
            case SUPPORTED_APK_TOO_OLD:
            case SUPPORTED_NOT_INSTALLED:
                startInstallation();
                break;
            default:
                future.complete(false);
        }
    }

    private void startInstallation() {
        try {
            switch (ArCoreApk.getInstance().requestInstall(getActivity(), userRequestedInstall)) {
                case INSTALLED:
                    // Success.
                    future.complete(true);
                    break;
                case INSTALL_REQUESTED:
                    // Ensures next invocation of requestInstall() will either return
                    // INSTALLED or throw an exception.
                    userRequestedInstall = false;
            }
        } catch (Exception exception) {
            future.completeExceptionally(exception);
        }
    }
}
