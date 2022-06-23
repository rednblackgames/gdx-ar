package games.rednblack.gdxar;

/**
 * Custom configuration for backend initialization
 *
 * @author fgnm
 */
public class GdxARConfiguration {
    /** Set the type of Light Estimation mode, disabled by default */
    public GdxLightEstimationMode lightEstimationMode = GdxLightEstimationMode.DISABLED;
    /** Set the type of plane finding mode, horizontal by default */
    public GdxPlaneFindingMode planeFindingMode = GdxPlaneFindingMode.HORIZONTAL;
    /** Enable depth processing if device is able to support it */
    public boolean enableDepth = false;
    /** Enable internal debug mode */
    public boolean debugMode = false;

    public GdxARConfiguration() {

    }

    public GdxARConfiguration(GdxARConfiguration configuration) {
        lightEstimationMode = configuration.lightEstimationMode;
        planeFindingMode = configuration.planeFindingMode;
        enableDepth = configuration.enableDepth;
        debugMode = configuration.debugMode;
    }
}
