package games.rednblack.gdxar.android;

import static android.opengl.GLES20.*;
import static android.opengl.GLES30.GL_LINEAR;
import static android.opengl.GLES30.GL_RG;
import static android.opengl.GLES30.GL_RG8;

import android.media.Image;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;

/** Handle RG8 GPU texture containing a DEPTH16 depth image. */
public final class DepthTextureHandler {

    private int depthTextureId = -1;
    private int depthTextureWidth = -1;
    private int depthTextureHeight = -1;

    /**
     * Creates and initializes the depth texture. This method needs to be called on a
     * thread with a EGL context attached.
     */
    public void createOnGlThread() {
        int[] textureId = new int[1];
        glGenTextures(1, textureId, 0);
        depthTextureId = textureId[0];
        glBindTexture(GL_TEXTURE_2D, depthTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    }

    /**
     * Updates the depth texture with the content from acquireDepthImage().
     * This method needs to be called on a thread with a EGL context attached.
     */
    public void update(final Frame frame) {
        try {
            Image depthImage = frame.acquireDepthImage16Bits();
            glBindTexture(GL_TEXTURE_2D, depthTextureId);

            if (depthTextureWidth == -1) {
                depthTextureWidth = depthImage.getWidth();
                depthTextureHeight = depthImage.getHeight();
                glTexImage2D(
                        GL_TEXTURE_2D,
                        0,
                        GL_RG8,
                        depthTextureWidth,
                        depthTextureHeight,
                        0,
                        GL_RG,
                        GL_UNSIGNED_BYTE,
                        depthImage.getPlanes()[0].getBuffer());
            } else {
                glTexSubImage2D(
                        GL_TEXTURE_2D,
                        0,
                        0, 0,
                        depthTextureWidth,
                        depthTextureHeight,
                        GL_RG,
                        GL_UNSIGNED_BYTE,
                        depthImage.getPlanes()[0].getBuffer());
            }

            depthImage.close();
        } catch (NotYetAvailableException e) {
            // This normally means that depth data is not available yet.
        }
    }

    public int getDepthTexture() {
        return depthTextureId;
    }

    public int getDepthWidth() {
        return depthTextureWidth;
    }

    public int getDepthHeight() {
        return depthTextureHeight;
    }
}