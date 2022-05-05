package games.rednblack.gdxar.util;

import com.badlogic.gdx.utils.Pool;

import java.io.InputStream;

/**
 * Wrapper class to a raw augmented image asset, used to load {@link games.rednblack.gdxar.GdxAugmentedImage} at runtime.
 *
 * @author fgnm
 */
public class RawAugmentedImageAsset implements Pool.Poolable {
    public String name;
    public InputStream inputStream;
    public float widthInMeter = 1f;

    @Override
    public void reset() {
        name = null;
        inputStream = null;
        widthInMeter = 1f;
    }
}
