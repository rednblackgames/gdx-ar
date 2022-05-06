package games.rednblack.gdxar.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pools;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import java.io.InputStream;
import java.util.Collection;

import games.rednblack.gdxar.*;
import games.rednblack.gdxar.util.PlaneModel;
import games.rednblack.gdxar.android.util.ARCoreToGdxAR;
import games.rednblack.gdxar.util.DebugShaderProvider;
import games.rednblack.gdxar.util.RawAugmentedImageAsset;

/**
 * ARCoreApplication is the base class for the scene to render. Application specific code should be
 * put in {@link GdxArApplicationListener}
 *
 * This class handles the basic boilerplate of rendering the background image, moving the camera
 * based on the ARCore frame pose, and basic batch rendering.
 *
 * @author fgnm
 */
public class ARCoreApplication implements ApplicationListener, GdxAR {
    // The camera which is controlled by the ARCore pose.
    private PerspectiveCamera arCamera;
    // Renderer for the camera image which is the background for the ARCore app.
    private BackgroundRenderer backgroundRenderer;
    // Drawing batch.
    private ModelBatch debugModelBatch;

    protected GdxArApplicationListener gdxArApplicationListener;
    protected GdxARConfiguration gdxARConfiguration;
    protected Config sessionConfig;

    private final float[] cameraProjectionMatrix = new float[16];
    private final float[] colorCorrection = new float[4];

    protected boolean hasSurface = false;
    protected boolean renderAR = false;
    protected final GdxFrame frameInstance;

    public ARCoreApplication(GdxArApplicationListener gdxArApplicationListener, GdxARConfiguration gdxARConfiguration) {
        this.gdxArApplicationListener = gdxArApplicationListener;
        this.gdxArApplicationListener.setArAPI(this);
        this.gdxARConfiguration = gdxARConfiguration;

        frameInstance = new GdxFrame();
    }

    @Override
    public void setRenderAR(boolean renderAR) {
        this.renderAR = renderAR;
    }

