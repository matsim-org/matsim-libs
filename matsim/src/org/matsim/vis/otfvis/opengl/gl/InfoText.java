/* *********************************************************************** *
 * project: org.matsim.*
 * InfoText.java
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

package org.matsim.vis.otfvis.opengl.gl;

import static javax.media.opengl.GL.GL_MODELVIEW_MATRIX;
import static javax.media.opengl.GL.GL_QUADS;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import org.matsim.api.basic.v01.Id;

import com.sun.opengl.util.j2d.TextRenderer;

/**
 * InfoText is the class behind all text displayed in the OPenGL context.
 * Texts can be either drawn for one tick only or reside permanent until removal.
 * 
 * @author dstrippgen
 *
 */
public class InfoText implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static TextRenderer renderer = null;
	
	public static void setRenderer(TextRenderer renderer) {
		InfoText.renderer = renderer;
	}
	
	public static Rectangle2D getBoundsOf(String testText) {
		return renderer.getBounds(testText);
	}
	
	private String line;
	private float x;
	private float y;
	private float z;
	private float size;
	private float fill = 0.0f;
	private boolean draw2D = false;
	private boolean decorated = true;
	private Color color = new Color(50, 50, 128, 200);
	private Id linkId;

	public InfoText(String text, float x2, float y2, float z2, float size2) {
		this.line = text;
		x = x2;
		y = y2;
		z = z2;
		this.size = size2;
	}

	public Rectangle2D getBounds() {
		return renderer.getBounds(line);
	}
	
	public void draw3D(GL gl) {
		float size = this.size; // kai: this is where font for labels can be made smaller
		//z -= 0.5*z;
		float border=15.f;
		// draw relative to camera aka do not resize
		if (size < 0){
			double[] modelview = new double[16];
			gl.glGetDoublev( GL_MODELVIEW_MATRIX, modelview,0);
			size *= modelview[14];
		}
		
        GLU glu = new GLU();
        Rectangle2D rect = renderer.getBounds(line);
        float halfh = (float)rect.getHeight()/2; 

        if(isDecorated()) {
            gl.glPushMatrix();
            gl.glEnable(GL.GL_BLEND);
            gl.glColor4f(0.9f, 0.9f, 0.9f, (color.getAlpha()/255.f));
            gl.glTranslatef(x , y, z);
            gl.glScalef(size, size, 1);
            gl.glTranslatef(-border , halfh , 0);
            GLUquadric quad1 = glu.gluNewQuadric();
            glu.gluPartialDisk(quad1, 0, halfh, 12, 2, 180, 180);
            glu.gluDeleteQuadric(quad1);
            gl.glBegin(GL_QUADS);
            gl.glVertex3d(0, -halfh, 0);
            gl.glVertex3d(0, halfh, 0);
            gl.glVertex3d(rect.getWidth() + 2*border, halfh, 0);
            gl.glVertex3d(rect.getWidth() + 2*border, -halfh, 0);
            gl.glEnd();
            if (fill > 0.0f) {
                gl.glColor4f(0.9f, 0.7f, 0.7f, (color.getAlpha()/255.f));
                gl.glBegin(GL_QUADS);
                gl.glVertex3d(0, -halfh, 0);
                gl.glVertex3d(0, -halfh -7, 0);
                gl.glVertex3d(rect.getWidth() + 2*border, -halfh -7, 0);
                gl.glVertex3d(rect.getWidth() + 2*border, -halfh, 0);
                gl.glEnd();
                gl.glColor4f(0.9f, 0.5f, 0.5f, 0.9f);
                gl.glBegin(GL_QUADS);
                gl.glVertex3d(0, -halfh, 0);
                gl.glVertex3d(0, -halfh -7, 0);
                gl.glVertex3d(rect.getWidth()*fill + 2*border, -halfh -7, 0);
                gl.glVertex3d(rect.getWidth()*fill + 2*border, -halfh, 0);
                gl.glEnd();
                gl.glColor4f(0.9f, 0.9f, 0.9f, (color.getAlpha()/255.f));
            }
            GLUquadric quad2 = glu.gluNewQuadric();
            gl.glTranslatef(2*border + (float)rect.getWidth() , 0,0);
            glu.gluPartialDisk(quad2, 0, halfh, 12, 2, 0, 180);
            glu.gluDeleteQuadric(quad2);
            gl.glPopMatrix();
            gl.glDisable(GL.GL_BLEND);
        }
        
        renderer.begin3DRendering();
        renderer.setColor(color);
        renderer.draw3D(line, x,y + 0.25f*halfh*size,z, size);
        renderer.end3DRendering();
	}
	
	void draw(GLAutoDrawable drawable){
		if(draw2D) {
	        // Render the text in 2D
	        renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
	        renderer.setColor(0.2f, 0.2f, 0.5f, color.getAlpha()/255.f);
	        renderer.draw(line, (int)x, (int)y);
	        renderer.endRendering();
		} else {
	        // Render the text in 3D
			draw3D(drawable.getGL());
		}
	}

	public float getAlpha() {
		return color.getAlpha()/255.f;
	}

	public void setAlpha(float alpha) {
		color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha*255));
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getX() {
		return x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getY() {
		return y;
	}

	public void setZ(float z) {
		this.z = z;
	}

	public float getZ() {
		return z;
	}

	public void setSize(float size) {
		this.size = size;
	}

	public float getSize() {
		return size;
	}

	public void setFill(float fill) {
		this.fill = fill;
	}

	public float getFill() {
		return fill;
	}

	public void setDecorated(boolean decorated) {
		this.decorated = decorated;
	}

	public boolean isDecorated() {
		return decorated;
	}

	public Id getLinkId() {
		return linkId;
	}

	/**
	 * Associate this text with a link. If this is set to something not null,
	 * the text is only rendered if the link is visible.
	 * 
	 * @param linkId the link for which this text is a label.
	 */
	public void setLinkId(Id linkId) {
		this.linkId = linkId;
	}

	public void setText(String text) {
		this.line = text;
	}
	
}

