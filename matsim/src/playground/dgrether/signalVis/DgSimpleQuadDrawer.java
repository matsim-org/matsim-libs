/* *********************************************************************** *
 * project: org.matsim.*
 * DgSimpleQuadDrawer
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.signalVis;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;

import com.sun.opengl.util.texture.TextureCoords;


/**
 * @author dgrether
 *
 */
public class DgSimpleQuadDrawer extends SimpleStaticNetLayer.SimpleQuadDrawer {

	
	private static final Logger log = Logger.getLogger(DgSimpleQuadDrawer.class);
	private float startX, startY, endX, endY;
	private int nrLanes;
	private int numberOfQueueLanes;
	private Map<String, Double> laneData;
	
	public DgSimpleQuadDrawer() {
		log.debug("using DgSimpleQuadDrawer");
	}
	static float cellWidth_m = 30.f;
	
	@Override
	public void setQuad(float startX, float startY, float endX, float endY, int nrLanes){
		super.setQuad(startX, startY, endX, endY, nrLanes);
		this.startX = startX;
		this.startY = startY;
		this.endX = endX;
		this.endY = endY;
		this.nrLanes = nrLanes;
	}
	
	@Override
	public void onDraw( GL gl) {
		gl.glColor3d(0.2, 0.2, 0.2);
		final Point2D.Float ortho = calcOrtho(this.quad[0].x, this.quad[0].y, this.quad[1].x, this.quad[1].y, nrLanes* cellWidth_m);
		this.quad[2] = new Point2D.Float(this.quad[0].x + ortho.x, this.quad[0].y + ortho.y);
		this.quad[3] = new Point2D.Float(this.quad[1].x + ortho.x, this.quad[1].y + ortho.y);
		//Draw quad
		TextureCoords co = new TextureCoords(0,0,1,1);
		if(SimpleStaticNetLayer.marktex != null) co =  SimpleStaticNetLayer.marktex.getImageTexCoords();
		gl.glBegin(GL.GL_QUADS);
		gl.glTexCoord2f(co.right(),co.bottom()); gl.glVertex3f(quad[0].x, quad[0].y, 0);
		gl.glTexCoord2f(co.right(),co.top()); gl.glVertex3f(quad[1].x, quad[1].y, 0);
		gl.glTexCoord2f(co.left(), co.top()); gl.glVertex3f(quad[3].x, quad[3].y, 0);
		gl.glTexCoord2f(co.left(),co.bottom()); gl.glVertex3f(quad[2].x, quad[2].y, 0);
		gl.glEnd();
		
		gl.glColor3d(1.0, 0, 0);
		
		
		//draw lines between coordinates of nodes(?)
		gl.glBegin(GL.GL_LINES);
			gl.glVertex3d(this.startX, this.startY , 0.1); 
			gl.glVertex3d(this.endX, this.endY, 0.1); 
		gl.glEnd();

		//		
//		double x1, x2, y1, y2;
//		if (quad[0].x == quad[1].x){
//			x1 = (quad[0].x + quad[3].x) / 2;
//			y1 = quad[0].y;
//			x2 = (quad[1].x + quad[2].x) / 2;
//			y2 = quad[3].y;
//		}
//		else {
//			x1 = quad[0].x ;
//			y1 = (quad[0].y + quad[2].y) / 2;
//			x2 = quad[3].x;
//			y2 = (quad[1].x + quad[3].x) / 2;
//		}
//		
//		
//		gl.glBegin(GL.GL_LINES);
//		gl.glVertex3d(x1, y1 , 0.1); 
//		gl.glVertex3d(x2, y2, 0.1); 
//		
////		gl.glVertex3d(quad[0].x, quad[0].y , 0); 
////		gl.glVertex3d(quad[2].x, quad[2].y, 0); 
////		
////		gl.glVertex3d(quad[0].x, quad[0].y , 0); 
////		gl.glVertex3d(quad[3].x, quad[3].y, 0); 
////		
////		gl.glVertex3d(quad[2].x, quad[2].y , 0); 
////		gl.glVertex3d(quad[1].x, quad[1].y, 0); 
//		
//		gl.glEnd();
//		double offset = 2.0;
//	gl.glBegin(GL.GL_QUADS);
//	gl.glTexCoord2f(co.right(),co.bottom()); 
//	gl.glVertex3d(x1 - offset, y1 - offset, 0.1);
//	gl.glTexCoord2f(co.right(),co.top()); 
//	gl.glVertex3d(x1 - offset, y1 + offset, 0.1);
//	gl.glTexCoord2f(co.left(), co.top()); 
//	gl.glVertex3d(x1 + offset, y1 + offset, 0.1);
//	gl.glTexCoord2f(co.left(),co.bottom()); 
//	gl.glVertex3d(x1 + offset, y1 - offset, 0.1);
//	gl.glEnd();
//	
//	gl.glBegin(GL.GL_QUADS);
//	gl.glTexCoord2f(co.right(),co.bottom()); 
//	gl.glVertex3d(x2 - offset, y2 - offset, 0.1);
//	gl.glTexCoord2f(co.right(),co.top()); 
//	gl.glVertex3d(x2 - offset, y2 + offset, 0.1);
//	gl.glTexCoord2f(co.left(), co.top()); 
//	gl.glVertex3d(x2 + offset, y2 + offset, 0.1);
//	gl.glTexCoord2f(co.left(),co.bottom()); 
//	gl.glVertex3d(x2 + offset, y2 - offset, 0.1);
//	gl.glEnd();
//		
//		gl.glColor3d(0.2, 0.2, 0.2);
		
	}
	
	public void addQueueLaneData(String id, double meterfromlinkend){
		if (this.laneData == null) {
			this.laneData = new HashMap<String, Double>();
		}
		this.laneData.put(id, meterfromlinkend);
	}

	public void setNumberOfLanes(int nrQueueLanes) {
		this.numberOfQueueLanes = nrQueueLanes;
	}
}
