/* *********************************************************************** *
 * project: org.matsim.*
 * OTFBackgroundTexturesDrawer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.gregor.snapshots.writers;

import static javax.media.opengl.GL.GL_QUADS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.matsim.api.core.v01.Coord;
import org.matsim.vis.otfvis.opengl.drawer.AbstractBackgroundDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

public class OTFBackgroundTexturesDrawer  extends AbstractBackgroundDrawer implements Serializable{

	private static final long serialVersionUID = 546369663527185491L;
	private final String name;

	private transient Texture picture = null;
	private final List<float []> txCoords = new ArrayList<float[]>();

	private boolean converted = false;

	public OTFBackgroundTexturesDrawer(String name) {
		this.name = name;
	}


	@Override
	public void onDraw(GL gl) {
		if (this.picture == null) this.picture = OTFOGLDrawer.createTexture(this.name);
		if (this.picture == null) return;

		if (!this.converted) convert();

		final TextureCoords tc = this.picture.getImageTexCoords();
		final float tx1 = tc.left();
		final float ty1 = tc.top();
		final float tx2 = tc.right();
		final float ty2 = tc.bottom();

		float z = 0.f;

		for (float [] coords : this.txCoords) {
//			gl.glEnable(GL.GL_BLEND);
//			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			this.picture.enable();
			this.picture.bind();
			gl.glColor4f(1,1,1,1);
//			final Rectangle2D.Float koords = new Rectangle2D.Float((float)(this.offsetEast), (float)(this.offsetNorth), 1000, 1000);

			gl.glBegin(GL_QUADS);
			gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(coords[0],coords[1], z);
			gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(coords[2],coords[3], z);
			gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(coords[4],coords[5], z);
			gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(coords[6],coords[7], z);
			gl.glEnd();
			this.picture.disable();



		}
//		gl.glDisable(GL.GL_BLEND);
	}

	private void convert() {
		for (float [] coords : this.txCoords) {
			for (int i = 1; i < coords.length; i+= 2) {
				coords[i-1] = (float) (coords[i-1] - this.offsetEast);
				coords[i] = (float) (coords[i] - this.offsetNorth);
			}

		}
		this.converted = true;

	}

	public void addLocation(float minX, float minY, float maxX, float maxY) {
		float [] coords = new float [] {minX, maxY, maxX, maxY, maxX , minY, minX, minY};
		this.txCoords.add(coords);
	}

	public void addLocation(Coord c, double angle, double size) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double bx1 = -size/2;
		double by1 = size/2;
		double bx2 = size/2;
		double by2 = size/2;
		double bx3 = size/2;
		double by3 = -size/2;
		double bx4 = -size/2;
		double by4 = -size/2;

		double x1 = bx1 * cos - by1 * sin + c.getX();
		double y1 = bx1 * sin + by1 * cos + c.getY();

		double x2 = bx2 * cos - by2 * sin + c.getX();
		double y2 = bx2 * sin + by2 * cos + c.getY();

		double x3 = bx3 * cos - by3 * sin + c.getX();
		double y3 = bx3 * sin + by3 * cos + c.getY();

		double x4 = bx4 * cos - by4 * sin + c.getX();
		double y4 = bx4 * sin + by4 * cos + c.getY();

		float [] coords = new float [] {(float) x1, (float) y1, (float) x2, (float) y2, (float) x3, (float) y3, (float) x4, (float) y4};
		this.txCoords.add(coords);

	}

}
