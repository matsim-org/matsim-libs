/* *********************************************************************** *
 * project: org.matsim.*
 * VisGUIMouseHandler.java
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

package org.matsim.vis.otfvis.opengl.gui;

import static javax.media.opengl.GL.GL_BLEND;
import static javax.media.opengl.GL.GL_MODELVIEW;
import static javax.media.opengl.GL.GL_MODELVIEW_MATRIX;
import static javax.media.opengl.GL.GL_PROJECTION;
import static javax.media.opengl.GL.GL_PROJECTION_MATRIX;
import static javax.media.opengl.GL.GL_QUADS;
import static javax.media.opengl.GL.GL_SRC_ALPHA;
import static javax.media.opengl.GL.GL_VIEWPORT;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.swing.event.MouseInputAdapter;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.Point3f;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

/**
 * VisGUIMouseHandler handles any user input.
 * It is the most important class apart from OTFOGLDrawer and OTFHostControlBar.
 * It is responsible for moving and zooming the screen rectangle and for handling the user clicks.
 *
 * @author dstrippgen
 *
 */
public class VisGUIMouseHandler extends MouseInputAdapter {

	private double[] modelview = new double[16];
	private double[] projection = new double[16];
	private int[] viewport = new int[4];
	private QuadTree.Rect viewBounds = null;


	private Point start = null;

	private Rectangle currentRect = null;
	private double scale = 1.;

	private int button = 0;
	private final OTFDrawer clickHandler;

	private Texture marker = null;

	private float alpha = 1.0f;

