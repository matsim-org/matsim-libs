package org.matsim.vis.otfvis.opengl.gl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.IOException;
import java.io.InputStream;

public class GLUtils {

	static public Texture createTexture(GL2 gl, final InputStream _data) {
		try (InputStream data = _data) {
			Texture t = TextureIO.newTexture(data, true, null);
			t.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			t.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
			return t;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
