package games.rednblack.gdxar.ios;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import org.robovm.apple.arkit.ARFrame;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.uikit.UIInterfaceOrientation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Background rendering for an ARKit. This renders the camera texture in the background of the scene.
 *
 * @author fgnm
 */
class BackgroundRenderer {
    private final ShaderProgram shader;
    private final Mesh mesh;
    private final IntBuffer intbuf;
    private final int[] saveFlags;

    private final BackgroundRendererHelper backgroundRendererHelper;

    private UIInterfaceOrientation lastOrientation = null;

    private static final String vertexShaderCode = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE +";\n"
                    + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
                    + "varying vec2 v_TexCoord;\n"
                    + "void main() {\n"
                    + "   gl_Position = " + ShaderProgram.POSITION_ATTRIBUTE +";\n"
                    + "   v_TexCoord = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
                    + "}";

    private static final String fragmentShaderCode = "precision mediump float;\n"
                    + "varying vec2 v_TexCoord;\n"
                    + "uniform sampler2D yTexture;\n"
                    + "uniform sampler2D uvTexture;\n"
                    + "\n"
                    + "const mat4 transform = mat4(\n" +
                    "                           1.0000,  1.0000,  1.0000, 0.0000,\n" +
                    "                           0.0000, -0.3441,  1.7720, 0.0000,\n" +
                    "                           1.4020, -0.7141,  0.0000, 0.0000,\n" +
                    "                          -0.7010,  0.5291, -0.8860, 1.0000\n" +
                    "                          );\n"
                    + "void main() {\n"
                    + "    vec4 YPlane = texture2D(yTexture, v_TexCoord);\n"
                    + "    vec4 CbCrPlane = texture2D(uvTexture, v_TexCoord);\n"
                    + "    gl_FragColor = transform * vec4(YPlane.r, CbCrPlane.ra, 1.0);\n"
                    + "}";

    public BackgroundRenderer() {
        backgroundRendererHelper = new BackgroundRendererHelper();
        backgroundRendererHelper.createOnGlThread();
        shader = new ShaderProgram(vertexShaderCode, fragmentShaderCode);
        if (!shader.isCompiled()) {
            System.out.println(shader.getLog());
        }

        mesh = new Mesh(true, 4, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                VertexAttribute.TexCoords(0));

        intbuf = ByteBuffer.allocateDirect(16 * Integer.SIZE / 8)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        saveFlags = new int[3];
    }

    public CGSize getViewportSize() {
        return backgroundRendererHelper.getViewport();
    }

    public void update(ARFrame frame, UIInterfaceOrientation currentOrientation) {
        if (mesh.getNumVertices() == 0 || currentOrientation != lastOrientation) {
            lastOrientation = currentOrientation;

            mesh.setVertices(backgroundRendererHelper.getVertices(frame, lastOrientation));
        }

        backgroundRendererHelper.updateTexture(frame);
    }

    public void render() {
        // Save the state of the glContext before drawing.
        GL20 gl = Gdx.gl;
        gl.glGetIntegerv(GL20.GL_DEPTH_TEST, intbuf);
        saveFlags[0] = intbuf.get(0);
        gl.glGetIntegerv(GL20.GL_DEPTH_WRITEMASK, intbuf);
        saveFlags[1] = intbuf.get(0);
        gl.glGetIntegerv(GL20.GL_DEPTH_FUNC, intbuf);
        saveFlags[2] = intbuf.get(0);

        // Disable depth, bind the texture and render it on the mesh.
        gl.glDisable(GL20.GL_DEPTH_TEST);
        gl.glDepthMask(false);

        backgroundRendererHelper.getYTexture().bind(0);
        backgroundRendererHelper.getUVTexture().bind(1);

        shader.bind();
        shader.setUniformi("yTexture", 0);
        shader.setUniformi("uvTexture", 1);
        mesh.render(shader, GL20.GL_TRIANGLE_STRIP);

        // Restore the state of the context.
        if (saveFlags[0] == GL20.GL_TRUE) {
            gl.glEnable(GL20.GL_DEPTH_TEST);
        }
        gl.glDepthMask(saveFlags[1] == GL20.GL_TRUE);
        gl.glDepthFunc(saveFlags[2]);
    }

    public void dispose() {
        backgroundRendererHelper.dispose();
        mesh.dispose();
        shader.dispose();
    }
}
