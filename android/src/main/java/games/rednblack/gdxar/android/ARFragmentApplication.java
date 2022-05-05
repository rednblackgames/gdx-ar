package games.rednblack.gdxar.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import games.rednblack.gdxar.GdxArApplicationListener;
import games.rednblack.gdxar.android.util.ARSessionSupport;

/**
 * Android Fragment subclass that handles initializing ARCore and the underlying graphics engine
 * used for drawing 3d and 2d models in the context of the ARCore Frame. This class is based on the
 * libgdx library for Android game development.
 *
 * @author claywilkinson, fgnm
 */
public class ARFragmentApplication extends AndroidFragmentApplication implements LifecycleOwner,
        ARSessionSupport.StatusChangeListener {

    // ARCore specific stuff
    private ARSessionSupport sessionSupport;
    private Snackbar messageSnackbar;

    // Implement the LifecycleOwner interface since AndroidApplication does not extend AppCompatActivity.
    // All this means is forward the events to the lifecycleRegistry object.
    private LifecycleRegistry lifecycleRegistry;
    private ARCoreApplication arApplication;
    private AndroidApplicationConfiguration configuration;

    public ARFragmentApplication() {
        lifecycleRegistry = new LifecycleRegistry(this);
    }

    public void setArApplication(GdxArApplicationListener arApplication) {
        this.arApplication = new ARCoreApplication(arApplication);
    }

    public ARCoreApplication getArApplication() {
        return arApplication;
    }

    public AndroidApplicationConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(AndroidApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Gets the ARCore session.  It can be null if the
     * permissions were not granted by the user or if the device does not support ARCore.
     */
    @NonNull
    public ARSessionSupport getSessionSupport() {
        return sessionSupport;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        sessionSupport = new ARSessionSupport(requireActivity(), lifecycleRegistry, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidApplicationConfiguration config = getConfiguration();
        if (config == null) {
            config = new AndroidApplicationConfiguration();
        }
        return initializeForView(getArApplication(), config);
    }

    @Override
    public void onStart() {
        super.onStart();
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }

    public void onPause() {
        super.onPause();
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }

    @Override
    public void onResume() {
        super.onResume();
        lifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);
    }

    private void showSnackbarMessage(String message, boolean finishOnDismiss) {
        messageSnackbar =
                Snackbar.make(
                        requireActivity().getWindow().getDecorView(),
                        message + "\n",
                        Snackbar.LENGTH_INDEFINITE);
        messageSnackbar.getView().setBackgroundColor(0xbf323232);
        if (finishOnDismiss) {
            messageSnackbar.setAction(
                    "Dismiss",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            messageSnackbar.dismiss();
                        }
                    });
            messageSnackbar.addCallback(
                    new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);
                            requireActivity().finish();
                        }
                    });
        }
        messageSnackbar.show();
    }

    /**
     * This method has to be called in the {Activity#onCreate(Bundle)} method. It sets up all
     * the things necessary to get input, render via OpenGL and so on. You can configure other aspects
     * of the application with the rest of the fields in the {@link AndroidApplicationConfiguration}
     * instance.
     *
     * @param listener the {@link ApplicationListener} implementing the program logic
     * @param config   the {@link AndroidApplicationConfiguration}, defining various settings of the
     *                 application (use accelerometer, etc.).
     */
    @Override
    public View initializeForView(ApplicationListener listener, AndroidApplicationConfiguration config) {
        super.initializeForView(listener, config);
        initArGraphics(config);
        return graphics.getView();
    }

    private void initArGraphics(AndroidApplicationConfiguration config) {
        super.graphics =
                new ARCoreGraphics(
                        this,
                        config,
                        config.resolutionStrategy == null
                                ? new FillResolutionStrategy()
                                : config.resolutionStrategy);
        input = createInput(this, requireContext(), graphics.getView(), config);
        Gdx.app = this;
        Gdx.input = this.getInput();
        Gdx.audio = this.getAudio();
        Gdx.files = this.getFiles();
        Gdx.graphics = this.getGraphics();
        Gdx.net = this.getNet();
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    @Override
    public void onStatusChanged() {
        if (getSessionSupport().getStatus() != ARSessionSupport.ARStatus.Ready) {
            showSnackbarMessage("Error with ARCore: " + getSessionSupport().getStatus(), true);
        }
    }
}