	public VisGUIMouseHandler(OTFDrawer clickHandler) {
		this.clickHandler = clickHandler;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		int mbutton = e.getButton();
		String function = "";
		switch (mbutton) {
		case 1:
			function = OTFClientControl.getInstance().getOTFVisConfig().getLeftMouseFunc();
			break;
		case 2:
			function = OTFClientControl.getInstance().getOTFVisConfig().getMiddleMouseFunc();
			break;
		case 3:
			function = OTFClientControl.getInstance().getOTFVisConfig().getRightMouseFunc();
			break;
		}
		if(function.equals("Zoom")) button = 1;
		else if (function.equals("Pan")) button = 2;
		else if (function.equals("Menu")) button = 3;
		else if (function.equals("Select")) button = 4;
		else button = 0;
		start = new Point(x, y);
		alpha = 1.0f;
		currentRect = null;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (button == 1 || button == 4)
			updateSize(e);
		else if (button == 2) {
			int deltax = start.x - e.getX();
			int deltay = start.y - e.getY();
			start.x = e.getX();
			start.y = e.getY();
			Point3f center = getOGLPos(viewport[2]/2, viewport[3]/2);
			Point3f excenter = getOGLPos(viewport[2]/2+deltax, viewport[3]/2+deltay);
			float glDeltaX = excenter.x - center.x;
			float glDeltaY = excenter.y - center.y;
			viewBounds = new Rect(viewBounds.minX + glDeltaX, viewBounds.minY + glDeltaY, viewBounds.maxX + glDeltaX, viewBounds.maxY + glDeltaY);
			clickHandler.redraw();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// update screen one last time
		mouseDragged(e);

		Rectangle screenRect = new Rectangle(start);
		screenRect.add(e.getPoint());
		if ((screenRect.getHeight() > 10)&& (screenRect.getWidth() > 10)) {
			if (button == 1 || button == 4) {
				if (button == 1) {
					int deltax = Math.abs(start.x - e.getX());
					int deltay = Math.abs(start.y - e.getY());
					double ratio =( (start.y - e.getY()) > 0 ? 1:0) + Math.max((double)deltax/viewport[2], (double)deltay/viewport[3]);
					Rectangle2D scaledNewViewBounds = quadTreeRectToRectangle2D(viewBounds.scale(ratio - 1, ratio - 1));
					Rectangle2D scaledAndTranslatedNewViewBounds = new Rectangle2D.Double(scaledNewViewBounds.getX() + (currentRect.getCenterX() - viewBounds.centerX), scaledNewViewBounds.getY() + (currentRect.getCenterY() - viewBounds.centerY), scaledNewViewBounds.getWidth(), scaledNewViewBounds.getHeight());
					Animator viewBoundsAnimator = PropertySetter.createAnimator(2020, this, "viewBounds", getViewBounds(), scaledAndTranslatedNewViewBounds);
					viewBoundsAnimator.start();
					// TODO: Scale property
					Animator rectFader = PropertySetter.createAnimator(2020, this, "alpha", 1.0f, 0.f);
					rectFader.setStartDelay(200);
					rectFader.setAcceleration(0.4f);
					rectFader.start();
				} else {
					clickHandler.handleClick(currentRect, button);
					currentRect = null;
					setAlpha(0);
				}
			}
		} else {
			Point3f newcameraStart = getOGLPos(start.x, start.y);
			Point2D.Double point = new Point2D.Double(newcameraStart.getX(), newcameraStart.getY());
			clickHandler.handleClick(point, button, e);
			currentRect = null;
		}
		button = 0;
	}

	private void updateSize(MouseEvent e) {
		Point3f newRectStart = getOGLPos(start.x, start.y);
		Point3f newRectEnd = getOGLPos(e.getX(), e.getY());
		currentRect = new Rectangle(new Point((int)newRectStart.getX(), (int)newRectStart.getY()));
		currentRect.add(newRectEnd.getX(), newRectEnd.getY());
		// This only redraws GUI Elements, no need to invalidate(), just redraw()
		clickHandler.redraw();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		scaleNetworkRelative((float) Math.pow(2.0f,e.getWheelRotation()));
	}

	public void setAlpha(float a){
		alpha = a;
		// This only redraws GUI Elements, no need to invalidate(), just redraw()
		if (clickHandler != null) clickHandler.redraw();
	}

	/**
	 * Renders the given texture so that it is centered within the given
	 * dimensions.
	 */
	private void renderFace(GL gl, Texture t) {
		TextureCoords tc = t.getImageTexCoords();
		float tx1 = tc.left();
		float ty1 = tc.top();
		float tx2 = tc.right();
		float ty2 = tc.bottom();
		t.enable();
		t.bind();

		if (button==4) gl.glColor4f(0.8f, 0.2f, 0.2f, alpha);
		else gl.glColor4f(alpha, alpha, alpha, alpha);

		gl.glBegin(GL_QUADS);
		gl.glTexCoord2f(tx1, ty1); gl.glVertex2f(currentRect.x, currentRect.y);
		gl.glTexCoord2f(tx2, ty1); gl.glVertex2f(currentRect.x, currentRect.y + currentRect.height);
		gl.glTexCoord2f(tx2, ty2); gl.glVertex2f(currentRect.x + currentRect.width, currentRect.y + currentRect.height);
		gl.glTexCoord2f(tx1, ty2); gl.glVertex2f(currentRect.x + currentRect.width, currentRect.y);
		gl.glEnd();
		t.disable();

	}

	public void drawElements(GL gl){
		if((currentRect != null) && (alpha >= 0.f)){
			gl.glEnable(GL_BLEND);
			gl.glBlendFunc(GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			renderFace(gl, marker);
			gl.glDisable(GL_BLEND);
		} else {
			currentRect = null;
			alpha = 1.0f;
		}

	}

	public void setFrustrum(GL gl) {
		GLU glu = new GLU();
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluOrtho2D(viewBounds.minX, viewBounds.maxX, viewBounds.minY, viewBounds.maxY);
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();
		// update matrices for mouse position calculation
		gl.glGetDoublev( GL_MODELVIEW_MATRIX, modelview,0);
		gl.glGetDoublev( GL_PROJECTION_MATRIX, projection,0);
		gl.glGetIntegerv( GL_VIEWPORT, viewport,0 );
	}


	private void scaleNetworkRelative(double scale) {
		this.scale *= scale;
		viewBounds = viewBounds.scale(scale - 1, scale - 1);
		clickHandler.redraw();
	}

	public void setScale(double scale) {
		double scaleFactor = scale / this.scale;
		scaleNetworkRelative(scaleFactor);
	}

	synchronized public Point3f getOGLPos(int x, int y)
	{
		double[] obj_pos = new double[3];
		float winX, winY;//, winZ = cameraStart.getZ();
		float posX, posY;//, posZ;
		double[] w_pos = new double[3];
		double[] z_pos = new double[1];


		winX = x;
		winY = viewport[3] - y;
		z_pos[0]=1;

		GLU glu = new GLU();
		obj_pos[2]=0; // Check view relative z-koord of layer zero == visnet layer
		glu.gluProject( obj_pos[0], obj_pos[1],obj_pos[2], modelview,0, projection,0, viewport,0, w_pos,0);
		glu.gluUnProject( winX, winY, w_pos[2], modelview,0, projection,0, viewport,0, obj_pos,0);

		posX = (float)obj_pos[0];
		posY = (float)obj_pos[1];
		return new Point3f(posX, posY, 0);
	}

	public CoordImpl getPixelsize() {
		Point3f p1 = getOGLPos(300,300);
		Point3f p2 = getOGLPos(301,301);
		return new CoordImpl(Math.abs(p2.x-p1.x), Math.abs(p2.y-p1.y));
	}

	public double getScale() {
		return this.scale;
	}

	public void init(GL gl) {
		marker = OTFOGLDrawer.createTexture(MatsimResource.getAsInputStream("otfvis/marker.png"));
		setFrustrum(gl);
	}

	public void setBounds(GLAutoDrawable drawable, float minEasting, float minNorthing, float maxEasting, float maxNorthing) {
		double aspectRatio;
		if (drawable != null) {
			aspectRatio = (double) drawable.getWidth() / (double) drawable.getHeight();
			double pixelRatio = (double) drawable.getHeight() / (double) (maxNorthing-minNorthing);
			this.scale = 1.0f / (float) pixelRatio;
		} else {
			aspectRatio = 1;
		}
		viewBounds =  new QuadTree.Rect(minEasting, minNorthing, minEasting + (maxNorthing - minNorthing) * aspectRatio, maxNorthing);
	}

	public void setViewBoundsAsQuadTreeRect(QuadTree.Rect viewBounds) {
		this.viewBounds = viewBounds;
	}

	public QuadTree.Rect getViewBoundsAsQuadTreeRect() {
		return viewBounds;
	}

	public Rectangle2D getViewBounds() {
		QuadTree.Rect viewBounds = this.viewBounds;
		return quadTreeRectToRectangle2D(viewBounds);
	}

	private Rectangle2D quadTreeRectToRectangle2D(QuadTree.Rect viewBounds) {
		return new Rectangle2D.Double(viewBounds.minX, viewBounds.minY, viewBounds.maxX-viewBounds.minX, viewBounds.maxY-viewBounds.minY);
	}

	public void setViewBounds(Rectangle2D viewBounds) {
		this.viewBounds = new QuadTree.Rect(viewBounds.getMinX(), viewBounds.getMinY(), viewBounds.getMaxX(), viewBounds.getMaxY());
	}

}
