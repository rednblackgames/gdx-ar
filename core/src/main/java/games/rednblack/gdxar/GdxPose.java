package games.rednblack.gdxar;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;

/**
 * Wrapper class to Pose object in native AR framework.
 *
 * @author fgnm
 */
public class GdxPose implements Pool.Poolable {

    /** Position in world coordinates */
    private final Vector3 position = new Vector3();

    /** Rotation in world coordinates */
    private final Quaternion rotation = new Quaternion();

    public GdxPose() {

    }

    public void setPosition(float[] pos) {
        position.set(pos[0], pos[1], pos[2]);
    }

    public void setRotation(float[] rot) {
        rotation.set(rot[0], rot[1], rot[2], rot[3]);
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    public void setRotation(float x, float y, float z, float w) {
        rotation.set(x, y, z, w);
    }

    public Vector3 getPosition() {
        return position;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    @Override
    public void reset() {
        position.set(0, 0, 0);
        rotation.set(0, 0, 0, 0);
    }
}
