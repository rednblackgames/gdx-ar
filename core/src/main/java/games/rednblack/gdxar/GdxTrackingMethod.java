package games.rednblack.gdxar;

/**
 * Indicates whether an {@link GdxAugmentedImage} is being tracked using the camera image, or is being tracked based
 * on its last known pose.
 *
 * @author fgnm
 */
public enum GdxTrackingMethod {
    /** The image is not being tracked */
    NOT_TRACKING,
    /** The image is fully tracked by the framework using camera image */
    FULL_TRACKING,
    /** The image is tracked by the last known position, but it's not based on camera image */
    LAST_KNOWN_POSE
}
