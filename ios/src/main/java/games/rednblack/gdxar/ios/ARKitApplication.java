package games.rednblack.gdxar.ios;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Pools;

import org.robovm.apple.arkit.*;
import org.robovm.apple.avfoundation.AVCaptureDevice;
import org.robovm.apple.avfoundation.AVMediaType;
import org.robovm.apple.coregraphics.CGColorRenderingIntent;
import org.robovm.apple.coregraphics.CGDataProvider;
import org.robovm.apple.coregraphics.CGImage;
import org.robovm.apple.coregraphics.CGPoint;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coremedia.CMSampleBuffer;
import org.robovm.apple.foundation.MatrixFloat4x4;
import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.foundation.NSData;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSMutableSet;
import org.robovm.apple.foundation.NSSet;
import org.robovm.apple.foundation.VectorFloat3;
import org.robovm.apple.imageio.CGImagePropertyOrientation;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UIView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import games.rednblack.gdxar.GdxAR;
import games.rednblack.gdxar.GdxARConfiguration;
import games.rednblack.gdxar.GdxAnchor;
import games.rednblack.gdxar.GdxArApplicationListener;
import games.rednblack.gdxar.GdxFrame;
import games.rednblack.gdxar.GdxLightEstimationMode;
import games.rednblack.gdxar.GdxPlaneType;
import games.rednblack.gdxar.GdxPose;
import games.rednblack.gdxar.util.DebugShaderProvider;
import games.rednblack.gdxar.util.RawAugmentedImageAsset;

/**
 * ARKitApplication is the base class for the scene to render. Application specific code should be
 * put in {@link GdxArApplicationListener}
 *
 * This class handles the basic boilerplate of rendering the background image, moving the camera
 * based on the ARKit frame pose, and basic batch rendering.
 *
 * @author fgnm
 */
public class ARKitApplication implements ApplicationListener, GdxAR, ARSessionDelegate, ARCoachingOverlayViewDelegate {
    protected GdxArApplicationListener gdxArApplicationListener;
    protected GdxARConfiguration gdxARConfiguration;
    protected IOSApplication iosApplication;

    protected final GdxFrame frameInstance;

    protected boolean hasSurface = false;
    protected boolean renderAR = false;
    protected boolean enableSurfaceGeometry = false;

    private PerspectiveCamera arCamera;

    private ARSession session;
    private ARWorldTrackingConfiguration sessionConfig;
    private ARCoachingOverlayView coachingOverlay = null;

    private BackgroundRenderer backgroundRenderer;
    private ModelBatch debugModelBatch;

    private final CGPoint hitPoint = new CGPoint();

    private final Array<ModelInstance> planeInstances = new Array<>();
    private final IntMap<ModelInstance> polygonsCache = new IntMap<>();
    private final ModelBuilder builder = new ModelBuilder();
    private final float[] tmpVerts = new float[14];

    public ARKitApplication(GdxArApplicationListener gdxArApplicationListener, GdxARConfiguration gdxARConfiguration) {
        this.gdxArApplicationListener = gdxArApplicationListener;
        this.gdxArApplicationListener.setArAPI(this);
        this.gdxARConfiguration = new GdxARConfiguration(gdxARConfiguration);

        frameInstance = new GdxFrame();
    }

    public void setIosApplication(IOSApplication iosApplication) {
        this.iosApplication = iosApplication;
    }