    @Override
    public boolean loadAugmentedImageDatabase(InputStream databaseStream) {
        try {
            AugmentedImageDatabase augmentedImageDatabase = AugmentedImageDatabase.deserialize(getSession(), databaseStream);
            sessionConfig.setAugmentedImageDatabase(augmentedImageDatabase);
            getSession().configure(sessionConfig);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public IntMap<String> buildAugmentedImageDatabase(Array<RawAugmentedImageAsset> images) {
        IntMap<String> map = new IntMap<>();

        AugmentedImageDatabase augmentedImageDatabase = new AugmentedImageDatabase(getSession());
        for (RawAugmentedImageAsset image : images) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(image.inputStream);
                int index = augmentedImageDatabase.addImage(image.name, bitmap, image.widthInMeter);
                map.put(index, image.name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sessionConfig.setAugmentedImageDatabase(augmentedImageDatabase);
        getSession().configure(sessionConfig);
        return map;
    }

    /**
     * Camera controlled by ARCore. This is used to determine where the user is looking.
     */
    @Override
    public PerspectiveCamera getARCamera() {
        return arCamera;
    }

    /**
     * ARCore session object.
     */
    protected Session getSession() {
        return ((ARFragmentApplication) Gdx.app).getSessionSupport().getSession();
    }

    @Override
    public void create() {
        String version = Gdx.graphics.getGL20().glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        System.out.println("Shaders version " + version);

        arCamera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        arCamera.position.set(0, 1.6f, 0f);
        arCamera.lookAt(0, 0, 1f);
        arCamera.near = .01f;
        arCamera.far = 30f;
        arCamera.update();

        backgroundRenderer = new BackgroundRenderer();

        debugModelBatch = new ModelBatch(new DebugShaderProvider());

        sessionConfig = new Config(getSession());
        // Check whether the user's device supports the Depth API.
        boolean isDepthSupported = getSession().isDepthModeSupported(Config.DepthMode.AUTOMATIC);
        if (isDepthSupported) {
            sessionConfig.setDepthMode(Config.DepthMode.AUTOMATIC);
        }
        sessionConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        sessionConfig.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
        switch (gdxARConfiguration.lightEstimationMode) {
            case DISABLED:
                sessionConfig.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
            case AMBIENT_INTENSITY:
                sessionConfig.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
            case ENVIRONMENTAL_HDR:
                sessionConfig.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
        }

        getSession().configure(sessionConfig);

        gdxArApplicationListener.create(arCamera);
    }

    @Override
    public void setAutofocus(boolean autofocus) {
        Config.FocusMode newFocusMode = autofocus ? Config.FocusMode.AUTO : Config.FocusMode.FIXED;
        if (sessionConfig.getFocusMode() == newFocusMode) return;
        sessionConfig.setFocusMode(newFocusMode);
        getSession().configure(sessionConfig);
    }

    @Override
    public void resize(int width, int height) {
        gdxArApplicationListener.resize(width, height);
    }

    @Override
    public void render() {
        if (renderAR) {
            // Boiler plate rendering code goes here, the intent is that this sets up the scene object,
            // Application specific rendering should be done from render(Frame).
            ARCoreGraphics arCoreGraphics = (ARCoreGraphics) Gdx.graphics;
            Frame frame = arCoreGraphics.getCurrentFrame();

            // Frame can be null when initializing or if ARCore is not supported on this device.
            if (frame == null) {
                return;
            }

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            backgroundRenderer.render(frame);

            Collection<Plane> surfaces = getSession().getAllTrackables(Plane.class);
            // If we're still loading/detecting planes, just return.
            if (handleLoadingMessage(frame, surfaces)) {
                frame.getCamera().getProjectionMatrix(cameraProjectionMatrix, 0, arCamera.near, arCamera.far);
                arCamera.projection.set(cameraProjectionMatrix);
                frame.getCamera().getViewMatrix(cameraProjectionMatrix, 0);
                arCamera.view.set(cameraProjectionMatrix);
                arCamera.combined.set(arCamera.projection);
                Matrix4.mul(arCamera.combined.val, arCamera.view.val);

                frameInstance.reset();

                for (Plane plane : surfaces) {
                    GdxPlane gdxPlane = ARCoreToGdxAR.createGdxPlane(plane);
                    frameInstance.addPlane(gdxPlane);
                }

                for (Anchor anchor : frame.getUpdatedAnchors()) {
                    GdxAnchor gdxAnchor = ARCoreToGdxAR.createGdxAnchor(anchor);
                    frameInstance.addAnchor(gdxAnchor);
                }

                for (AugmentedImage img : frame.getUpdatedTrackables(AugmentedImage.class)) {
                    GdxAugmentedImage augmentedImage = ARCoreToGdxAR.createGdxAugmentedImage(img);
                    frameInstance.addAugmentedImage(augmentedImage);
                }

                // Get the light estimate for the current frame.
                LightEstimate lightEstimate = frame.getLightEstimate();
                frameInstance.lightEstimationMode = ARCoreToGdxAR.map(sessionConfig.getLightEstimationMode());

                if (frameInstance.lightEstimationMode == GdxLightEstimationMode.ENVIRONMENTAL_HDR) {
                    float[] intensity = lightEstimate.getEnvironmentalHdrMainLightIntensity();

                    float mainLightIntensityScalar = Math.max(1.0f, Math.max(Math.max(intensity[0], intensity[1]), intensity[2]));
                    frameInstance.lightIntensity = mainLightIntensityScalar;

                    frameInstance.lightColor.set(intensity[0] / mainLightIntensityScalar, intensity[1] / mainLightIntensityScalar, intensity[2] / mainLightIntensityScalar, 1);
                    float[] direction = lightEstimate.getEnvironmentalHdrMainLightDirection();

                    frameInstance.lightDirection.set(direction);
                    frameInstance.lightDirection.y = -frameInstance.lightDirection.y;

                    float[] sphericalHarmonics = lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics();
                    frameInstance.sphericalHarmonics.set(sphericalHarmonics);

                    frameInstance.ambientIntensity = (sphericalHarmonics[0] + sphericalHarmonics[1] + sphericalHarmonics[2]) / 3f;
                } else if (frameInstance.lightEstimationMode == GdxLightEstimationMode.AMBIENT_INTENSITY) {
                    frameInstance.ambientIntensity = lightEstimate.getPixelIntensity();

                    lightEstimate.getColorCorrection(colorCorrection, 0);
                    frameInstance.lightColor.set(colorCorrection[0], colorCorrection[1], colorCorrection[2], 1);
                    frameInstance.lightIntensity = colorCorrection[3];
                }

                //TODO draw depth mask
                /*Gdx.gl.glDepthMask(true);
                try {
                    Image depth = frame.acquireRawDepthConfidenceImage();
                    Gdx.gl.glColorMask(false, false, false, false);
                    //glDrawElements
                    Gdx.gl.glColorMask(true, true, true, true);
                } catch (NotYetAvailableException e) {
                    e.printStackTrace();
                }*/

                Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
                Gdx.gl.glEnable(GL20.GL_CULL_FACE);

                if (gdxARConfiguration.debugMode) {
                    debugModelBatch.begin(arCamera);
                    drawPlanes(debugModelBatch, surfaces);
                }

                gdxArApplicationListener.renderARModels(frameInstance);

                if (gdxARConfiguration.debugMode) debugModelBatch.end();
            }
        }

        gdxArApplicationListener.render();
    }

    @Override
    public void pause() {
        gdxArApplicationListener.pause();
    }

    @Override
    public void resume() {
        gdxArApplicationListener.resume();
    }

    @Override
    public void dispose() {
        gdxArApplicationListener.dispose();
    }

    @Override
    @Null
    public GdxAnchor requestHitPlaneAnchor(float x, float y) {
        ARCoreGraphics arCoreGraphics = (ARCoreGraphics) Gdx.graphics;
        Frame frame = arCoreGraphics.getCurrentFrame();

        for (HitResult hit : frame.hitTest(x, y)) {
            Trackable trackable = hit.getTrackable();
            Pose pose = hit.getHitPose();
            if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(pose)) {
                Plane plane = (Plane) trackable;
                if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) continue;
                Anchor newAnchor = plane.createAnchor(pose);
                return ARCoreToGdxAR.createGdxAnchor(newAnchor);
            }
        }
        return null;
    }

    @Override
    @Null
    public GdxPose requestHitPlanePose(float x, float y) {
        ARCoreGraphics arCoreGraphics = (ARCoreGraphics) Gdx.graphics;
        Frame frame = arCoreGraphics.getCurrentFrame();

        for (HitResult hit : frame.hitTest(x, y)) {
            Trackable trackable = hit.getTrackable();
            Pose pose = hit.getHitPose();
            if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(pose)) {
                Plane plane = (Plane) trackable;
                if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) continue;
                GdxPose gdxPose = Pools.obtain(GdxPose.class);
                ARCoreToGdxAR.map(pose, gdxPose);
                return gdxPose;
            }
        }
        return null;
    }

    /**
     * Handles showing the loading message, then hiding it once a plane is detected.
     *
     * @param frame - the ARCore frame.
     * @return true once a plane is loaded.
     */
    private boolean handleLoadingMessage(Frame frame, Collection<Plane> surfaces) {
        // If not tracking, don't draw 3d objects.
        if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            gdxArApplicationListener.lookingSurfaces(false);
            hasSurface = false;
            return false;
        }

        if (surfaces.size() == 0) {
            gdxArApplicationListener.lookingSurfaces(false);
            hasSurface = false;
            return false;
        }

        // Check if we detected at least one plane. If so, hide the loading message.
        if (!hasSurface) {
            for (Plane plane : surfaces) {
                if (plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING
                        && plane.getTrackingState() == TrackingState.TRACKING) {
                    gdxArApplicationListener.lookingSurfaces(true);
                    hasSurface = true;
                }
            }
        }
        return hasSurface;
    }

    Array<ModelInstance> planeInstances = new Array<>();
    /** Draws the planes detected. */
    private void drawPlanes(ModelBatch modelBatch,  Collection<Plane> surfaces) {
        planeInstances.clear();
        int index = 0;
        for (Plane plane : surfaces) {

            // check for planes that are no longer valid
            if (plane.getSubsumedBy() != null
                    || plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING
                    || plane.getTrackingState() == TrackingState.STOPPED
                    || plane.getPolygon().capacity() == 0) {
                continue;
            }
            // New plane
            Model planeModel = PlaneModel.createPlane(plane.getPolygon(), plane.getExtentX(), plane.getExtentZ(), index++);
            if (planeModel == null) {
                continue;
            }
            ModelInstance instance = new ModelInstance(planeModel);
            instance.transform.setToTranslation(
                    plane.getCenterPose().tx(), plane.getCenterPose().ty(), plane.getCenterPose().tz());
            planeInstances.add(instance);
        }
        modelBatch.render(planeInstances);
    }
}

