/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleBackgroundDrawer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.opengl.drawer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import org.matsim.vis.otfvis.caching.SceneGraph;

import java.awt.geom.Rectangle2D;

/**
 * SimpleBackgroundDrawer can draw a picture behind the network at a given coord.
 * @author dstrippgen
 *
 */
public class SimpleBackgroundDrawer extends AbstractBackgroundDrawer {
	private Texture picture = null;
	private final Rectangle2D.Float abskoords;
	private final String name;

	
	public SimpleBackgroundDrawer(final String picturePath, final Rectangle2D.Float koords) {
		this.abskoords = koords;
		this.name = picturePath;
	}

	@Override
	public void onDraw(final GL2 gl) {
		if (this.picture == null) this.picture = OTFOGLDrawer.createTexture(gl, this.name);
		if (this.picture == null) return;
        final TextureCoords tc = this.picture.getImageTexCoords();
        final float tx1 = tc.left();
        final float ty1 = tc.top();
        final float tx2 = tc.right();
        final float ty2 = tc.bottom();


        final float z = 0.0f;
        this.picture.enable(gl);
        this.picture.bind(gl);

        gl.glColor4f(1,1,1,1);

		final Rectangle2D.Float koords = new Rectangle2D.Float((float)(this.abskoords.x - this.offsetEast), (float)(this.abskoords.y- this.offsetNorth), this.abskoords.width, this.abskoords.height);
		
        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(koords.x, koords.y, z);
        gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(koords.x, koords.y + koords.height, z);
        gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(koords.x + koords.width, koords.y + koords.height, z);
        gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(koords.x + koords.width, koords.y, z);
        gl.glEnd();
        
        this.picture.disable(gl);
	}

	@Override
	public void addToSceneGraph(SceneGraph graph) {
		graph.addItem(this);
	}
	
}

