package games.rednblack.gdxar;

/**
 * Select the behavior of the lighting estimation subsystem.
 *
 * @author fgnm
 */
public enum GdxLightEstimationMode {
    /** Lighting estimation is disabled. */
    DISABLED,
    /**
     * Lighting estimation is enabled, generating a single-value intensity estimate and three (R, G,
     * B) color correction values.
     */
    AMBIENT_INTENSITY,
    /**
     * Lighting estimation is enabled, generating inferred Environmental HDR lighting estimation in
     * linear color space.
     */
    ENVIRONMENTAL_HDR
}
