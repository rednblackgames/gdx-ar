package games.rednblack.gdxar;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Null;

import java.io.InputStream;

import games.rednblack.gdxar.util.RawAugmentedImageAsset;

/**
 * Main GdxAR API Interface to interact with underlying AR framework.
 *
 * @author fgnm
 */
public interface GdxAR {

    /**
     * Request to create and attach to a Plane an Anchor for further tracking. Performs an hit plane
     * check.
     *
     * @param x position in screen coordinate
     * @param y position in screen coordinate
     * @param planeType filter hit test with a specific plane type
     * @return a new {@link GdxAnchor} object or null if no plane was hit
     */
    @Null
    GdxAnchor requestHitPlaneAnchor(float x, float y, GdxPlaneType planeType);

    /**
     * Request a pose from an hit plane check.
     *
     * @param x position in screen coordinate
     * @param y position in screen coordinate
     * @param planeType filter hit test with a specific plane type
     * @return a new {@link GdxPose} or null if no plane was hit
     */
    @Null
    GdxPose requestHitPlanePose(float x, float y, GdxPlaneType planeType);

    /**
     * Request to create a new geospatial anchor for further tracking
     *
     * @param latitude position in WGS84
     * @param longitude position in WGS84
     * @param altitude position in WGS84
     * @param rotation vector of the object compared to Earth
     *
     * @return a new {@link GdxAnchor} object or null if anchor was created
     */
    @Null
    public GdxAnchor createGeospatialAnchor(double latitude, double longitude, double altitude, Quaternion rotation);

    /**
     * Usually, camera autofocus is disabled by default for quality reason, enable only when needed.
     *
     * @param autofocus enable or disable
     */
    void setAutofocus(boolean autofocus);

    /**
     * AR has its own camera to render object in real world space.
     *
     * @return Camera object using framework projection matrix
     */
    PerspectiveCamera getARCamera();

    /**
     * AR rendering is not always needed during app execution, this function disable AR updates.
     * If disabled {@link GdxArApplicationListener#renderARModels(GdxFrame)} will not be called.
     *
     * @param renderAR enable or disable AR updates
     */
    void setRenderAR(boolean renderAR);

    /**
     * Check if the library is rendering AR scenes or not.
     *
     * @return true if AR rendering is enabled with {@link #setRenderAR(boolean)}
     */
    boolean isRenderingAR();

    /**
     * Check if AR is supported by the current device settings.
     *
     * @return true if AR is ready to be used.
     */
    boolean isARAllowed();

    /**
     * Load an Augmented Images Database from a binary file.
     *
     * @param databaseStream InputStream of the database asset file.
     * @return success if the database was correctly loaded into AR session
     */
    boolean loadAugmentedImageDatabase(InputStream databaseStream);

    /**
     * Build a local database using raw asset images
     * @param images list of images to be added into database
     * @return map that link the index into database to the image name
     */
    IntMap<String> buildAugmentedImageDatabase(Array<RawAugmentedImageAsset> images);

    /**
     * Enable or disable surface geometry, in same cases getting geometry info from backend AR Framework may result
     * in an additional overhead to copy geometry data from ARCore/ARKit to GdxAR.
     *
     * @param geometryEnabled enable/disable geometry data
     */
    void enableSurfaceGeometry(boolean geometryEnabled);

    /**
     * Set common configuration to reduce memory and power usage in the AR framework
     *
     * @param powerSaveMode enable or disable power save mode
     */
    void setPowerSaveMode(boolean powerSaveMode);

    /**
     * Get the light estimation mode as specified in the configuration
     *
     * @return current light estimation mode
     */
    GdxLightEstimationMode getLightEstimationMode();
}
