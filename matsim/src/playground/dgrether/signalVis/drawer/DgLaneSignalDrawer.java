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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;


/**
 * @author dgrether
 *
 */
public class DgLaneSignalDrawer extends OTFGLDrawableImpl {

	private static final Logger log = Logger.getLogger(DgLaneSignalDrawer.class);
	
	private double startX, startY;
	private int numberOfQueueLanes;

	private Point2D.Double branchPoint;
	private Map<String, LaneData> laneData;
	
	public DgLaneSignalDrawer() {
	}
	
	public void onDraw( GL gl) {
		gl.glColor3d(1.0, 0, 0);
		double zCoord = 1.0;
		double offset = 2.0;
		// always draw a branch point
		gl.glBegin(GL.GL_QUADS);
		gl.glVertex3d(branchPoint.x - offset, branchPoint.y - offset, zCoord);
		gl.glVertex3d(branchPoint.x - offset, branchPoint.y + offset, zCoord);
		gl.glVertex3d(branchPoint.x + offset, branchPoint.y + offset, zCoord);
		gl.glVertex3d(branchPoint.x + offset, branchPoint.y - offset, zCoord);
		gl.glEnd();
	//draw lines between link start point and branch point
		gl.glBegin(GL.GL_LINES);
			gl.glVertex3d(this.startX, this.startY , zCoord); 
			gl.glVertex3d(branchPoint.x, branchPoint.y, zCoord); 
		gl.glEnd();

		//only draw lanes if there are more than one
		if (this.numberOfQueueLanes != 1) {
			for (LaneData ld : this.laneData.values()){
				// draw connections between branch point and lane end
				gl.glBegin(GL.GL_LINES);
	  			gl.glVertex3d(branchPoint.x, branchPoint.y, zCoord); 
		  		gl.glVertex3d(ld.getEndPoint().x, ld.getEndPoint().y, zCoord); 
  			gl.glEnd();
				if (ld.isGreen()) {
					gl.glColor3d(0.0, 1.0, 0.0);
				}
				else {
					gl.glColor3d(1.0, 0.0, 0.0);
				}
				// draw lane ends
				gl.glBegin(GL.GL_QUADS);
  			  gl.glVertex3d(ld.getEndPoint().x - offset, ld.getEndPoint().y - offset, zCoord);
	  		  gl.glVertex3d(ld.getEndPoint().x - offset, ld.getEndPoint().y + offset, zCoord);
		  	  gl.glVertex3d(ld.getEndPoint().x + offset, ld.getEndPoint().y + offset, zCoord);
			    gl.glVertex3d(ld.getEndPoint().x + offset, ld.getEndPoint().y - offset, zCoord);
   		  gl.glEnd();
			}
		}
	}
	
	

	public void setNumberOfLanes(int nrQueueLanes) {
		this.numberOfQueueLanes = nrQueueLanes;
	}
	
	public void setMiddleOfLinkStart(double x, double y) {
		this.startX = x;
		this.startY = y;
	}

	public void setBranchPoint(double x, double y) {
		this.branchPoint = new Point2D.Double(x, y);
	}

	public void addNewQueueLaneData(String id, double endx, double endy) {
		if (this.laneData == null) {
			this.laneData = new LinkedHashMap<String, LaneData>();
		}
		LaneData ld = new LaneData();
		ld.setId(id);
		ld.setEndPoint(endx, endy);
		this.laneData.put(id, ld);
	}

	public void updateGreenState(String id, boolean green) {
		this.laneData.get(id).setGreen(green);
	}

	
	private static final class LaneData {
		private String id;
		private Point2D.Double endPoint;
		private boolean isGreen = false;

		public void setId(String id){
			this.id = id;
		}

		public void setEndPoint(double endx, double endy) {
			this.endPoint = new Point2D.Double(endx, endy);
		}

		public void setGreen(boolean isGreen) {
			this.isGreen = isGreen;
		}
		
		public boolean isGreen(){
			return this.isGreen ;
		}
		
		public Point2D.Double getEndPoint() {
			return endPoint;
		}
		
		public String getId() {
			return id;
		}
	}
}
