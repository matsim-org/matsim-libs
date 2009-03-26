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

package org.matsim.vis.otfvis.opengl.layer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vis.netvis.VisConfig;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFDataQuad;
import org.matsim.vis.otfvis.data.OTFData.Receiver;
import org.matsim.vis.otfvis.gui.OTFDrawable;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.opengl.drawer.OGLProvider;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer.AgentDrawer;

import com.sun.opengl.util.texture.Texture;


public class SimpleStaticNetLayer  extends SimpleSceneLayer{

	public static class SimpleQuadDrawer extends OTFGLDrawableImpl implements OTFDataQuad.Receiver{
		
		protected final Point2D.Float[] quad = new Point2D.Float[4];
		protected float coloridx = 0;
		protected char[] id;
		protected int nrLanes;

		public void onDraw( GL gl) {
			final Point2D.Float ortho = calcOrtho(this.quad[0].x, this.quad[0].y, this.quad[1].x, this.quad[1].y, nrLanes*SimpleStaticNetLayer.cellWidth_m);
			this.quad[2] = new Point2D.Float(this.quad[0].x + ortho.x, this.quad[0].y + ortho.y);
			this.quad[3] = new Point2D.Float(this.quad[1].x + ortho.x, this.quad[1].y + ortho.y);
			//Draw quad
			gl.glBegin(GL.GL_QUADS);
			gl.glTexCoord2f(1,1); gl.glVertex3f(quad[0].x, quad[0].y, 0);
			gl.glTexCoord2f(1,0); gl.glVertex3f(quad[1].x, quad[1].y, 0);
			gl.glTexCoord2f(0,0); gl.glVertex3f(quad[3].x, quad[3].y, 0);
			gl.glTexCoord2f(0,1); gl.glVertex3f(quad[2].x, quad[2].y, 0);
			gl.glEnd();
		}

		public void prepareLinkId(Map<CoordImpl, String> linkIds) {
			// TODO Auto-generated method stub
			double alpha = 0.4; 
			double middleX = alpha*this.quad[0].x + (1.0-alpha)*this.quad[3].x;
			double middleY = alpha*this.quad[0].y + (1.0-alpha)*this.quad[3].y;
			//Point2D.Float anchor = SimpleStaticNetLayer.SimpleQuadDrawer.calcOrtho(fromX, fromY, middleX, middleY, cellWidth/2.);
			String idstr = new String(id);
//			if(idstr.equals("990990")) {
//				int i =0;
//				i++;
//			}
			linkIds.put(new CoordImpl(middleX , middleY ), idstr);
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
			setQuad(startX, startY,endX, endY, 1);
		}

		public void setQuad(float startX, float startY, float endX, float endY, int nrLanes) {
			this.quad[0] = new Point2D.Float(startX, startY);
			this.quad[1] = new Point2D.Float(endX, endY);
			this.nrLanes = nrLanes;
		}

		public void setColor(float coloridx) {
			this.coloridx = coloridx;
		}

		public void setId(char[] id) {
			this.id = id;
		}
	}

	public static class NoQuadDrawer extends SimpleQuadDrawer {
		public void setQuad(float startX, float startY, float endX, float endY) {
		}
	
		public void setQuad(float startX, float startY, float endX, float endY, int nrLanes) {
		}

		/* (non-Javadoc)
		 * @see org.matsim.utils.vis.otfvis.opengl.layer.SimpleStaticNetLayer.SimpleQuadDrawer#onDraw(javax.media.opengl.GL)
		 */
		@Override
		public void onDraw(GL gl) {
		}
}
	protected OGLProvider myDrawer;
	protected static final Map<OGLProvider, Integer> netDisplListMap = new HashMap<OGLProvider, Integer>(); // not yet defined
	protected static final Map<OGLProvider, List<OTFDrawable>> itemsListMap = new HashMap<OGLProvider, List<OTFDrawable>>(); // not yet defined
	
	protected int netDisplList = -1;
	protected static float cellWidth_m = -1.f;

	@Override
	public void addItem(Receiver item) {
		// only add items in initial run
		if (netDisplList < 0) items.add((OTFDrawable)item);
	}

	public void drawNetList(List<OTFDrawable> items){
		// make quad filled to hit every pixel/texel

		//System.out.print("DRAWING NET ONCE: objects count: " + items.size() );
		for (OTFDrawable item : items) {
			item.draw();
		}
	}

	protected void checkNetList(GL gl) {
		List<OTFDrawable> it = items;
		
		float cellWidthAct_m = ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getLinkWidth();
		
		if (netDisplListMap.containsKey(myDrawer) && (cellWidth_m != cellWidthAct_m)){
			int displList = netDisplListMap.get(myDrawer);
			gl.glDeleteLists(displList, 1);
			if(itemsListMap.containsKey(myDrawer))it = itemsListMap.get(myDrawer);
			netDisplList = -2;
		}
		
		if (netDisplList < 0) {
			cellWidth_m = cellWidthAct_m;
			netDisplList = gl.glGenLists(1);
			gl.glNewList(netDisplList, GL.GL_COMPILE);
			drawNetList(it);
			gl.glEndList();
			//items.clear();
			netDisplListMap.put(myDrawer, netDisplList);
			itemsListMap.put(myDrawer, it);
		}
	}

	Texture marktex = null;//

	private void checkTexture(GL gl) {
		if(marktex == null)marktex = OTFOGLDrawer.createTexture(MatsimResource.getAsInputStream("mark.png"));

	}
	
	@Override
	public void draw() {
		GL gl = myDrawer.getGL();
		checkNetList(gl);

		checkTexture(gl);
		if (marktex != null) {
			marktex.enable();
			gl.glEnable(GL.GL_TEXTURE_2D);
			//gl.glTexEnvf(GL.GL_POINT_SPRITE_ARB, GL.GL_COORD_REPLACE_ARB, GL.GL_TRUE);
			marktex.bind();	        	
		}
		Color netColor = ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).getNetworkColor();
		float[] components = netColor.getColorComponents(new float[4]);
		gl.glColor4d(components[0], components[1], components[2], netColor.getAlpha() / 255.0f);
		gl.glCallList(netDisplList);
		if (marktex != null ) {
			marktex.disable();	
		}
	}

	@Override
	public void init(SceneGraph graph, boolean initConstData) {
		myDrawer = (OGLProvider)graph.getDrawer();
		
		if (netDisplListMap.containsKey(myDrawer) && netDisplList != -2){
			netDisplList = netDisplListMap.get(myDrawer);

			if(initConstData) {
				itemsListMap.remove(myDrawer);
				items.clear();
				netDisplList = -2;
			}
		}
	}

	@Override
	public int getDrawOrder() {
		return 1;
	}

}
