package games.rednblack.gdxar;

import com.badlogic.gdx.utils.Pool;

/**
 * An Anchor is a point in the AR space tracked during the camera movement.
 *
 * @author fgnm
 */
public class GdxAnchor implements Pool.Poolable {
    /** Current position in AR world of this Anchor */
    public GdxPose gdxPose = new GdxPose();

    /** Current trcking state of the Anchor */
    public GdxTrackingState trackingState = GdxTrackingState.STOPPED;

    /** Native id assigned by AR framework */
    public long id = -1;

    @Override
    public void reset() {
        trackingState = GdxTrackingState.STOPPED;
        gdxPose.reset();
        id = -1;
    }
}
