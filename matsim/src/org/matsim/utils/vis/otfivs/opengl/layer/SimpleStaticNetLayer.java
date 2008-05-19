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

package org.matsim.utils.vis.otfivs.opengl.layer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.otfivs.caching.SceneGraph;
import org.matsim.utils.vis.otfivs.caching.SimpleSceneLayer;
import org.matsim.utils.vis.otfivs.data.OTFDataQuad;
import org.matsim.utils.vis.otfivs.data.OTFData.Receiver;
import org.matsim.utils.vis.otfivs.gui.OTFDrawable;
import org.matsim.utils.vis.otfivs.gui.OTFVisConfig;
import org.matsim.utils.vis.otfivs.opengl.drawer.OGLProvider;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFGLDrawable;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFGLDrawableImpl;


public class SimpleStaticNetLayer  extends SimpleSceneLayer{
	public static class SimpleQuadDrawer extends OTFGLDrawableImpl implements OTFDataQuad.Receiver, OTFGLDrawable{
		protected final Point2D.Float[] quad = new Point2D.Float[4];
		protected float coloridx = 0;

		public void onDraw( GL gl) {
			//Draw quad
			gl.glBegin(gl.GL_QUADS);
			gl.glVertex3f(quad[0].x, quad[0].y, 0);
			gl.glVertex3f(quad[1].x, quad[1].y, 0);
			gl.glVertex3f(quad[3].x, quad[3].y, 0);
			gl.glVertex3f(quad[2].x, quad[2].y, 0);
			gl.glEnd();
		}

		Point2D.Float calcOrtho(Point2D.Float start, Point2D.Float end){
			double dx = end.y - start.y;
			double dy = end.x -start.x;
			double sqr1 = Math.sqrt(dx*dx +dy*dy);
			final double cellWidth_m = ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth();

			dx = dx*cellWidth_m/sqr1;
			dy = -dy*cellWidth_m/sqr1;

			return new Point2D.Float((float)dx,(float)dy);
		}

		public void setQuad(float startX, float startY, float endX, float endY) {
			this.quad[0] = new Point2D.Float(startX, startY);
			this.quad[1] = new Point2D.Float(endX, endY);
			final Point2D.Float ortho = calcOrtho(this.quad[0], this.quad[1]);
			this.quad[2] = new Point2D.Float(startX + ortho.x, startY + ortho.y);
			this.quad[3] = new Point2D.Float(endX + ortho.x, endY + ortho.y);
		}

		public void setColor(float coloridx) {
			this.coloridx = coloridx;
		}
	}

	protected OGLProvider myDrawer;
	protected static Map<OGLProvider, Integer> netDisplListMap = new HashMap<OGLProvider, Integer>(); // not yet defined
	protected int netDisplList = -1;

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

	/* (non-Javadoc)
	 * @see playground.david.vis.data.SimpleSceneLayer#draw()
	 */
	@Override
	public void draw() {
		GL gl = myDrawer.getGL();
		checkNetList(gl);

		Color netColor = ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getNetworkColor();
		float[] components = netColor.getColorComponents(new float[4]);
		gl.glColor4d(components[0], components[1], components[2], netColor.getAlpha() / 255.0f);
		gl.glCallList(netDisplList);
	}

	/* (non-Javadoc)
	 * @see playground.david.vis.data.DefaultSceneLayer#init(playground.david.vis.data.SceneGraph, playground.david.vis.data.OTFClientQuad)
	 */
	@Override
	public void init(SceneGraph graph) {
		myDrawer = (OGLProvider)graph.getDrawer();
		if (netDisplListMap.containsKey(myDrawer)) netDisplList = netDisplListMap.get(myDrawer);
		else  netDisplListMap.put(myDrawer, -1);
	}

	/* (non-Javadoc)
	 * @see playground.david.vis.data.SimpleSceneLayer#getDrawOrder()
	 */
	@Override
	public int getDrawOrder() {
		return 1;
	}

}
