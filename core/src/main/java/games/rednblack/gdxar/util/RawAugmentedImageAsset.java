package games.rednblack.gdxar.util;

import com.badlogic.gdx.utils.Pool;

import java.io.InputStream;

/**
 * Wrapper class to a raw augmented image asset, used to load {@link games.rednblack.gdxar.GdxAugmentedImage} at runtime.
 *
 * @author fgnm
 */
public class RawAugmentedImageAsset implements Pool.Poolable {
    /** Tag name of the image */
    public String name;
    /** Input stream where to read the image data */
    public InputStream inputStream;
    /** Width of the image in real world space */
    public float widthInMeter = 1f;

    @Override
    public void reset() {
        name = null;
        inputStream = null;
        widthInMeter = 1f;
    }
}
