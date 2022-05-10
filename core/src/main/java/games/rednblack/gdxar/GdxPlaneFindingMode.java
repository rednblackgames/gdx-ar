package games.rednblack.gdxar;

/**
 * Behavior of the plane detection system.
 *
 *  @author fgnm
 */
public enum GdxPlaneFindingMode {
    /** Plane detection is disabled. */
    DISABLED,
    /** Detection of only horizontal planes is enabled. */
    HORIZONTAL,
    /** Detection of only vertical planes is enabled. */
    VERTICAL,
    /** Detection of both horizontal and vertical planes is enabled. */
    HORIZONTAL_AND_VERTICAL;
}
