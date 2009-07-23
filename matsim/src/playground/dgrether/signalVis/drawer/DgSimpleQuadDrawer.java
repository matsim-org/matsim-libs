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
package playground.dgrether.signalVis.drawer;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

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

	private Point2D.Double _branchPoint;
	private List<LaneData> _laneData;
	
	public DgSimpleQuadDrawer() {
	}
	
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
		final Point2D.Float ortho = calcOrtho(this.quad[0].x, this.quad[0].y, this.quad[1].x, this.quad[1].y, nrLanes* SimpleStaticNetLayer.cellWidth_m);
		this.quad[2] = new Point2D.Float(this.quad[0].x + ortho.x, this.quad[0].y + ortho.y);
		this.quad[3] = new Point2D.Float(this.quad[1].x + ortho.x, this.quad[1].y + ortho.y);
//		log.debug("ortho.x " + ortho.x + " ortho.y " + ortho.y);
		//Draw quad
		TextureCoords co = new TextureCoords(0,0,1,1);
		if(SimpleStaticNetLayer.marktex != null) co =  SimpleStaticNetLayer.marktex.getImageTexCoords();
//		gl.glBegin(GL.GL_QUADS);
//		gl.glTexCoord2f(co.right(),co.bottom()); gl.glVertex3f(quad[0].x, quad[0].y, 0);
//		gl.glTexCoord2f(co.right(),co.top()); gl.glVertex3f(quad[1].x, quad[1].y, 0);
//		gl.glTexCoord2f(co.left(), co.top()); gl.glVertex3f(quad[3].x, quad[3].y, 0);
//		gl.glTexCoord2f(co.left(),co.bottom()); gl.glVertex3f(quad[2].x, quad[2].y, 0);
//		gl.glEnd();
		
		gl.glColor3d(1.0, 0, 0);
		
		double linkWidthX = ortho.x;
		double linkWidhtY = ortho.y;
		
		double dx = this.endX - this.startX;
		double dy = this.endY - this.startY;
		double sqrt = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
		double dxNorm = dx / sqrt;
		double dyNorm = dy / sqrt;
		double zCoord = 1.0;
		
		
		double branchPoint = 0.0;
		double branchPointX = 0.0;
		double branchPointY = 0.0;
		double offset = 2.0;
		
		if (this.numberOfQueueLanes != 1) {
			gl.glBegin(GL.GL_QUADS);
			  gl.glVertex3d(_branchPoint.x - offset, _branchPoint.y - offset, zCoord);
			  gl.glVertex3d(_branchPoint.x - offset, _branchPoint.y + offset, zCoord);
			  gl.glVertex3d(_branchPoint.x + offset, _branchPoint.y + offset, zCoord);
			  gl.glVertex3d(_branchPoint.x + offset, _branchPoint.y - offset, zCoord);
		  gl.glEnd();
			
		//draw lines between coordinates of nodes(?)
			gl.glBegin(GL.GL_LINES);
				gl.glVertex3d(this.startX, this.startY , zCoord); 
				gl.glVertex3d(_branchPoint.x, _branchPoint.y, zCoord); 
			gl.glEnd();
			
			for (LaneData ld : this._laneData){
				gl.glBegin(GL.GL_QUADS);
  			  gl.glVertex3d(ld.getEndPoint().x - offset, ld.getEndPoint().y - offset, zCoord);
	  		  gl.glVertex3d(ld.getEndPoint().x - offset, ld.getEndPoint().y + offset, zCoord);
		  	  gl.glVertex3d(ld.getEndPoint().x + offset, ld.getEndPoint().y + offset, zCoord);
			    gl.glVertex3d(ld.getEndPoint().x + offset, ld.getEndPoint().y - offset, zCoord);
   		  gl.glEnd();
			}
		}
		
//		if (this.laneData != null){
//			for (String key : this.laneData.keySet()) {
//				branchPoint = this.laneData.get(key);
//				if (branchPoint == 0.0){
//					continue;
//				}
//				log.debug("lane data not null, meter from linkend: " + branchPoint);
//				log.debug("startx: " + this.startX + " endx: " + this.endX);
//				log.debug("startY: " + this.startY + " endY: " + this.endY);
//				branchPointX = this.startX + (sqrt - branchPoint) * dxNorm;
//				branchPointY = this.startY + (sqrt - branchPoint) * dyNorm;
//				log.debug("branchPoint: (" + branchPointX + ", " + branchPointY +")");
//				double offset = 2.0;
//				gl.glBegin(GL.GL_QUADS);
//					gl.glVertex3d(branchPointX - offset, branchPointY - offset, zCoord);
//					gl.glVertex3d(branchPointX - offset, branchPointY + offset, zCoord);
//					gl.glVertex3d(branchPointX + offset, branchPointY + offset, zCoord);
//					gl.glVertex3d(branchPointX + offset, branchPointY - offset, zCoord);
//				gl.glEnd();
//			}
//			
//			log.debug("Width of quad: " + (nrLanes * cellWidth_m));
//			double distanceLanes = (nrLanes * cellWidth_m) / (this.nrLanes + 2);
//			log.debug("distance of lanes : " + distanceLanes);
//			Point2D.Double normalizedOrthoV = calcNormalizedOrthogonalVector(this.quad[0].x, this.quad[0].y, this.quad[1].x, this.quad[1].y);
//			for (int i = 0; i < this.nrLanes; i++){
//				
//			}
//		}
		

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
	
	

	public void setNumberOfLanes(int nrQueueLanes) {
		this.numberOfQueueLanes = nrQueueLanes;
	}

	public void setBranchPoint(double x, double y) {
		this._branchPoint = new Point2D.Double(x, y);
	}

	public void addNewQueueLaneData(String id, double endx, double endy) {
		if (this._laneData == null) {
			this._laneData = new ArrayList<LaneData>();
		}
		LaneData ld = new LaneData();
		ld.setId(id);
		ld.setEndPoint(endx, endy);
		this._laneData.add(ld);
	}
	
	private static final class LaneData {
		private String id;
		private Point2D.Double endPoint;

		public void setId(String id){
			this.id = id;
		}

		public void setEndPoint(double endx, double endy) {
			this.endPoint = new Point2D.Double(endx, endy);
		}

		
		public Point2D.Double getEndPoint() {
			return endPoint;
		}

		
		public void setEndPoint(Point2D.Double endPoint) {
			this.endPoint = endPoint;
		}

		
		public String getId() {
			return id;
		}
	};
}
