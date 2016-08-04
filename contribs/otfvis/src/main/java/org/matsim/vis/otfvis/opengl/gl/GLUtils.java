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

	private static final int w = 40;
	private static final double incr = 2*Math.PI/w;

	public static void drawCircle(GL2 gl, float x, float y, float size) {
		gl.glLineWidth(2);
		gl.glEnable(GL2.GL_LINE_SMOOTH);
		gl.glBegin(GL2.GL_LINE_STRIP);
		float f=0;
		for (int i=0; i<=w; i++) {
			gl.glVertex3d(Math.cos(f)*size + x, Math.sin(f)*size + y,0);
			f += incr;
		}
		gl.glEnd();
		gl.glDisable(GL2.GL_LINE_SMOOTH);
	}
}
