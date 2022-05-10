package games.rednblack.gdxar;

/**
 * The normal vector of a {@link GdxPlane}, for filtering purposes.
 *
 *  @author fgnm
 */
public enum GdxPlaneType {
    /** A horizontal plane facing upward (e.g. floor or tabletop). */
    HORIZONTAL_UPWARD_FACING,
    /** A horizontal plane facing downward (e.g. a ceiling). */
    HORIZONTAL_DOWNWARD_FACING,
    /** A vertical plane (e.g. a wall). */
    VERTICAL,
    /** Used for hit plane test */
    ANY
}
