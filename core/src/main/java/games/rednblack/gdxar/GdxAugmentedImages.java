package games.rednblack.gdxar;

import com.badlogic.gdx.utils.Array;

/**
 * Utility class for Augmented Images management.
 *
 * @author fgnm
 */
public class GdxAugmentedImages extends Array<GdxAugmentedImage> {
    private final GdxAugmentedImage tmpImage = new GdxAugmentedImage();

    public GdxAugmentedImages() {
        super(GdxAugmentedImage.class);
    }

    public GdxAugmentedImages(int capacity) {
        super(capacity);
    }

    public GdxAugmentedImages(boolean ordered, int capacity) {
        super(ordered, capacity, GdxAugmentedImage.class);
    }

    public GdxAugmentedImages(Array<? extends GdxAugmentedImage> array) {
        super(array);
    }

    public GdxAugmentedImages(GdxAugmentedImage[] array) {
        super(array);
    }

    public GdxAugmentedImages(boolean ordered, GdxAugmentedImage[] array, int start, int count) {
        super(ordered, array, start, count);
    }

    public boolean contains(int index) {
        tmpImage.index = index;
        return contains(tmpImage, false);
    }
}
