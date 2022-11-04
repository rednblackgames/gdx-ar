package games.rednblack.gdxar.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.Disposable;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Implementation of a Streaming texture from CPU buffer to OpenGL Texture, supports async streaming with
 * Pixel Buffer Objects (only for OpenGL ES 3.0+)
 *
 * @author fgnm
 */
public class StreamTexture implements Disposable {
    private static final String TAG = "StreamTexture";

    private boolean textureCreated = false;
    private final int[] pboIds = new int[2];
    private int pboIndex = 0;

    private final boolean pboSupported;
    private final int glFormat;
    private final int glTarget;
    private final int glHandle;

    public StreamTexture(int format) {
        this(GL20.GL_TEXTURE_2D, format);
    }

    public StreamTexture(int target, int format) {
        glFormat = format;
        glTarget = target;
        pboSupported = isPBOSupported();
        glHandle = Gdx.gl.glGenTexture();

        Gdx.gl.glBindTexture(glTarget, glHandle);
        Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
        Gdx.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);

        Gdx.gl.glBindTexture(glTarget, 0);

        if (pboSupported) {
            pboIds[0] = Gdx.gl.glGenBuffer();
            pboIds[1] = Gdx.gl.glGenBuffer();
        }
    }

    public void bind() {
        Gdx.gl.glBindTexture(glTarget, glHandle);
    }

    public void bind(int unit) {
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit);
        Gdx.gl.glBindTexture(glTarget, glHandle);
    }

    public void update(ByteBuffer dataBuffer, int width, int height) {
        int size = dataBuffer.limit();

        //Bind the exture
        Gdx.gl.glBindTexture(glTarget, glHandle);
        if (!textureCreated) {
            //Create texture and allocate buffer for the first time with correct dimensions
            textureCreated = true;
            Gdx.gl.glTexImage2D(glTarget, 0, glFormat, width, height, 0, glFormat, GL20.GL_UNSIGNED_BYTE, dataBuffer);
            if (pboSupported) {
                // glBufferData() with NULL pointer reserves only memory space.
                Gdx.gl.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, pboIds[0]);
                Gdx.gl.glBufferData(GL30.GL_PIXEL_UNPACK_BUFFER, size, null, GL30.GL_STREAM_DRAW);
                Gdx.gl.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, pboIds[1]);
                Gdx.gl.glBufferData(GL30.GL_PIXEL_UNPACK_BUFFER, size, null, GL30.GL_STREAM_DRAW);
                Gdx.gl.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0);
            }
        } else {
            //Stream buffer data to the texture
            if (pboSupported) {
                // "pboIndex" is used to copy pixels from a PBO to a texture object
                // "nextPboIndex" is used to update pixels in a PBO
                pboIndex = (pboIndex + 1) % 2;
                int nextPboIndex = (pboIndex + 1) % 2;

                // bind the PBO
                Gdx.gl.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, pboIds[pboIndex]);
                // copy pixels from PBO to texture object. Use offset instead of ponter.
                Gdx.gl.glTexSubImage2D(glTarget, 0, 0, 0, width, height, glFormat, GL20.GL_UNSIGNED_BYTE, null);

                // bind PBO to update pixel values
                Gdx.gl.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, pboIds[nextPboIndex]);
                // map the buffer object into client's memory
                // Note that glMapBuffer() causes sync issue.
                // If GPU is working with this buffer, glMapBuffer() will wait(stall)
                // for GPU to finish its job. To avoid waiting (stall), you can call
                // first glBufferData() with NULL pointer before glMapBuffer().
                // If you do that, the previous data in PBO will be discarded and
                // glMapBuffer() returns a new allocated pointer immediately
                // even if GPU is still working with the previous data.
                Gdx.gl.glBufferData(GL30.GL_PIXEL_UNPACK_BUFFER, size, null, GL30.GL_STREAM_DRAW);

                // update data directly on the mapped buffer
                Buffer bb = Gdx.gl30.glMapBufferRange(GL30.GL_PIXEL_UNPACK_BUFFER, 0, size, GL30.GL_MAP_WRITE_BIT);
                if (bb != null) {
                    ByteBuffer b = (ByteBuffer) bb;
                    b.put(dataBuffer);
                    // release pointer to mapping buffer
                    Gdx.gl30.glUnmapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER);
                } else {
                    Gdx.app.log(TAG, String.valueOf(Gdx.gl.glGetError()));
                }

                // it is good idea to release PBOs with ID 0 after use.
                // Once bound with 0, all pixel operations behave normal ways.
                Gdx.gl.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0);
            } else {
                //If PBO are not supported fall back to glTexSubImage2D
                Gdx.gl.glTexSubImage2D(glTarget, 0, 0, 0, width, height, glFormat, GL20.GL_UNSIGNED_BYTE, dataBuffer);
            }
        }

        Gdx.gl.glBindTexture(glTarget, 0);
    }

    public boolean isPBOSupported() {
        return Gdx.gl30 != null;
    }

    public int getHandle() {
        return glHandle;
    }

    @Override
    public void dispose() {
        Gdx.gl.glDeleteTexture(glHandle);
        if (pboSupported) {
            Gdx.gl.glDeleteBuffer(pboIds[0]);
            Gdx.gl.glDeleteBuffer(pboIds[1]);
        }
    }
}
