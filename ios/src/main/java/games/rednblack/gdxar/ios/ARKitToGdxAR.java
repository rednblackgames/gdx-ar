package games.rednblack.gdxar.ios;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Pools;

import org.robovm.apple.arkit.ARAnchor;
import org.robovm.apple.arkit.ARImageAnchor;
import org.robovm.apple.arkit.ARPlaneAnchor;
import org.robovm.apple.arkit.ARPlaneAnchorAlignment;
import org.robovm.apple.arkit.ARPlaneGeometry;
import org.robovm.apple.foundation.MatrixFloat4x4;
import org.robovm.apple.foundation.VectorFloat3;
import org.robovm.apple.foundation.VectorFloat4;
import org.robovm.apple.scenekit.SCNNode;
import org.robovm.apple.scenekit.SCNVector3;
import org.robovm.apple.scenekit.SCNVector4;

import games.rednblack.gdxar.GdxAnchor;
import games.rednblack.gdxar.GdxAugmentedImage;
import games.rednblack.gdxar.GdxPlane;
import games.rednblack.gdxar.GdxPlaneType;
import games.rednblack.gdxar.GdxPose;
import games.rednblack.gdxar.GdxTrackingMethod;
import games.rednblack.gdxar.GdxTrackingState;

public class ARKitToGdxAR {

    private static final float[] tmpMatrix = new float[16];
    private static final SCNNode tmpNode = new SCNNode();

    public static void map(MatrixFloat4x4 matrix, Matrix4 gdxMatrix) {
        VectorFloat4 c1 = matrix.getC1();
        tmpMatrix[0] = c1.getX();
        tmpMatrix[1] = c1.getY();
        tmpMatrix[2] = c1.getZ();
        tmpMatrix[3] = c1.getW();
        VectorFloat4 c2 = matrix.getC2();
        tmpMatrix[4] = c2.getX();
        tmpMatrix[5] = c2.getY();
        tmpMatrix[6] = c2.getZ();
        tmpMatrix[7] = c2.getW();
        VectorFloat4 c3 = matrix.getC3();
        tmpMatrix[8] = c3.getX();
        tmpMatrix[9] = c3.getY();
        tmpMatrix[10] = c3.getZ();
        tmpMatrix[11] = c3.getW();
        VectorFloat4 c4 = matrix.getC4();
        tmpMatrix[12] = c4.getX();
        tmpMatrix[13] = c4.getY();
        tmpMatrix[14] = c4.getZ();
        tmpMatrix[15] = c4.getW();
        gdxMatrix.set(tmpMatrix);
    }

    private static final double[] redpoly = {4.93596077e0, -1.29917429e0,
            1.64810386e-01, -1.16449912e-02,
            4.86540872e-04, -1.19453511e-05,
            1.59255189e-07, -8.89357601e-10};

    private static final double[] greenpoly1 = {-4.95931720e-01, 1.08442658e0,
            -9.17444217e-01, 4.94501179e-01,
            -1.48487675e-01, 2.49910386e-02,
            -2.21528530e-03, 8.06118266e-05};

    private static final double[] greenpoly2 = {3.06119745e0, -6.76337896e-01,
            8.28276286e-02, -5.72828699e-03,
            2.35931130e-04, -5.73391101e-06,
            7.58711054e-08, -4.21266737e-10};

    private static final double[] bluepoly = {4.93997706e-01, -8.59349314e-01,
            5.45514949e-01, -1.81694167e-01,
            4.16704799e-02, -6.01602324e-03,
            4.80731598e-04, -1.61366693e-05};

