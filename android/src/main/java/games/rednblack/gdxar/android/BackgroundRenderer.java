package games.rednblack.gdxar.android;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.google.ar.core.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Background rendering for an ARCore activing. This renders the camera texture in the backrgound of
 * the scene.
 *
 * @author claywilkinson
 */
class BackgroundRenderer {
    private final ShaderProgram shader, depthShader;
    private final Mesh mesh;
    private final IntBuffer intbuf;
    private final int[] saveFlags;

    private byte[] depthBytes = new byte[1];
    //private Texture depthTexture;
    private DepthTextureHandler depthTexture;

    // The Shader class in GDX is aware of some common uniform and attribute names.
    // These are used to make setting the values when drawing "automatic".
    // This shader simply draws the OES texture on the provided coordinates.
    // a_position == ShaderProgram.POSITION_ATTRIBUTE
    private static final String vertexShaderCode =
            "attribute vec4 a_position;\n"
                    +
                    // a_texCoord0 == ShaderProgram.TEXCOORD_ATTRIBUTE + "0"
                    "attribute vec2 a_texCoord0;\n"
                    + "varying vec2 v_TexCoord;\n"
                    + "void main() {\n"
                    + "gl_Position = a_position;\n"
                    + " v_TexCoord = a_texCoord0;\n"
                    + "}";

    private static final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "\n"
                    + "precision mediump float;\n"
                    + "varying vec2 v_TexCoord;\n"
                    + "uniform samplerExternalOES sTexture;\n"
                    + "\n"
                    + "\n"
                    + "void main() {\n"
                    + "    gl_FragColor = texture2D(sTexture, v_TexCoord);\n"
                    + "}";

    private static final String depthFragmentShaderCode =
                    "precision mediump float;\n"
                    + "varying vec2 v_TexCoord;\n"
                    + "uniform sampler2D sTexture;\n"
                    + "\n"
                    + "\n"
                    + "void main() {\n"
                    + "    gl_FragColor = texture2D(sTexture, v_TexCoord);\n"
                    + "}";

    public BackgroundRenderer() {
        depthTexture = new DepthTextureHandler();
        depthTexture.createOnGlThread();
        shader = new ShaderProgram(vertexShaderCode, fragmentShaderCode);
        depthShader = new ShaderProgram(vertexShaderCode, depthFragmentShaderCode);

        mesh = new Mesh(true, 4, 0, VertexAttribute.Position(), VertexAttribute.TexCoords(0));

        intbuf = ByteBuffer.allocateDirect(16 * Integer.SIZE / 8)
                        .order(ByteOrder.nativeOrder())
                        .asIntBuffer();
        saveFlags = new int[3];
    }

    public void render(Frame frame) {
        if (mesh.getNumVertices() == 0 || frame.hasDisplayGeometryChanged()) {
            mesh.setVertices(((ARCoreGraphics) Gdx.graphics).getBackgroundVertices(frame));
        }

        // Save the state of the glContext before drawing.
        GL20 gl = Gdx.gl;
        gl.glGetIntegerv(GL20.GL_DEPTH_TEST, intbuf);
        saveFlags[0] = intbuf.get(0);
        gl.glGetIntegerv(GL20.GL_DEPTH_WRITEMASK, intbuf);
        saveFlags[1] = intbuf.get(0);
        gl.glGetIntegerv(GL20.GL_DEPTH_FUNC, intbuf);
        saveFlags[2] = intbuf.get(0);

        // Disable depth, bind the texture and render it on the mesh.
        gl.glDisable(GLES20.GL_DEPTH_TEST);
        gl.glDepthMask(false);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ((ARCoreGraphics) Gdx.graphics).getBackgroundTexture());
        shader.bind();
        mesh.render(shader, GL20.GL_TRIANGLE_STRIP);

        // Restore the state of the context.
        if (saveFlags[0] == GL20.GL_TRUE) {
            gl.glEnable(GL20.GL_DEPTH_TEST);
        }
        gl.glDepthMask(saveFlags[1] == GL20.GL_TRUE);
        gl.glDepthFunc(saveFlags[2]);
    }

    public void renderDepth(Frame frame) {
        depthTexture.update(frame);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glBindTexture(GLES20.GL_TEXTURE_2D, depthTexture.getDepthTexture());
        depthShader.bind();
        depthShader.setUniformi("sTexture", 0);
        mesh.render(depthShader, GL20.GL_TRIANGLE_STRIP);
    }
}
