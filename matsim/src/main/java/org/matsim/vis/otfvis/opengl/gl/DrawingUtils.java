package org.matsim.vis.otfvis.opengl.gl;

import javax.media.opengl.GL;

public class DrawingUtils {
	
	private static final int w = 40;
	private static final double incr = 2*Math.PI/w;

	public static void drawCircle(GL gl, float x, float y, float size) {
		gl.glLineWidth(2);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glBegin(GL.GL_LINE_STRIP);
		float f=0;
		for (int i=0; i<=w; i++) {
			gl.glVertex3d(Math.cos(f)*size + x, Math.sin(f)*size + y,0);
			f += incr;
		}
		gl.glEnd();
		gl.glDisable(GL.GL_LINE_SMOOTH);
	}

}