    public static void map(double temperature, Color dst) {
        // Used this: https://gist.github.com/paulkaplan/5184275 at the beginning
        // based on http://stackoverflow.com/questions/7229895/display-temperature-as-a-color-with-c
        // this answer: http://stackoverflow.com/a/24856307
        // (so, just interpretation of pseudocode in Java)

        double x = temperature / 1000.0;
        if (x > 40) {
            x = 40;
        }
        double red;
        double green;
        double blue;

        // R
        if (temperature < 6527) {
            red = 1;
        } else {
            red = poly(redpoly, x);
        }
        // G
        if (temperature < 850) {
            green = 0;
        } else if (temperature <= 6600) {
            green = poly(greenpoly1, x);
        } else {
            green = poly(greenpoly2, x);
        }
        // B
        if (temperature < 1900) {
            blue = 0;
        } else if (temperature < 6600) {
            blue = poly(bluepoly, x);
        } else {
            blue = 1;
        }

        red = MathUtils.clamp(red, 0, 1) * 255;
        blue = MathUtils.clamp(blue, 0, 1) * 255;
        green = MathUtils.clamp(green, 0, 1) * 255;
        dst.set((float) red, (float) green, (float) blue, 1);
    }

    public static double poly(double[] coefficients, double x) {
        double result = coefficients[0];
        double xn = x;
        for (int i = 1; i < coefficients.length; i++) {
            result += xn * coefficients[i];
            xn *= x;
        }
        return result;
    }

    public static void map(MatrixFloat4x4 transform, GdxPose gdxPose) {
        tmpNode.setSimdWorldTransform(transform);
        SCNVector3 vector3 = tmpNode.getPosition();
        gdxPose.setPosition(vector3.getX(), vector3.getY(), vector3.getZ());
        SCNVector4 vector4 = tmpNode.getOrientation();
        gdxPose.setRotation(vector4.getX(), vector4.getY(), vector4.getZ(), vector4.getW());
    }

    public static GdxPlaneType map(ARPlaneAnchorAlignment type) {
        switch (type) {
            case Vertical:
                return GdxPlaneType.VERTICAL;
            case Horizontal:
                return GdxPlaneType.HORIZONTAL_UPWARD_FACING;
            default:
                return null;
        }
    }

    public static GdxPlane createGdxPlane(ARPlaneAnchor plane, boolean enableSurfaceGeometry) {
        GdxPlane gdxPlane = Pools.obtain(GdxPlane.class);
        ARKitToGdxAR.map(plane.getTransform(), gdxPlane.gdxPose);
        gdxPlane.trackingState = GdxTrackingState.TRACKING;
        VectorFloat3 extend = plane.getExtent();
        gdxPlane.extentX = extend.getX();
        gdxPlane.extentZ = extend.getZ();
        gdxPlane.type = ARKitToGdxAR.map(plane.getAlignment());

        if (enableSurfaceGeometry) {
            ARPlaneGeometry geometry = plane.getGeometry();
            int boundaryCount = (int) geometry.getBoundaryVertexCount();
            gdxPlane.vertices.ensureCapacity(boundaryCount * 2);
            VectorFloat3[] vv = geometry.getBoundaryVertices().toArray(boundaryCount);
            for (int i = 0; i < vv.length; i++) {
                gdxPlane.vertices.add(vv[i].getX(), vv[i].getZ());
            }
            geometry.dispose();
        }
        return gdxPlane;
    }

    public static GdxAugmentedImage createGdxAugmentedImage(ARImageAnchor img) {
        GdxAugmentedImage augmentedImage = Pools.obtain(GdxAugmentedImage.class);
        ARKitToGdxAR.map(img.getTransform(), augmentedImage.gdxPose);
        boolean isTracked = img.isTracked();
        augmentedImage.trackingState = GdxTrackingState.TRACKING;
        augmentedImage.trackingMethod = isTracked ? GdxTrackingMethod.FULL_TRACKING : GdxTrackingMethod.LAST_KNOWN_POSE;
        augmentedImage.name = img.getName();

        return augmentedImage;
    }

    public static GdxAnchor createGdxAnchor(ARAnchor anchor) {
        GdxAnchor gdxAnchor = Pools.obtain(GdxAnchor.class);
        map(anchor.getTransform(), gdxAnchor.gdxPose);
        gdxAnchor.trackingState = GdxTrackingState.TRACKING;
        gdxAnchor.id = anchor.getIdentifier().hashCode();
        return gdxAnchor;
    }
}
