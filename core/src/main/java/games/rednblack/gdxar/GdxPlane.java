package games.rednblack.gdxar;

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

    @Override
    public void reset() {
        super.reset();
        extentX = 0;
        extentZ = 0;
        type = null;
    }
}
