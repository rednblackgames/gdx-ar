package games.rednblack.gdxar.android.util;

import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.google.ar.core.*;

import games.rednblack.gdxar.*;

import java.nio.FloatBuffer;

/**
 * Utility class that convert ARCore classes into GdxAR
 *
 * @author fgnm
 */
public class ARCoreToGdxAR {

    // Allocate temporary storage to avoid multiple allocations per frame.
    private static final float[] tmpPoseTranslation = new float[3];
    private static final float[] tmpPoseRotation = new float[4];

    public static GdxAnchor createGdxAnchor(Anchor anchor) {
        GdxAnchor gdxAnchor = Pools.obtain(GdxAnchor.class);
        Pose pose = anchor.getPose();
        map(pose, gdxAnchor.gdxPose);
        gdxAnchor.trackingState = map(anchor.getTrackingState());
        try {
            Field field = ClassReflection.getDeclaredField(Anchor.class, "nativeHandle");
            field.setAccessible(true);
            gdxAnchor.id = (Long) field.get(anchor);
        } catch (ReflectionException e) {
            gdxAnchor.id = anchor.hashCode();
        }
        return gdxAnchor;
    }

    public static GdxAugmentedImage createGdxAugmentedImage(AugmentedImage img) {
        GdxAugmentedImage augmentedImage = Pools.obtain(GdxAugmentedImage.class);
        Pose pose = img.getCenterPose();
        ARCoreToGdxAR.map(pose, augmentedImage.gdxPose);
        augmentedImage.trackingState = ARCoreToGdxAR.map(img.getTrackingState());
        augmentedImage.trackingMethod = ARCoreToGdxAR.map(img.getTrackingMethod());
        augmentedImage.index = img.getIndex();
        augmentedImage.extentX = img.getExtentX();
        augmentedImage.extentZ = img.getExtentZ();
        augmentedImage.name = img.getName();

        return augmentedImage;
    }

    public static GdxPlane createGdxPlane(Plane plane, boolean enableGeometry) {
        GdxPlane gdxPlane = Pools.obtain(GdxPlane.class);
        Pose pose = plane.getCenterPose();
        ARCoreToGdxAR.map(pose, gdxPlane.gdxPose);
        gdxPlane.trackingState = ARCoreToGdxAR.map(plane.getTrackingState());
        gdxPlane.extentX = plane.getExtentX();
        gdxPlane.extentZ = plane.getExtentZ();
        gdxPlane.type = ARCoreToGdxAR.map(plane.getType());
        if (enableGeometry) {
            FloatBuffer polygon = plane.getPolygon();
            gdxPlane.vertices.ensureCapacity(polygon.limit());
            for (int i = 0; i < polygon.limit(); i++) {
                gdxPlane.vertices.add(polygon.get(i));
            }
        }
        return gdxPlane;
    }

    public static void map(Pose pose, GdxPose gdxPose) {
        pose.getTranslation(tmpPoseTranslation, 0);
        pose.getRotationQuaternion(tmpPoseRotation, 0);
        gdxPose.setPosition(tmpPoseTranslation);
        gdxPose.setRotation(tmpPoseRotation);
    }

    public static GdxTrackingState map(TrackingState trackingState) {
        switch (trackingState) {
            case PAUSED:
                return GdxTrackingState.PAUSED;
            case TRACKING:
                return GdxTrackingState.TRACKING;
            default:
                return GdxTrackingState.STOPPED;
        }
    }

    public static GdxTrackingMethod map(AugmentedImage.TrackingMethod trackingState) {
        switch (trackingState) {
            case FULL_TRACKING:
                return GdxTrackingMethod.FULL_TRACKING;
            case LAST_KNOWN_POSE:
                return GdxTrackingMethod.LAST_KNOWN_POSE;
            default:
                return GdxTrackingMethod.NOT_TRACKING;
        }
    }

    public static GdxPlaneType map(Plane.Type type) {
        switch (type) {
            case VERTICAL:
                return GdxPlaneType.VERTICAL;
            case HORIZONTAL_DOWNWARD_FACING:
                return GdxPlaneType.HORIZONTAL_DOWNWARD_FACING;
            case HORIZONTAL_UPWARD_FACING:
                return GdxPlaneType.HORIZONTAL_UPWARD_FACING;
            default:
                return null;
        }
    }

    public static GdxLightEstimationMode map(Config.LightEstimationMode lightEstimationMode) {
        switch (lightEstimationMode) {
            case AMBIENT_INTENSITY:
                return GdxLightEstimationMode.AMBIENT_INTENSITY;
            case ENVIRONMENTAL_HDR:
                return GdxLightEstimationMode.ENVIRONMENTAL_HDR;
            default:
                return GdxLightEstimationMode.DISABLED;
        }
    }
}
