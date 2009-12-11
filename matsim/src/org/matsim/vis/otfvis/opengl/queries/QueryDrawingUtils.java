package org.matsim.vis.otfvis.opengl.queries;

import javax.media.opengl.GL;

public class QueryDrawingUtils {
	
	public static void drawCircle(GL gl, float x, float y, float size) {
		float w = 40;

		gl.glLineWidth(2);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glBegin(GL.GL_LINE_STRIP);
		for (float f = 0; f < w;) {
			gl.glVertex3d(Math.cos(f)*size + x, Math.sin(f)*size + y,0);
			f += (2*Math.PI/w);
		}
		gl.glEnd();
		gl.glDisable(GL.GL_LINE_SMOOTH);
	}

}
