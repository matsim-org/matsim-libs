/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLaneSignalDrawer2
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;
import org.matsim.lanes.otfvis.io.LaneToLinkData;
import org.matsim.lanes.otfvis.io.OTFLaneData2;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;


public class OTFLaneSignalDrawer2 extends OTFGLDrawableImpl {

	private static final Logger log = Logger.getLogger(OTFLaneSignalDrawer2.class);
	
	private static final double zCoord = 1.0;
	private static final double offsetLinkEnd = 6.0;
	private static final double offsetLinkStart = 4.5;
	private static final double offsetLaneStart = 4.5;
	private static final double offsetLaneEnd = 6.0;

	private Map<String, OTFLaneData2> laneData = new HashMap<String, OTFLaneData2>();
	
	private OTFLanesLinkData lanesLinkData =  new OTFLanesLinkData();
	
	private double linkWidth;

	private double numberOfLinkParts;
	
	@Override
	public void onDraw(GL gl) {
		linkWidth = this.lanesLinkData.getNumberOfLanes() * SimpleStaticNetLayer.cellWidth_m;
		numberOfLinkParts = (2 * this.lanesLinkData.getMaximalAlignment()) + 2;
		Point2D.Double linkStartCenter = this.calculatePointOnLink(0.0, 0.5);
		//draw a rect around linkStart
		gl.glColor3d(0.0, 0.0, 1.0);
		gl.glBegin(GL.GL_QUADS);
		gl.glVertex3d(linkStartCenter.x - offsetLinkStart, linkStartCenter.y - offsetLinkStart, zCoord);
		gl.glVertex3d(linkStartCenter.x - offsetLinkStart, linkStartCenter.y + offsetLinkStart, zCoord);
		gl.glVertex3d(linkStartCenter.x + offsetLinkStart, linkStartCenter.y + offsetLinkStart, zCoord);
		gl.glVertex3d(linkStartCenter.x + offsetLinkStart, linkStartCenter.y - offsetLinkStart, zCoord);
		gl.glEnd();
		
		
		gl.glColor3d(1.0, 1.0, 0.8);
		for (OTFLaneData2 ld : this.laneData.values()){
			double horizontalFraction = this.calculateWidthFraction(ld.getAlignment());
			Point2D.Double laneStart = calculatePointOnLink(ld.getStartPoint(), horizontalFraction);
			Point2D.Double laneEnd = calculatePointOnLink(ld.getEndPoint(), horizontalFraction);
			//draw lane start
			gl.glBegin(GL.GL_QUADS);
			gl.glVertex3d(laneStart.x - offsetLaneStart, laneStart.y - offsetLaneStart, zCoord);
			gl.glVertex3d(laneStart.x - offsetLaneStart, laneStart.y + offsetLaneStart, zCoord);
			gl.glVertex3d(laneStart.x + offsetLaneStart, laneStart.y + offsetLaneStart, zCoord);
			gl.glVertex3d(laneStart.x + offsetLaneStart, laneStart.y - offsetLaneStart, zCoord);
			gl.glEnd();

			//draw line between lane start point and lane end point
			gl.glLineWidth((float)ld.getNumberOfLanes() * 2);
			gl.glBegin(GL.GL_LINES);
			gl.glVertex3d(laneStart.x, laneStart.y , zCoord);
			gl.glVertex3d(laneEnd.x, laneEnd.y, zCoord);
			gl.glEnd();
			gl.glLineWidth(1.0f);
			
			if (ld.getSignalGroupState() != null) {
				if (SignalGroupState.GREEN.equals(ld.getSignalGroupState())) {
					gl.glColor3d(0.0, 1.0, 0.0);
				}
				else if (SignalGroupState.RED.equals(ld.getSignalGroupState())) {
					gl.glColor3d(1.0, 0.0, 0.0);
				}
				else if (SignalGroupState.REDYELLOW.equals(ld.getSignalGroupState())) {
					gl.glColor3d(1.0, 0.75, 0.0);
				}
				else if (SignalGroupState.YELLOW.equals(ld.getSignalGroupState())) {
					gl.glColor3d(1.0, 1.0, 0.0);
				}
			}
			
			//draw lane end
			gl.glBegin(GL.GL_QUADS);
			gl.glVertex3d(laneEnd.x - offsetLaneEnd, laneEnd.y - offsetLaneEnd, zCoord);
			gl.glVertex3d(laneEnd.x - offsetLaneEnd, laneEnd.y + offsetLaneEnd, zCoord);
			gl.glVertex3d(laneEnd.x + offsetLaneEnd, laneEnd.y + offsetLaneEnd, zCoord);
			gl.glVertex3d(laneEnd.x + offsetLaneEnd, laneEnd.y - offsetLaneEnd, zCoord);
			gl.glEnd();
			
			//reset color
			gl.glColor3d(1.0, 1.0, 0.8);
			//draw link to link lines
			double x, y;
			if (ld.getLaneToLinkData() != null) {
				for (LaneToLinkData toLinkData : ld.getLaneToLinkData()){
					Point2D.Double startOfToLink = toLinkData.getStartPoint();	
					x = startOfToLink.x + (0.5 * toLinkData.getNumberOfLanes() *  SimpleStaticNetLayer.cellWidth_m * toLinkData.getNormalVector().x);
					y = startOfToLink.y + (0.5 * toLinkData.getNumberOfLanes() *  SimpleStaticNetLayer.cellWidth_m * toLinkData.getNormalVector().y);
					gl.glBegin(GL.GL_LINES);
					gl.glVertex3d(laneEnd.x, laneEnd.y , zCoord);
					gl.glVertex3d(x, y, zCoord);
					gl.glEnd();
				}
			}
		}
	}

	private double calculateWidthFraction(int alignment){
			return 0.5 - (alignment / numberOfLinkParts);
	}
	
	
	
	private Point2D.Double calculatePointOnLink(double startPointX, double horizontalFraction) {
		double x = this.lanesLinkData.getLinkStart().x + (startPointX * this.lanesLinkData.getNormalizedLinkVector().x);
		double y = this.lanesLinkData.getLinkStart().y + (startPointX * this.lanesLinkData.getNormalizedLinkVector().y);
		x = x + (horizontalFraction * this.linkWidth * this.lanesLinkData.getLinkOrthogonalVector().x);
		y = y + (horizontalFraction * this.linkWidth * this.lanesLinkData.getLinkOrthogonalVector().y);
		return new Point2D.Double(x, y);
	}

	public void updateGreenState(String id, SignalGroupState state) {
		OTFLaneData2 lane = this.laneData.get(id);
		if (lane != null) {
		  lane.setSignalGroupState(state);
		}
		else {
		  log.error("lane data is null for id " + id + " and drawer " + this);
		}
	}



	public Map<String, OTFLaneData2> getLaneData() {
		return laneData;
	}

	public void addLaneData(OTFLaneData2 data) {
		this.laneData.put(data.getId(), data);
	}
	
	public OTFLanesLinkData getLanesLinkData(){
		return this.lanesLinkData;
	}
	
	
}