    @Override
    public void create() {
        debugModelBatch = new ModelBatch(new DebugShaderProvider());
        String version = Gdx.graphics.getGL20().glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        System.out.println("Shaders version " + version);

        arCamera = new PerspectiveCamera(67, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        arCamera.position.set(0, 1.6f, 0f);
        arCamera.lookAt(0, 0, 1f);
        arCamera.near = .01f;
        arCamera.far = 30f;
        arCamera.update();

        backgroundRenderer = new BackgroundRenderer();

        session = new ARSession();
        session.setDelegate(this);
        sessionConfig = new ARWorldTrackingConfiguration();

        if (iosApplication != null) {
            coachingOverlay = new ARCoachingOverlayView();
            UIView uiView = iosApplication.getUIWindow();
            coachingOverlay.setUserInteractionEnabled(false);
            uiView.addSubview(coachingOverlay);
            coachingOverlay.setFrame(new CGRect(uiView.getFrame().getX(), uiView.getFrame().getY(), uiView.getFrame().getWidth(), uiView.getFrame().getHeight()));

            coachingOverlay.setSession(session);
            coachingOverlay.setDelegate(this);
        }

        //Setup Plane Finding Mode
        switch (gdxARConfiguration.planeFindingMode) {
            case DISABLED:
                sessionConfig.setPlaneDetection(ARPlaneDetection.None);
                break;
            case HORIZONTAL:
                sessionConfig.setPlaneDetection(ARPlaneDetection.Horizontal);
                if (coachingOverlay != null)
                    coachingOverlay.setGoal(ARCoachingGoal.HorizontalPlane);
                break;
            case VERTICAL:
                sessionConfig.setPlaneDetection(ARPlaneDetection.Vertical);
                if (coachingOverlay != null)
                    coachingOverlay.setGoal(ARCoachingGoal.HorizontalPlane);
                break;
            case HORIZONTAL_AND_VERTICAL:
                sessionConfig.setPlaneDetection(ARPlaneDetection.with(ARPlaneDetection.Horizontal, ARPlaneDetection.Vertical));
                if (coachingOverlay != null)
                    coachingOverlay.setGoal(ARCoachingGoal.AnyPlane);
                break;
        }

        //Setup light estimation type
        switch (gdxARConfiguration.lightEstimationMode) {
            case DISABLED:
                sessionConfig.setLightEstimationEnabled(false);
                break;
            case AMBIENT_INTENSITY:
                sessionConfig.setLightEstimationEnabled(true);
                break;
            case ENVIRONMENTAL_HDR:
                throw new IllegalArgumentException("ENVIRONMENTAL_HDR is not supported by ARKit");
        }

        gdxArApplicationListener.create();
    }

    @Override
    public void resize(int width, int height) {
        arCamera.viewportWidth = Gdx.graphics.getBackBufferWidth();
        arCamera.viewportHeight = Gdx.graphics.getBackBufferHeight();

        if (iosApplication != null) {
            UIView uiView = iosApplication.getUIWindow();
            coachingOverlay.setFrame(new CGRect(uiView.getFrame().getX(), uiView.getFrame().getY(), uiView.getFrame().getWidth(), uiView.getFrame().getHeight()));
        }
        gdxArApplicationListener.resize(width, height);
    }

    @Override
    public void render() {
        if (renderAR) {
            gdxArApplicationListener.arPipelineBegin();

            Gdx.gl.glClearColor(0,0,0,0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            backgroundRenderer.render();

            if (gdxARConfiguration.debugMode) {
                Gdx.gl.glLineWidth(10);
                debugModelBatch.begin(arCamera);
                debugModelBatch.render(planeInstances);
                debugModelBatch.end();
                Gdx.gl.glLineWidth(1);
            }

            gdxArApplicationListener.renderARModels(frameInstance);

            gdxArApplicationListener.arPipelineEnd();
        }

        gdxArApplicationListener.render();
    }

    @Override
    public void pause() {
        if (renderAR)
            session.pause();
        gdxArApplicationListener.pause();
    }

    @Override
    public void resume() {
        if (renderAR) {
            try {
                session.run(sessionConfig);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        gdxArApplicationListener.resume();
    }

    @Override
    public void dispose() {
        backgroundRenderer.dispose();
        gdxArApplicationListener.dispose();
    }

    @Override
    public GdxAnchor requestHitPlaneAnchor(float x, float y, GdxPlaneType planeType) {
        if (!renderAR) return null;
        ARRaycastTargetAlignment alignment;
        switch (planeType) {
            case HORIZONTAL_DOWNWARD_FACING:
            case HORIZONTAL_UPWARD_FACING:
                alignment = ARRaycastTargetAlignment.Horizontal;
                break;
            case VERTICAL:
                alignment = ARRaycastTargetAlignment.Vertical;
                break;
            default:
                alignment = ARRaycastTargetAlignment.Any;
        }

        hitPoint.setY(1f - x / Gdx.graphics.getWidth());
        hitPoint.setX(y / Gdx.graphics.getHeight());
        ARFrame currentFrame = session.getCurrentFrame();
        ARRaycastQuery raycastQuery = currentFrame
                .raycastQueryFromPoint(hitPoint, ARRaycastTarget.ExistingPlaneGeometry, alignment);
        if (raycastQuery == null) {
            currentFrame.dispose();
            return null;
        }

        NSArray<ARRaycastResult> results = session.raycast(raycastQuery);
        if (results.size() == 0) {
            raycastQuery.dispose();
            currentFrame.dispose();
            return null;
        }

        ARRaycastResult hitTestResult = results.last();
        MatrixFloat4x4 worldTransform = hitTestResult.getWorldTransform();
        ARAnchor anchor = new ARAnchor(worldTransform);
        session.addAnchor(anchor);
        GdxAnchor gdxAnchor = ARKitToGdxAR.createGdxAnchor(anchor);
        hitTestResult.dispose();
        anchor.dispose();
        raycastQuery.dispose();
        currentFrame.dispose();
        return gdxAnchor;
    }

    @Override
    public GdxAnchor createGeospatialAnchor(double latitude, double longitude, double altitude, Quaternion rotation) {
        return null;
    }

    @Override
    public GdxPose requestHitPlanePose(float x, float y, GdxPlaneType planeType) {
        if (!renderAR) return null;
        ARRaycastTargetAlignment alignment;
        switch (planeType) {
            case HORIZONTAL_DOWNWARD_FACING:
            case HORIZONTAL_UPWARD_FACING:
                alignment = ARRaycastTargetAlignment.Horizontal;
                break;
            case VERTICAL:
                alignment = ARRaycastTargetAlignment.Vertical;
                break;
            default:
                alignment = ARRaycastTargetAlignment.Any;
        }

        hitPoint.setY(1f - x / Gdx.graphics.getWidth());
        hitPoint.setX(y / Gdx.graphics.getHeight());
        ARFrame currentFrame = session.getCurrentFrame();
        ARRaycastQuery raycastQuery = currentFrame
                .raycastQueryFromPoint(hitPoint, ARRaycastTarget.ExistingPlaneGeometry, alignment);
        if (raycastQuery == null) {
            currentFrame.dispose();
            return null;
        }

        NSArray<ARRaycastResult> results = session.raycast(raycastQuery);
        if (results.size() == 0) {
            raycastQuery.dispose();
            currentFrame.dispose();
            return null;
        }

        ARRaycastResult hitTestResult = results.last();
        GdxPose gdxPose = Pools.obtain(GdxPose.class);
        ARKitToGdxAR.map(hitTestResult.getWorldTransform(), gdxPose);
        raycastQuery.dispose();
        hitTestResult.dispose();
        currentFrame.dispose();
        return gdxPose;
    }

    @Override
    public void setAutofocus(boolean autofocus) {
        if (sessionConfig.isAutoFocusEnabled() == autofocus) return;
        sessionConfig.setAutoFocusEnabled(autofocus);
        session.run(sessionConfig, ARSessionRunOptions.None);
    }

    @Override
    public PerspectiveCamera getARCamera() {
        return arCamera;
    }

    @Override
    public void setRenderAR(boolean renderAR) {
        if (this.renderAR == renderAR) return;

        if (renderAR) {
            try {
                session.run(sessionConfig);
                this.renderAR = true;
            } catch (Exception e) {
                this.renderAR = false;
                e.printStackTrace();
            }
        } else {
            session.pause();
            this.renderAR = false;
        }
    }

    @Override
    public boolean isRenderingAR() {
        return renderAR;
    }

    @Override
    public boolean isARAllowed() {
        switch (AVCaptureDevice.getAuthorizationStatusForMediaType(AVMediaType.Video)) {
            case Restricted:
            case Denied:
                return false;
        }
        return true;
    }

    @Override
    public boolean loadAugmentedImageDatabase(InputStream databaseStream) {
        return false;
    }

    @Override
    public IntMap<String> buildAugmentedImageDatabase(Array<RawAugmentedImageAsset> images) {
        IntMap<String> map = new IntMap<>();
        int index = 0;
        NSSet<ARReferenceImage> detectionImages = new NSMutableSet<>();
        for (RawAugmentedImageAsset image : images) {
            ByteBuffer byteBuffer = readAllBytes(image.inputStream);
            NSData data = new NSData(byteBuffer);
            CGDataProvider cgDataProvider = CGDataProvider.create(data);
            CGImage cgImage = CGImage.createWithPNGDataProvider(cgDataProvider, false, CGColorRenderingIntent.Default);
            ARReferenceImage arReferenceImage = new ARReferenceImage(cgImage, CGImagePropertyOrientation.Up, image.widthInMeter);
            arReferenceImage.setName(image.name);
            detectionImages.add(arReferenceImage);
            map.put(index, image.name);
            index++;
            data.dispose();
            cgDataProvider.dispose();
            cgImage.dispose();
            BufferUtils.disposeUnsafeByteBuffer(byteBuffer);
        }
        sessionConfig.setDetectionImages(detectionImages);
        return map;
    }

    public static ByteBuffer readAllBytes(InputStream inputStream) {
        ByteBuffer byteBuffer = null;
        try {
            byteBuffer = BufferUtils.newUnsafeByteBuffer(inputStream.available());
            Channels.newChannel(inputStream).read(byteBuffer);
            ((Buffer) byteBuffer).position(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteBuffer;
    }

    @Override
    public void didUpdateFrame(ARSession session, ARFrame frame) {
        UIInterfaceOrientation currentOrientation = iosApplication != null ? iosApplication.getUIWindow().getWindowScene().getInterfaceOrientation() : UIInterfaceOrientation.Portrait;

        backgroundRenderer.update(frame, currentOrientation);

        ARCamera camera = frame.getCamera();
        MatrixFloat4x4 projectionMatrix = camera
                .getProjectionMatrix(currentOrientation,
                        backgroundRenderer.getViewportSize(), arCamera.near, arCamera.far);
        ARKitToGdxAR.map(projectionMatrix, arCamera.projection);

        MatrixFloat4x4 viewMatrix = camera.viewMatrixForOrientation(currentOrientation);
        ARKitToGdxAR.map(viewMatrix, arCamera.view);
        camera.dispose();

        arCamera.combined.set(arCamera.projection);
        Matrix4.mul(arCamera.combined.val, arCamera.view.val);

        frameInstance.reset();
        planeInstances.clear();
        NSArray<ARAnchor> anchors = frame.getAnchors();
        for (ARAnchor anchor : anchors) {
            if (anchor instanceof ARPlaneAnchor) {
                ARPlaneAnchor plane = anchor.as(ARPlaneAnchor.class);
                if (gdxARConfiguration.debugMode)
                    addPlane(plane);
                frameInstance.addPlane(ARKitToGdxAR.createGdxPlane(plane, enableSurfaceGeometry));
            } else if (anchor instanceof ARImageAnchor) {
                ARImageAnchor image = anchor.as(ARImageAnchor.class);
                frameInstance.addAugmentedImage(ARKitToGdxAR.createGdxAugmentedImage(image));
            } else {
                frameInstance.addAnchor(ARKitToGdxAR.createGdxAnchor(anchor));
            }
            anchor.dispose();
        }

        frameInstance.lightEstimationMode = gdxARConfiguration.lightEstimationMode;
        if (frameInstance.lightEstimationMode == GdxLightEstimationMode.AMBIENT_INTENSITY) {
            ARLightEstimate lightEstimate = frame.getLightEstimate();
            frameInstance.ambientIntensity = (float) lightEstimate.getAmbientIntensity();
            ARKitToGdxAR.map(lightEstimate.getAmbientColorTemperature(), frameInstance.lightColor);
            lightEstimate.dispose();
        }

        frame.dispose();
    }

    @Override
    public void didAddAnchors(ARSession session, NSArray<ARAnchor> anchors) {
    }

    @Override
    public void didUpdateAnchors(ARSession session, NSArray<ARAnchor> anchors) {

    }

    @Override
    public void didRemoveAnchors(ARSession session, NSArray<ARAnchor> anchors) {

    }

    @Override
    public void didFailWithError(ARSession session, NSError error) {

    }

    @Override
    public void cameraDidChangeTrackingState(ARSession session, ARCamera camera) {
    }

    @Override
    public void sessionWasInterrupted(ARSession session) {

    }

    @Override
    public void sessionInterruptionEnded(ARSession session) {

    }

    @Override
    public boolean sessionShouldAttemptRelocalization(ARSession session) {
        return false;
    }

    @Override
    public void didOutputAudioSampleBuffer(ARSession session, CMSampleBuffer audioSampleBuffer) {

    }

    @Override
    public void didOutputCollaborationData(ARSession session, ARCollaborationData data) {

    }

    @Override
    public void didChangeGeoTrackingStatus(ARSession session, ARGeoTrackingStatus geoTrackingStatus) {

    }

    private void addPlane(ARPlaneAnchor plane) {
        ARPlaneGeometry geometry = plane.getGeometry();
        // check for planes that are no longer valid
        if (geometry.getVertexCount() == 0) {
            geometry.dispose();
            return;
        }

        int boundaryCount = (int) geometry.getBoundaryVertexCount();
        VectorFloat3[] vv = geometry.getBoundaryVertices().toArray(boundaryCount);
        ModelInstance m = getPolygonModel(boundaryCount);
        updateVertices(m, vv, Color.YELLOW);

        GdxPose gdxPose = Pools.obtain(GdxPose.class);
        ARKitToGdxAR.map(plane.getTransform(), gdxPose);
        m.transform.set(gdxPose.getPosition(), gdxPose.getRotation());
        planeInstances.add(m);
        Pools.free(gdxPose);
        geometry.dispose();
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

    private void updateVertices(ModelInstance modelInstance, VectorFloat3[] vertices, Color color) {
        Node node = modelInstance.nodes.get(0);
        for (int i = 0; i < vertices.length; i++) {
            int j = i == 0 ? vertices.length - 1 : i - 1;
            NodePart line = node.parts.get(i);
            MeshPart lineMesh = line.meshPart;
            Mesh mesh = lineMesh.mesh;
            tmpVerts[0] = vertices[j].getX();
            tmpVerts[1] = vertices[j].getY();
            tmpVerts[2] = vertices[j].getZ();
            tmpVerts[3] = color.r;
            tmpVerts[4] = color.g;
            tmpVerts[5] = color.b;
            tmpVerts[6] = color.a;
            tmpVerts[7] = vertices[i].getX();
            tmpVerts[8] = vertices[i].getY();
            tmpVerts[9] = vertices[i].getZ();
            tmpVerts[10] = color.r;
            tmpVerts[11] = color.g;
            tmpVerts[12] = color.b;
            tmpVerts[13] = color.a;
            mesh.updateVertices(i * 14, tmpVerts);
        }
    }

    @Override
    public void coachingOverlayViewDidRequestSessionReset(ARCoachingOverlayView coachingOverlayView) {

    }

    @Override
    public void coachingOverlayViewWillActivate(ARCoachingOverlayView coachingOverlayView) {
        gdxArApplicationListener.lookingSurfaces(false);
    }

    @Override
    public void coachingOverlayViewDidDeactivate(ARCoachingOverlayView coachingOverlayView) {
        gdxArApplicationListener.lookingSurfaces(true);
    }

    @Override
    public void enableSurfaceGeometry(boolean geometryEnabled) {
        enableSurfaceGeometry = geometryEnabled;
    }

    @Override
    public void setPowerSaveMode(boolean powerSaveMode) {

    }

    @Override
    public GdxLightEstimationMode getLightEstimationMode() {
        return gdxARConfiguration.lightEstimationMode;
    }
}
