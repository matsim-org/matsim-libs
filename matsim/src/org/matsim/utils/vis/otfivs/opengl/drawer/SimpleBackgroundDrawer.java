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

package org.matsim.utils.vis.otfivs.opengl.drawer;

import static javax.media.opengl.GL.GL_QUADS;

import java.awt.geom.Rectangle2D;

import javax.media.opengl.GL;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

public class SimpleBackgroundDrawer extends OTFGLDrawableImpl {
	private Texture picture = null;
	private final Rectangle2D.Float abskoords;
	private final String name;
	private double offsetEast; 
	private double offsetNorth;
	
	public SimpleBackgroundDrawer(String picturePath, Rectangle2D.Float koords) {
		this.abskoords = koords;
		name = picturePath;
	}

	public void onDraw(GL gl) {
		if (picture == null) this.picture = OTFOGLDrawer.createTexture(name);
		if (picture == null) return;
        TextureCoords tc = picture.getImageTexCoords();
        float tx1 = tc.left();
        float ty1 = tc.top();
        float tx2 = tc.right();
        float ty2 = tc.bottom();


        float z = 1.1f;
        picture.enable();
        picture.bind();

        gl.glColor4f(1,1,1,1);

		Rectangle2D.Float koords = new Rectangle2D.Float((float)(abskoords.x - offsetEast), (float)(abskoords.y- offsetNorth), abskoords.width, abskoords.height);
		
        gl.glBegin(GL_QUADS);
        gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(koords.x, koords.y, z);
        gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(koords.x, koords.y + koords.height, z);
        gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(koords.x + koords.width, koords.y + koords.height, z);
        gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(koords.x + koords.width, koords.y, z);
        gl.glEnd();
        
        picture.disable();
	}

	public void setOffset(double offsetEast, double offsetNorth) {
		this.offsetEast = offsetEast;
		this.offsetNorth = offsetNorth;
	}
	
}

