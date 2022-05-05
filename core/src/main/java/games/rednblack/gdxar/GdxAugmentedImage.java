package games.rednblack.gdxar;

import java.util.Objects;

/**
 * Wrapper class to Augmented Image object in native AR framework.
 *
 * @author fgnm
 */
public class GdxAugmentedImage extends GdxAnchor {
    /** Current state of the tracking method */
    public GdxTrackingMethod trackingMethod = GdxTrackingMethod.NOT_TRACKING;

    /** Index position in the augmented image database */
    public int index = -1;

    /** Dimension in X direction */
    public float extentX = 0;

    /** Dimension in Z direction */
    public float extentZ = 0;

    /** Name in the augmented image database */
    public String name;

    @Override
    public void reset() {
        super.reset();
        trackingMethod = GdxTrackingMethod.NOT_TRACKING;
        index = -1;
        extentX = 0;
        extentZ = 0;
        name = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GdxAugmentedImage that = (GdxAugmentedImage) o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }
}
