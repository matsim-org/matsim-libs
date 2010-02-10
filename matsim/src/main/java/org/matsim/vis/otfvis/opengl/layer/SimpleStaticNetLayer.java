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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFDataQuadReceiver;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.gui.OTFDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OGLProvider;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;


/**
 * SimpleStaticNetLayer is the default network drawing layer.
 * It represents the network as uni-colored quads.
 * It is not dynamically changeable.
 *
 * @author dstrippgen
 *
 */
public class SimpleStaticNetLayer  extends SimpleSceneLayer{

	protected OGLProvider myDrawer;
	private static final Map<OGLProvider, Integer> netDisplListMap = new HashMap<OGLProvider, Integer>(); // not yet defined
	private static final Map<OGLProvider, List<OTFDrawable>> itemsListMap = new HashMap<OGLProvider, List<OTFDrawable>>(); // not yet defined

	protected int netDisplList = -1;
	public static float cellWidth_m = 30.f;

	public static Texture marktex = null;


	@Override
	public void addItem(OTFDataReceiver item) {
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

		float cellWidthAct_m = OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth();

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


	private void checkTexture() {
		if (marktex == null) {
			marktex = OTFOGLDrawer.createTexture(MatsimResource.getAsInputStream("mark.png"));
		}
	}

	@Override
	public void draw() {
		GL gl = myDrawer.getGL();

		checkNetList(gl);

		checkTexture();
		if (marktex != null) {
			marktex.enable();
			gl.glEnable(GL.GL_TEXTURE_2D);
			//gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
			marktex.bind();
		}
		//gl.glDisable(GL.GL_BLEND);
		Color netColor = OTFClientControl.getInstance().getOTFVisConfig().getNetworkColor();
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

		if (netDisplListMap.containsKey(myDrawer) && (netDisplList != -2)){
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

	 public static class SimpleQuadDrawer extends OTFGLDrawableImpl implements OTFDataQuadReceiver{

    private static final Logger log = Logger
        .getLogger(SimpleStaticNetLayer.SimpleQuadDrawer.class);

	    protected final Point2D.Float[] quad = new Point2D.Float[4];
	    protected float coloridx = 0;
	    protected char[] id;
	    protected int nrLanes;

	    public void onDraw2( GL gl) {
	      final Point2D.Float ortho = calcOrtho(this.quad[0].x, this.quad[0].y, this.quad[1].x, this.quad[1].y, nrLanes*SimpleStaticNetLayer.cellWidth_m);
	      this.quad[2] = new Point2D.Float(this.quad[0].x + ortho.x, this.quad[0].y + ortho.y);
	      this.quad[3] = new Point2D.Float(this.quad[1].x + ortho.x, this.quad[1].y + ortho.y);
	      //Draw quad
	      gl.glBegin(GL.GL_QUADS);
	      gl.glVertex3f(quad[0].x, quad[0].y, 0);
	      gl.glVertex3f(quad[1].x, quad[1].y, 0);
	      gl.glVertex3f(quad[3].x, quad[3].y, 0);
	      gl.glVertex3f(quad[2].x, quad[2].y, 0);
	      gl.glEnd();
	    }

	    public void onDraw( GL gl) {
	      final Point2D.Float ortho = calcOrtho(this.quad[0].x, this.quad[0].y, this.quad[1].x, this.quad[1].y, nrLanes*SimpleStaticNetLayer.cellWidth_m);
	      this.quad[2] = new Point2D.Float(this.quad[0].x + ortho.x, this.quad[0].y + ortho.y);
	      this.quad[3] = new Point2D.Float(this.quad[1].x + ortho.x, this.quad[1].y + ortho.y);
	      //Draw quad
	      TextureCoords co = new TextureCoords(0,0,1,1);
	      if(marktex != null) co =  marktex.getImageTexCoords();
	      gl.glBegin(GL.GL_QUADS);
	      gl.glTexCoord2f(co.right(),co.bottom()); gl.glVertex3f(quad[0].x, quad[0].y, 0);
	      gl.glTexCoord2f(co.right(),co.top()); gl.glVertex3f(quad[1].x, quad[1].y, 0);
	      gl.glTexCoord2f(co.left(), co.top()); gl.glVertex3f(quad[3].x, quad[3].y, 0);
	      gl.glTexCoord2f(co.left(),co.bottom()); gl.glVertex3f(quad[2].x, quad[2].y, 0);
	      gl.glEnd();
	    }

	    public void prepareLinkId(Map<Coord, String> linkIds) {
	      double alpha = 0.4;
	      double middleX = alpha*this.quad[0].x + (1.0-alpha)*this.quad[3].x;
	      double middleY = alpha*this.quad[0].y + (1.0-alpha)*this.quad[3].y;
	      //Point2D.Float anchor = SimpleStaticNetLayer.SimpleQuadDrawer.calcOrtho(fromX, fromY, middleX, middleY, cellWidth/2.);
	      String idstr = new String(id);
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
//	      log.error("setQuad...");
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

	  public static class NoQuadDrawer implements OTFDataQuadReceiver {
	    @Override
	    public void setQuad(float startX, float startY, float endX, float endY) {
	    }

	    @Override
	    public void setQuad(float startX, float startY, float endX, float endY, int nrLanes) {
	    }

	    public void onDraw(GL gl) {
	    }

      @Override
      public void setColor(float coloridx) {
      }

      @Override
      public void setId(char[] idBuffer) {
      }

      @Override
      public void invalidate(SceneGraph graph) {
      }
	  }

}
