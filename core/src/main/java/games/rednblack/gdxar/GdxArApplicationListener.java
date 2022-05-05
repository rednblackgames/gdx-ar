package games.rednblack.gdxar;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.graphics.Camera;

/**
 * Main interface to be used for libGDX core project. In receive canonical libGDX callbacks from
 * system, in addition takes track of main {@link GdxAR} API.
 *
 * @author fgnm
 */
public abstract class GdxArApplicationListener {
    private GdxAR arAPI;

    /** Rendering callback that contains information from AR world for the current frame */
    public abstract void renderARModels(GdxFrame frame);

    /** Callback to notify that the AR framework has start tracking at least one surface */
    public void lookingSurfaces(boolean hasSurfaces) { }

    /** Standard libGDX create callback similar to  {@link ApplicationListener#create()}
     * with information on AR camera */
    public void create(Camera arCamera) { }

    /** Standard libGDX create callback similar to  {@link ApplicationListener#create()}
     * with information on AR camera */
    public void render () { }

    /** Standard libGDX callback similar to {@link ApplicationListener#resize(int, int)}*/
    public void resize(int width, int height) { }

    /** Standard libGDX create callback similar to  {@link ApplicationListener#pause()}*/
    public void pause() { }

    /** Standard libGDX create callback similar to  {@link ApplicationListener#resume()}*/
    public void resume() { }

    /** Standard libGDX create callback similar to  {@link ApplicationListener#dispose()}*/
    public void dispose() { }

    /**
     * Internal function for setting the AR API backend
     * @param arAPI backend interface
     */
    public void setArAPI(GdxAR arAPI) {
        this.arAPI = arAPI;
    }

    /**
     * Access to the AR API backend
     * @return AR API backend
     */
    public GdxAR getArAPI() {
        return arAPI;
    }
}
