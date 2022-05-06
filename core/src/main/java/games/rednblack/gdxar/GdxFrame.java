package games.rednblack.gdxar;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.environment.SphericalHarmonics;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

/**
 * Wrapper class to Frame object in native AR framework.
 *
 * @author fgnm
 */
public class GdxFrame implements Pool.Poolable {
    /** Current anchors list tracked by the framework */
    private final Array<GdxAnchor> anchors = new Array<>(GdxAnchor.class);

    /** Current augmented images list tracked by the framework */
    private final GdxAugmentedImages augmentedImages = new GdxAugmentedImages();

    /** Current planes list tracked by the framework */
    private final Array<GdxPlane> planes = new Array<>(GdxPlane.class);

    /** Current status of the light estimation calculated by the framework */
    public GdxLightEstimationMode lightEstimationMode = GdxLightEstimationMode.DISABLED;

    /** Main directional light intensity */
    public float lightIntensity = 0;

    /** Main directional light direction */
    public final Vector3 lightDirection = new Vector3();

    /** Main directional light color */
    public final Color lightColor = new Color();

    /** Ambient light intensity in {@link SphericalHarmonics} format */
    public final SphericalHarmonics sphericalHarmonics = new SphericalHarmonics();

    /** Ambient light intensity as a single float format, when SphericalHarmonics are not supported */
    public float ambientIntensity = 0;

    public void addAnchor(GdxAnchor anchor) {
        anchors.add(anchor);
    }

    public void addPlane(GdxPlane plane) {
        planes.add(plane);
    }

    public void addAugmentedImage(GdxAugmentedImage augmentedImage) {
        augmentedImages.add(augmentedImage);
    }

    /**
     * Get current anchors list tracked by the framework.
     * @return Array with anchors in updated state
     */
    public Array<GdxAnchor> getAnchors() {
        return anchors;
    }

    /**
     * Get current planes list tracked by the framework.
     * @return Array with planes in updated state
     */
    public Array<GdxPlane> getPlanes() {
        return planes;
    }

    /**
     * Current augmented images tracked by the framework.
     * @return Array with augmented images in updated state
     */
    public GdxAugmentedImages getAugmentedImages() {
        return augmentedImages;
    }

    @Override
    public void reset() {
        Pools.freeAll(anchors);
        anchors.clear();

        Pools.freeAll(planes);
        planes.clear();

        Pools.freeAll(augmentedImages);
        augmentedImages.clear();
    }
}
