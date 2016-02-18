package org.matsim.vis.otfvis.opengl.gl;

import com.jogamp.opengl.GL2;

public class DrawingUtils {
	
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
