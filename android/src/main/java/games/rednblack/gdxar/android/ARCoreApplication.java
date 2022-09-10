package games.rednblack.gdxar.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pools;
import com.google.ar.core.*;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.List;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import games.rednblack.gdxar.*;
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

    private final Array<ModelInstance> planeInstances = new Array<>();
    private final IntMap<ModelInstance> polygonsCache = new IntMap<>();
    private final ModelBuilder builder = new ModelBuilder();
    private final float[] tmpVerts = new float[14];

    public ARCoreApplication(GdxArApplicationListener gdxArApplicationListener, GdxARConfiguration gdxARConfiguration) {
        this.gdxArApplicationListener = gdxArApplicationListener;
        this.gdxArApplicationListener.setArAPI(this);
        this.gdxARConfiguration = new GdxARConfiguration(gdxARConfiguration);

        frameInstance = new GdxFrame();
    }

    @Override
    public void setRenderAR(boolean renderAR) {
        if (this.renderAR == renderAR) return;

        if (renderAR) {
            try {
                getSession().resume();
                this.renderAR = true;
            } catch (CameraNotAvailableException e) {
                this.renderAR = false;
                e.printStackTrace();
            }
        } else {
            getSession().pause();
            this.renderAR = false;
        }
    }

    @Override
    public boolean isRenderingAR() {
        return renderAR;
    }

    @Override
    public boolean isARAllowed() {
        return true;
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
     * Get current ARCore session from framework.
     *
     * @return ARCore session object
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
        sessionConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);

        //Setup Depth
        if (gdxARConfiguration.enableDepth) {
            // Check whether the user's device supports the Depth API.
            boolean isDepthSupported = getSession().isDepthModeSupported(Config.DepthMode.AUTOMATIC);
            if (isDepthSupported) {
                sessionConfig.setDepthMode(Config.DepthMode.AUTOMATIC);
            }
            gdxARConfiguration.enableDepth = isDepthSupported;
        }

        //Setup Plane Finding Mode
        switch (gdxARConfiguration.planeFindingMode) {
            case DISABLED:
                sessionConfig.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
                break;
            case HORIZONTAL:
                sessionConfig.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
                break;
            case VERTICAL:
                sessionConfig.setPlaneFindingMode(Config.PlaneFindingMode.VERTICAL);
                break;
            case HORIZONTAL_AND_VERTICAL:
                sessionConfig.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
                break;
        }

        //Setup light estimation type
        switch (gdxARConfiguration.lightEstimationMode) {
            case DISABLED:
                sessionConfig.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
                break;
            case AMBIENT_INTENSITY:
                sessionConfig.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
                break;
            case ENVIRONMENTAL_HDR:
                sessionConfig.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
                break;
        }

        getSession().configure(sessionConfig);

        gdxArApplicationListener.create();
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
                planeInstances.clear();

                for (Plane plane : surfaces) {
                    GdxPlane gdxPlane = ARCoreToGdxAR.createGdxPlane(plane);
                    frameInstance.addPlane(gdxPlane);
                    if (gdxARConfiguration.debugMode)
                        addPlane(plane);
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

                if (gdxARConfiguration.enableDepth) {
                    /*Gdx.gl.glDepthMask(true);
                    Gdx.gl.glDepthFunc(GL20.GL_LESS);
                    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
                    Gdx.gl.glEnable(GL20.GL_CULL_FACE);
                    Gdx.gl.glCullFace(GL20.GL_FRONT_AND_BACK);
                    Gdx.gl.glColorMask(false, false, false, false);

                    try {
                        backgroundRenderer.renderDepth(frame);
                    } catch (NotYetAvailableException e) {
                        e.printStackTrace();
                    }

                    Gdx.gl.glColorMask(true, true, true, true);
                    Gdx.gl.glDepthFunc(GL20.GL_EQUAL);*/
                }

                if (gdxARConfiguration.debugMode) {
                    Gdx.gl.glLineWidth(10);
                    debugModelBatch.begin(arCamera);
                    debugModelBatch.render(planeInstances);
                    debugModelBatch.end();
                    Gdx.gl.glLineWidth(1);
                }

                gdxArApplicationListener.renderARModels(frameInstance);
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
        if (renderAR) {
            try {
                getSession().resume();
            } catch (CameraNotAvailableException e) {
                e.printStackTrace();
            }
        }
        gdxArApplicationListener.resume();
    }

    @Override
    public void dispose() {
        gdxArApplicationListener.dispose();
    }

    @Override
    @Null
    public GdxAnchor requestHitPlaneAnchor(float x, float y, GdxPlaneType planeType) {
        if (!renderAR) return null;

        ARCoreGraphics arCoreGraphics = (ARCoreGraphics) Gdx.graphics;
        Frame frame = arCoreGraphics.getCurrentFrame();

        List<HitResult> hitResultList = frame.hitTest(x, y);
        if (hitResultList.size() == 0) return null;

        for (HitResult hit : hitResultList) {
            Trackable trackable = hit.getTrackable();
            Pose pose = hit.getHitPose();
            if (trackable instanceof Plane) {
                Plane plane = (Plane) trackable;
                if (plane.getSubsumedBy() != null
                        || !plane.isPoseInPolygon(pose)
                        || (planeType != GdxPlaneType.ANY && ARCoreToGdxAR.map(plane.getType()) != planeType)
                        || plane.getTrackingState() != TrackingState.TRACKING
                        || plane.getPolygon().capacity() == 0) continue;

                Anchor newAnchor = plane.createAnchor(pose);
                return ARCoreToGdxAR.createGdxAnchor(newAnchor);
            }
        }
        return null;
    }

    @Override
    @Null
    public GdxPose requestHitPlanePose(float x, float y, GdxPlaneType planeType) {
        if (!renderAR) return null;

        ARCoreGraphics arCoreGraphics = (ARCoreGraphics) Gdx.graphics;
        Frame frame = arCoreGraphics.getCurrentFrame();

        List<HitResult> hitResultList = frame.hitTest(x, y);
        if (hitResultList.size() == 0) return null;

        for (HitResult hit : hitResultList) {
            Trackable trackable = hit.getTrackable();
            Pose pose = hit.getHitPose();
            if (trackable instanceof Plane) {
                Plane plane = (Plane) trackable;
                if (plane.getSubsumedBy() != null
                        || !plane.isPoseInPolygon(pose)
                        || (planeType != GdxPlaneType.ANY && ARCoreToGdxAR.map(plane.getType()) != planeType)
                        || plane.getTrackingState() != TrackingState.TRACKING
                        || plane.getPolygon().capacity() == 0) continue;

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
                Plane.Type type = plane.getType();
                TrackingState trackingState = plane.getTrackingState();
                if (type != Plane.Type.HORIZONTAL_DOWNWARD_FACING && trackingState == TrackingState.TRACKING) {
                    gdxArApplicationListener.lookingSurfaces(true);
                    hasSurface = true;
                }
            }
        }
        return hasSurface;
    }

    private void addPlane(Plane plane) {
        // check for planes that are no longer valid
        if (plane.getSubsumedBy() != null
                || plane.getType() == Plane.Type.HORIZONTAL_DOWNWARD_FACING
                || plane.getTrackingState() != TrackingState.TRACKING
                || plane.getPolygon().capacity() == 0) {
            return;
        }

        FloatBuffer polygon = plane.getPolygon();
        ModelInstance m = getPolygonModel(polygon.limit() / 2);
        updateVertices(m, polygon, Color.YELLOW);

        Pose pose = plane.getCenterPose();
        m.transform.set(pose.tx(), pose.ty(), pose.tz(), pose.qx(), pose.qy(), pose.qz(), pose.qw());
        planeInstances.add(m);
    }

    private ModelInstance getPolygonModel(int vertexCount) {
        ModelInstance model = polygonsCache.get(vertexCount);
        if (model != null) return model;

        builder.begin();
        for (int i = 0; i < vertexCount; i++) {
            MeshPartBuilder meshPartBuilder = builder.part("line" + i, GL20.GL_LINES, 3, new Material());
            meshPartBuilder.setColor(Color.WHITE);
            meshPartBuilder.line(0, 0, 0, 0, 0, 0);
        }

        model = new ModelInstance(builder.end());
        polygonsCache.put(vertexCount, model);
        return model;
    }

    private void updateVertices(ModelInstance modelInstance, FloatBuffer vertices, Color color) {
        Node node = modelInstance.nodes.get(0);
        int verticesCount = vertices.limit() / 2;
        for (int i = 0; i < verticesCount; i++) {
            int j = i == 0 ? verticesCount - 1 : i - 1;
            NodePart line = node.parts.get(i);
            MeshPart lineMesh = line.meshPart;
            Mesh mesh = lineMesh.mesh;
            tmpVerts[0] = vertices.get(j * 2);
            tmpVerts[1] = 0.0f;
            tmpVerts[2] = vertices.get((j * 2) + 1);
            tmpVerts[3] = color.r;
            tmpVerts[4] = color.g;
            tmpVerts[5] = color.b;
            tmpVerts[6] = color.a;
            tmpVerts[7] = vertices.get(i * 2);
            tmpVerts[8] = 0.0f;
            tmpVerts[9] = vertices.get((i * 2) + 1);
            tmpVerts[10] = color.r;
            tmpVerts[11] = color.g;
            tmpVerts[12] = color.b;
            tmpVerts[13] = color.a;
            mesh.updateVertices(i * 14, tmpVerts);
        }
    }
}

