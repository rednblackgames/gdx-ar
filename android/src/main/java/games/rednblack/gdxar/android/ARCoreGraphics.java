package games.rednblack.gdxar.android;

import android.view.Surface;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.badlogic.gdx.backends.android.surfaceview.ResolutionStrategy;
import com.google.ar.core.Frame;

import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Extended AndroidGraphics that is ARCore aware. This handles creating an OES Texture an passing it
 * to the ARCore session.
 *
 * @author claywilkinson
 */
public class ARCoreGraphics extends AndroidGraphics {

    private final ARFragmentApplication application;
    private final BackgroundRendererHelper mBackgroundRenderer;
    private final AtomicReference<Frame> mCurrentFrame;

    public ARCoreGraphics(
            ARFragmentApplication arCoreApplication,
            AndroidApplicationConfiguration config,
            ResolutionStrategy resolutionStrategy) {
        super(arCoreApplication, config, resolutionStrategy);
        application = arCoreApplication;

        mBackgroundRenderer = new BackgroundRendererHelper();
        mCurrentFrame = new AtomicReference<>(null);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);
        WindowManager mgr = null;
        mgr = application.requireActivity().getSystemService(WindowManager.class);
        int rotation = Surface.ROTATION_0;
        if (mgr != null) {
            rotation = mgr.getDefaultDisplay().getRotation();
        }
        application.getSessionSupport().setDisplayGeometry(rotation, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        mBackgroundRenderer.createOnGlThread(application.requireContext());
        application.getSessionSupport().setCameraTextureName(mBackgroundRenderer.getTextureId());
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);
        mCurrentFrame.set(null);
    }

    public int getBackgroundTexture() {
        return mBackgroundRenderer.getTextureId();
    }

    public float[] getBackgroundVertices(Frame frame) {
        return mBackgroundRenderer.getVertices(frame);
    }

    /**
     * Returns the current ARCore frame.  This is reset at the end of the render loop.
     */
    public Frame getCurrentFrame() {
        if (mCurrentFrame.get() == null) {
            mCurrentFrame.compareAndSet(null, application.getSessionSupport().update());
        }
        return mCurrentFrame.get();
    }
}

