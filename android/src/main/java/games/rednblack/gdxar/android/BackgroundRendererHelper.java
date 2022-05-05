package games.rednblack.gdxar.android;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Sets up the OES texture used to render the camera. This is refactored out from the rendering of
 * the background to decouple the background processing, such a rotation, from the actual rendering.
 *
 * @author claywilkinson, fgnm
 */
public class BackgroundRendererHelper {
    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXCOORDS_PER_VERTEX = 2;
    private static final int FLOAT_SIZE = 4;

    private FloatBuffer quadVertices;
    private FloatBuffer quadTexCoord;
    private FloatBuffer quadTexCoordTransformed;

    private int mTextureId = -1;
    private int mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

    public BackgroundRendererHelper() {}

    public int getTextureId() {
        return mTextureId;
    }

    public void createOnGlThread(Context context) {
        // Generate the background texture.
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        GLES20.glBindTexture(mTextureTarget, mTextureId);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        int numVertices = 4;
        if (numVertices != QUAD_COORDS.length / COORDS_PER_VERTEX) {
            throw new RuntimeException("Unexpected number of vertices in BackgroundRenderer.");
        }

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE);
        bbVertices.order(ByteOrder.nativeOrder());
        quadVertices = bbVertices.asFloatBuffer();
        quadVertices.put(QUAD_COORDS);
        quadVertices.position(0);

        ByteBuffer bbTexCoords =
                ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoords.order(ByteOrder.nativeOrder());
        quadTexCoord = bbTexCoords.asFloatBuffer();
        quadTexCoord.put(QUAD_TEXCOORDS);
        quadTexCoord.position(0);

        ByteBuffer bbTexCoordsTransformed =
                ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder());
        quadTexCoordTransformed = bbTexCoordsTransformed.asFloatBuffer();
    }

    private static final float[] QUAD_COORDS =
            new float[] {
                    -1.0f, -1.0f, 0.0f, -1.0f, +1.0f, 0.0f, +1.0f, -1.0f, 0.0f, +1.0f, +1.0f, 0.0f,
            };

    private static final float[] QUAD_TEXCOORDS =
            new float[] {
                    0.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f,
            };

    float[] tmpVertices;
    float[] getVertices(Frame frame) {
        if (frame != null && frame.hasDisplayGeometryChanged()) {
            frame.transformCoordinates2d(Coordinates2d.VIEW_NORMALIZED, quadTexCoord, Coordinates2d.TEXTURE_NORMALIZED, quadTexCoordTransformed);
        }
        if (tmpVertices == null)
            tmpVertices = new float[QUAD_COORDS.length + QUAD_TEXCOORDS.length];

        for (int i = 0; i < 4; i++) {
            tmpVertices[(i * 5)] = QUAD_COORDS[i * 3];
            tmpVertices[(i * 5) + 1] = QUAD_COORDS[(i * 3) + 1];
            tmpVertices[(i * 5) + 2] = QUAD_COORDS[(i * 3) + 2];
            tmpVertices[(i * 5) + 3] = quadTexCoordTransformed.get((i * 2));
            tmpVertices[(i * 5) + 4] = quadTexCoordTransformed.get((i * 2) + 1);
        }
        return tmpVertices;
    }
}

