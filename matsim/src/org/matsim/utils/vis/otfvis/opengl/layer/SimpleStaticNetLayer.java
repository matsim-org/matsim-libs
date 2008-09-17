/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleStaticNetLayer.java
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

package org.matsim.utils.vis.otfvis.opengl.layer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.otfvis.caching.SceneGraph;
import org.matsim.utils.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.utils.vis.otfvis.data.OTFDataQuad;
import org.matsim.utils.vis.otfvis.data.OTFData.Receiver;
import org.matsim.utils.vis.otfvis.gui.OTFDrawable;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.opengl.drawer.OGLProvider;
import org.matsim.utils.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;


public class SimpleStaticNetLayer  extends SimpleSceneLayer{
	public static class SimpleQuadDrawer extends OTFGLDrawableImpl implements OTFDataQuad.Receiver{
		protected final Point2D.Float[] quad = new Point2D.Float[4];
		protected float coloridx = 0;

		public void onDraw( GL gl) {
			//Draw quad
			gl.glBegin(GL.GL_QUADS);
			gl.glVertex3f(quad[0].x, quad[0].y, 0);
			gl.glVertex3f(quad[1].x, quad[1].y, 0);
			gl.glVertex3f(quad[3].x, quad[3].y, 0);
			gl.glVertex3f(quad[2].x, quad[2].y, 0);
			gl.glEnd();
		}

		public static Point2D.Float calcOrtho(Point2D.Float start, Point2D.Float end){
			return calcOrtho(start.x, start.y, end.x, end.y, SimpleStaticNetLayer.cellWidth_m);
		}

		public static Point2D.Float calcOrtho(double startx, double starty, double endx, double endy, double len){
			double dx = endy - starty;
			double dy = endx -startx;
			double sqr1 = Math.sqrt(dx*dx +dy*dy);

			dx = dx*len/sqr1;
			dy = -dy*len/sqr1;

			return new Point2D.Float((float)dx,(float)dy);
		}

		public void setQuad(float startX, float startY, float endX, float endY) {
			this.quad[0] = new Point2D.Float(startX, startY);
			this.quad[1] = new Point2D.Float(endX, endY);
			final Point2D.Float ortho = calcOrtho(startX, startY,endX, endY, SimpleStaticNetLayer.cellWidth_m);
			this.quad[2] = new Point2D.Float(startX + ortho.x, startY + ortho.y);
			this.quad[3] = new Point2D.Float(endX + ortho.x, endY + ortho.y);
		}

		public void setColor(float coloridx) {
			this.coloridx = coloridx;
		}
	}

	protected OGLProvider myDrawer;
	protected static final Map<OGLProvider, Integer> netDisplListMap = new HashMap<OGLProvider, Integer>(); // not yet defined
	protected int netDisplList = -1;
	private static float cellWidth_m;

	@Override
	public void addItem(Receiver item) {
		if (netDisplList == -1) items.add((OTFDrawable)item);
	}

	public void drawNetList(){
		// make quad filled to hit every pixel/texel

		//System.out.print("DRAWING NET ONCE: objects count: " + items.size() );
		for (OTFDrawable item : items) {
			item.draw();
		}
	}

	protected void checkNetList(GL gl) {
		if (netDisplList == -1) {
			netDisplList = gl.glGenLists(1);
			gl.glNewList(netDisplList, GL.GL_COMPILE);
			drawNetList();
			gl.glEndList();
			items.clear();
			netDisplListMap.put(myDrawer, netDisplList);
		}
	}

	@Override
	public void draw() {
		GL gl = myDrawer.getGL();
		checkNetList(gl);

		Color netColor = ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getNetworkColor();
		float[] components = netColor.getColorComponents(new float[4]);
		gl.glColor4d(components[0], components[1], components[2], netColor.getAlpha() / 255.0f);
		gl.glCallList(netDisplList);
	}

	@Override
	public void init(SceneGraph graph) {
		cellWidth_m = ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth();
		myDrawer = (OGLProvider)graph.getDrawer();
		if (netDisplListMap.containsKey(myDrawer)) netDisplList = netDisplListMap.get(myDrawer);
		else  netDisplListMap.put(myDrawer, -1);
	}

	@Override
	public int getDrawOrder() {
		return 1;
	}

}
