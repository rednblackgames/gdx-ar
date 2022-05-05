package games.rednblack.gdxar.util;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider;

/**
 * Simple shader provider that gives an extension point to register new shaders.
 *
 * @author claywilkinson
 */
public class DebugShaderProvider extends BaseShaderProvider {

    public void registerShader(Shader shader) {
        this.shaders.add(shader);
    }

    @Override
    protected Shader createShader(Renderable renderable) {
        if (renderable.material.id.startsWith(PlaneMaterial.MATERIAL_ID_PREFIX)) {
            return PlaneMaterial.getShader(renderable);
        } else {
            return new DefaultShader(renderable);
        }
    }
}

