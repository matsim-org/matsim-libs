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
import javax.vecmath.Point2d;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;

import playground.dgrether.signalVis.io.DgOtfLaneWriter;


/**
 * @author dgrether
 *
 */
public class DgLaneSignalDrawer extends OTFGLDrawableImpl {

	private static final Logger log = Logger.getLogger(DgLaneSignalDrawer.class);
	
	private double startX, startY;
	private int numberOfQueueLanes;

	private Point2D.Double branchPoint;
	private Map<String, DgOtfLaneData> laneData = new LinkedHashMap<String, DgOtfLaneData>();
	
	public DgLaneSignalDrawer() {
	}
	
	public void onDraw( GL gl) {
		gl.glColor3d(0.0, 0.0, 1.0);
		double zCoord = 1.0;
		double offset = 5.0;
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
			for (DgOtfLaneData ld : this.laneData.values()){
				// draw connections between branch point and lane end
				gl.glColor3d(0.0, 0, 1.0);
				gl.glBegin(GL.GL_LINES);
	  			gl.glVertex3d(branchPoint.x, branchPoint.y, zCoord); 
		  		gl.glVertex3d(ld.getEndPoint().x, ld.getEndPoint().y, zCoord); 
  			gl.glEnd();
  			
  			if (DgOtfLaneWriter.DRAW_LINK_TO_LINK_LINES){
  				for (Point2d point : ld.getToLinkStartPoints()){
  					gl.glBegin(GL.GL_LINES);
  					  gl.glVertex3d(ld.getEndPoint().x, ld.getEndPoint().y, zCoord); 
  	  			  gl.glVertex3d(point.x, point.y, zCoord); 
    			  gl.glEnd();
  				}
  			}
  			
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
	
	@Override
	public void invalidate(SceneGraph graph) {
		super.invalidate(graph);
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


	public void updateGreenState(String id, boolean green) {
		this.laneData.get(id).setGreen(green);
	}

	
	public Map<String, DgOtfLaneData> getLaneData() {
		return laneData;
	}
}
