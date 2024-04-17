package games.rednblack.gdxar.ios;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

import games.rednblack.gdxar.util.StreamTexture;
import org.robovm.apple.arkit.ARFrame;
import org.robovm.apple.coregraphics.CGAffineTransform;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.corevideo.CVPixelBuffer;
import org.robovm.apple.corevideo.CVPixelBufferLockFlags;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.rt.bro.ptr.BytePtr;
import org.robovm.rt.bro.ptr.VoidPtr;

import java.nio.ByteBuffer;

/**
 * Sets up the YUV textures used to render the camera. This is refactored out from the rendering of
 * the background to decouple the background processing, such a rotation, from the actual rendering.
 *
 * @author fgnm
 */
public class BackgroundRendererHelper {
    private float[] tmpVertices;

    private static final float[] QUAD_COORDS =
            new float[] {
                    -1.0f, +1.0f,
                    -1.0f, -1.0f,
                    +1.0f, +1.0f,
                    +1.0f, -1.0f,
            };

    private static final float[] QUAD_COORDS_INVERSE =
            new float[] {
                    +1.0f, -1.0f,
                    +1.0f, +1.0f,
                    -1.0f, -1.0f,
                    -1.0f, +1.0f,
            };

    private static final float[] QUAD_TEXCOORDS =
            new float[] {
                    1.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    0.0f, 0.0f,
            };

    private CGSize viewport;

    private StreamTexture yTexture, uvTexture;

    public BackgroundRendererHelper() {
    }

    public StreamTexture getYTexture() {
        return yTexture;
    }

    public StreamTexture getUVTexture() {
        return uvTexture;
    }

    public void createOnGlThread() {
        viewport = new CGSize(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        yTexture = new StreamTexture(GL20.GL_LUMINANCE);
        uvTexture = new StreamTexture(GL20.GL_LUMINANCE_ALPHA);
    }

    float[] getVertices(ARFrame frame, UIInterfaceOrientation currentOrientation) {
        float[] coords = currentOrientation == UIInterfaceOrientation.LandscapeRight || currentOrientation == UIInterfaceOrientation.LandscapeLeft ? QUAD_COORDS_INVERSE : QUAD_COORDS;

        if (tmpVertices == null)
            tmpVertices = new float[QUAD_COORDS.length + QUAD_TEXCOORDS.length];

        CGAffineTransform transform = frame.displayTransform(currentOrientation, viewport);
        for (int i = 0; i < 4; i++) {
            float x = (coords[i * 2] + 1.0f) / 2.0f;
            float y = (coords[(i * 2) + 1] + 1.0f) / 2.0f;

            tmpVertices[(i * 4)] = ((float) (transform.getA() * x + transform.getC() * y + transform.getTx()) * 2.0f) - 1.0f;
            tmpVertices[(i * 4) + 1] = ((float) (transform.getB() * x + transform.getD() * y + transform.getTy()) * 2.0f) - 1.0f;
            tmpVertices[(i * 4) + 2] = QUAD_TEXCOORDS[(i * 2)];
            tmpVertices[(i * 4) + 3] = QUAD_TEXCOORDS[(i * 2) + 1];
        }
        return tmpVertices;
    }

    public void updateTexture(ARFrame frame) {
        CVPixelBuffer cameraImage = frame.getCapturedImage();
        int width = (int) cameraImage.getWidthOfPlane(0);
        int height = (int) cameraImage.getHeightOfPlane(0);
        cameraImage.lockBaseAddress(CVPixelBufferLockFlags.ReadOnly);
        VoidPtr baseAddress = cameraImage.getBaseAddressOfPlane(0);

        if (baseAddress != null) {
            int size = width * height;
            ByteBuffer buffer = baseAddress.as(BytePtr.class).asByteBuffer(size);
            yTexture.update(buffer, width, height);
        }

        width = (int) cameraImage.getWidthOfPlane(1);
        height = (int) cameraImage.getHeightOfPlane(1);
        baseAddress = cameraImage.getBaseAddressOfPlane(1);

        if (baseAddress != null) {
            int size = width * height * 2;
            ByteBuffer buffer = baseAddress.as(BytePtr.class).asByteBuffer(size);
            uvTexture.update(buffer, width, height);
        }

        cameraImage.unlockBaseAddress(CVPixelBufferLockFlags.ReadOnly);
        cameraImage.dispose();
    }

    public CGSize getViewport() {
        return viewport;
    }

    public void dispose() {
        yTexture.dispose();
        uvTexture.dispose();
    }
}