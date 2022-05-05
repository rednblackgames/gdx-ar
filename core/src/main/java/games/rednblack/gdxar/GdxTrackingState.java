package games.rednblack.gdxar;

/**
 * Indicates whether an {@link GdxAnchor} is being tracked using the camera image, or is being tracked based
 * on its last known pose.
 *
 * @author fgnm
 */
public enum GdxTrackingState {
    /** The Anchor is currently tracked */
    TRACKING,
    /** The Anchor is not currently tracked, the state may be resumed in future */
    PAUSED,
    /** The Anchor is removed from the scene and not currently tracked
     * it will not be resumed in future */
    STOPPED
}
