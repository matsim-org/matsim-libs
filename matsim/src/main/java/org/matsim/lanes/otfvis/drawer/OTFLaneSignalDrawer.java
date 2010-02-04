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
package org.matsim.lanes.otfvis.drawer;

import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.opengl.GL;

import org.matsim.lanes.otfvis.io.OTFLaneData;
import org.matsim.lanes.otfvis.io.OTFLaneWriter;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;

/**
 *
 * @author dgrether
 */
public class OTFLaneSignalDrawer extends OTFGLDrawableImpl {

	private double startX, startY;
	private int numberOfQueueLanes;

	private Point2D.Double branchPoint = null;
	private OTFLaneData originalLaneData = null;
	private Map<String, OTFLaneData> laneData = new LinkedHashMap<String, OTFLaneData>();

	@Override
	public void onDraw( GL gl) {
		gl.glColor3d(0.0, 0.0, 1.0);
		double zCoord = 1.0;
		double offsetLinkEnd = 4.0;
		double offsetLinkStart = 2.5;
		// always draw a branch point
		gl.glBegin(GL.GL_QUADS);
			gl.glVertex3d(branchPoint.x - offsetLinkEnd, branchPoint.y - offsetLinkEnd, zCoord);
			gl.glVertex3d(branchPoint.x - offsetLinkEnd, branchPoint.y + offsetLinkEnd, zCoord);
			gl.glVertex3d(branchPoint.x + offsetLinkEnd, branchPoint.y + offsetLinkEnd, zCoord);
			gl.glVertex3d(branchPoint.x + offsetLinkEnd, branchPoint.y - offsetLinkEnd, zCoord);
		gl.glEnd();
		//draw a rect around linkStart
		gl.glBegin(GL.GL_QUADS);
			gl.glVertex3d(this.startX - offsetLinkStart, this.startY - offsetLinkStart, zCoord);
			gl.glVertex3d(this.startX - offsetLinkStart, this.startY + offsetLinkStart, zCoord);
			gl.glVertex3d(this.startX + offsetLinkStart, this.startY + offsetLinkStart, zCoord);
			gl.glVertex3d(this.startX + offsetLinkStart, this.startY - offsetLinkStart, zCoord);
		gl.glEnd();



	//draw lines between link start point and branch point
		gl.glBegin(GL.GL_LINES);
			gl.glVertex3d(this.startX, this.startY , zCoord);
			gl.glVertex3d(branchPoint.x, branchPoint.y, zCoord);
		gl.glEnd();

		//only draw lanes if there are more than one
		if (this.numberOfQueueLanes != 1) {
			for (OTFLaneData ld : this.laneData.values()){
				// draw connections between branch point and lane end
				gl.glColor3d(0.0, 0, 1.0);
				gl.glBegin(GL.GL_LINES);
	  			gl.glVertex3d(branchPoint.x, branchPoint.y, zCoord);
		  		gl.glVertex3d(ld.getEndPoint().x, ld.getEndPoint().y, zCoord);
  			gl.glEnd();

  			//draw link to link lines

  			if (OTFLaneWriter.DRAW_LINK_TO_LINK_LINES){
  				this.drawLinkToLinkLines(gl, ld, zCoord);
  			}

				if (ld.isGreen()) {
					gl.glColor3d(0.0, 1.0, 0.0);
				}
				else {
					gl.glColor3d(1.0, 0.0, 0.0);
				}
				// draw lane ends
				gl.glBegin(GL.GL_QUADS);
  			  gl.glVertex3d(ld.getEndPoint().x - offsetLinkEnd, ld.getEndPoint().y - offsetLinkEnd, zCoord);
	  		  gl.glVertex3d(ld.getEndPoint().x - offsetLinkEnd, ld.getEndPoint().y + offsetLinkEnd, zCoord);
		  	  gl.glVertex3d(ld.getEndPoint().x + offsetLinkEnd, ld.getEndPoint().y + offsetLinkEnd, zCoord);
			    gl.glVertex3d(ld.getEndPoint().x + offsetLinkEnd, ld.getEndPoint().y - offsetLinkEnd, zCoord);
   		  gl.glEnd();
			}
		}
		//also draw link to link lines if there is only one lane
		else if (OTFLaneWriter.DRAW_LINK_TO_LINK_LINES) {
			this.originalLaneData.setEndPoint(branchPoint.x, branchPoint.y);
			this.drawLinkToLinkLines(gl, this.originalLaneData, zCoord);
		}
	}


	private void drawLinkToLinkLines(GL gl, OTFLaneData ld, double zCoord){
		gl.glColor3d(1.0, 0.86, 0.0);
		for (Point2D.Double point : ld.getToLinkStartPoints()){
			gl.glBegin(GL.GL_LINES);
			  gl.glVertex3d(ld.getEndPoint().x, ld.getEndPoint().y, zCoord);
			  gl.glVertex3d(point.x, point.y, zCoord);
		  gl.glEnd();
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

	public void updateGreenState(String id, boolean green) {
		this.laneData.get(id).setGreen(green);
	}

	public Map<String, OTFLaneData> getLaneData() {
		return laneData;
	}

	public void setOriginalLaneData(OTFLaneData originalLaneData) {
		this.originalLaneData = originalLaneData;
	}
}
