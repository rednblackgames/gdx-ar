package games.rednblack.gdxar;

import com.badlogic.gdx.utils.FloatArray;

/**
 * Wrapper class to Plane object in native AR framework.
 *
 * @author fgnm
 */
public class GdxPlane extends GdxAnchor {
    /** Plane type */
    public GdxPlaneType type;

    /** Dimension in X direction */
    public float extentX = 0;

    /** Dimension in Z direction */
    public float extentZ = 0;

    /** The 2D vertices of a convex polygon approximating the detected plane, in the form [x1, z1, x2, z2, ... ]
     * These X-Z values are in the plane's local x-z plane (y=0) and must be transformed by the pose */
    public FloatArray vertices = new FloatArray();

    @Override
    public void reset() {
        super.reset();
        extentX = 0;
        extentZ = 0;
        type = null;
        vertices.clear();
    }
}
