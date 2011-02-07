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
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import org.matsim.lanes.otfvis.io.OTFLane;
import org.matsim.lanes.otfvis.io.OTFLinkWLanes;
import org.matsim.signalsystems.model.SignalGroupState;
import org.matsim.signalsystems.otfvis.io.OTFSignal;
import org.matsim.signalsystems.otfvis.io.OTFSignalSystem;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawableReceiver;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;


public class OTFLaneSignalDrawer extends OTFGLAbstractDrawableReceiver {

//	private static final Logger log = Logger.getLogger(OTFLaneSignalDrawer.class);
	
	private static final int glListName = 2342;
	
	private static final double zCoord = 1.0;
	private static final double quadSizeLinkEnd = 6.0;
	private static final double quadSizeLinkStart = 3.5;
	private static final double quadSizeLaneStart = 3.5;
	private static final double quadSizeLaneEnd = 4.5;

	private Map<String, OTFLinkWLanes> lanesLinkData =  new HashMap<String, OTFLinkWLanes>();
	
	private Map<String, OTFSignalSystem> signalSystems = new HashMap<String, OTFSignalSystem>();

	private enum Color {LANECOLOR, GREEN, REDYELLOW, YELLOW, RED};
	
	private double currentCellWidth = java.lang.Double.NEGATIVE_INFINITY;
	
	private int glNetList = -1;
	
	@Override
	public void onDraw(GL gl) {
		if (this.currentCellWidth != OGLSimpleStaticNetLayer.getBasicLaneWidth_m()){
			this.currentCellWidth = OGLSimpleStaticNetLayer.getBasicLaneWidth_m();
			this.recalculatePositions();
			gl.glDeleteLists(this.glNetList, 1);
			this.glNetList = -1;
		}
		if (this.glNetList < 0){
			this.updateNetList(gl);
		}
		gl.glCallList(this.glNetList);
		
		for (OTFLinkWLanes laneLinkData : this.lanesLinkData.values()){
			this.drawLinkEndsAndSignals(gl, laneLinkData);
		}
	}
	
	private void updateNetList(GL gl){
		this.glNetList = gl.glGenLists(glListName);
		gl.glNewList(this.glNetList, GL.GL_COMPILE);
		for (OTFLinkWLanes laneLinkData : this.lanesLinkData.values()){
			this.drawLink(gl, laneLinkData);
		}
		gl.glEndList();
	}
	

	private void drawLinkEndsAndSignals(GL gl, OTFLinkWLanes link) {
		if (link.getLaneData() != null) {
			for (OTFLane ld : link.getLaneData().values()){
				if (ld.getSignals() != null){
					this.drawSignals(gl, ld.getSignals(), ld.getEndPoint(), link.getLinkOrthogonalVector(), ld.getToLinks());
				}
				else {
					this.setColor(gl, Color.LANECOLOR);
					this.drawLaneEnd(gl, ld);
					this.drawToLinks(gl, ld.getEndPoint(), ld.getToLinks());
				}
			}
		}
		else { //link end without lanes
			if (link.getSignals() != null){
				this.drawSignals(gl, link.getSignals(), link.getLinkEndCenterPoint(), link.getLinkOrthogonalVector(), link.getToLinks());
			}
			else {
				this.setColor(gl, Color.LANECOLOR);
				this.drawQuad(gl, link.getLinkEndCenterPoint(), quadSizeLinkEnd);
			}
		}
	}


	private void drawLink(GL gl, OTFLinkWLanes link){
		//draw a rect around linkStart
		this.setColor(gl, Color.LANECOLOR);
		this.drawQuad(gl, link.getLinkStartCenterPoint(), quadSizeLinkStart);
		if (link.getLaneData() != null) { //draw the lanes
			for (OTFLane ld : link.getLaneData().values()){
				this.drawLane(gl, ld, link);
			}
		}
		else { //draw the link
			this.drawVertex(gl, link.getLinkStartCenterPoint(), link.getLinkEndCenterPoint(), (float) (link.getNumberOfLanes() * 2));
		}
	}
	
	private void drawSignals(GL gl, List<OTFSignal> signals, Point2D.Double point, Point2D.Double ortho, List<OTFLinkWLanes> toLinks){
		double dist = signals.size() - 1;
		Point2D.Double startPoint = this.calcPoint(point, ortho, (quadSizeLinkEnd * -dist));
		int i = 0;
		for (OTFSignal signal : signals){
			i++;
			if (SignalGroupState.GREEN.equals(signal.getSignalGroupState())) {
				setColor(gl, Color.GREEN);
			}
			else if (SignalGroupState.RED.equals(signal.getSignalGroupState())) {
				this.setColor(gl, Color.RED);
			}
			else if (SignalGroupState.REDYELLOW.equals(signal.getSignalGroupState())) {
				this.setColor(gl, Color.REDYELLOW);
			}
			else if (SignalGroupState.YELLOW.equals(signal.getSignalGroupState())) {
				this.setColor(gl, Color.YELLOW);
			}
			else if (SignalGroupState.OFF.equals(signal.getSignalGroupState())){
				this.setColor(gl, Color.LANECOLOR);
			}
			this.drawQuad(gl, startPoint, quadSizeLinkEnd);
			if (!(signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty())){
				this.drawToLinks(gl, startPoint, signal.getTurningMoveRestrictions());
			}
			else{
				this.drawToLinks(gl, startPoint, toLinks);
			}
			startPoint = this.calcPoint(point, ortho, (quadSizeLinkEnd * i));
		}
	}

	private Point2D.Double calcPoint(Point2D.Double start, Point2D.Double vector, double distance){
		double x = start.getX() + (distance * vector.x);
		double y = start.getY() + (distance * vector.y);
		return new Point2D.Double(x, y);
	}

	
	private void recalculatePositions() {
		for (OTFLinkWLanes linkData : this.lanesLinkData.values()){
			double linkWidth = linkData.getNumberOfLanes() * OGLSimpleStaticNetLayer.getBasicLineWidth_m();
			linkData.setLinkWidth(linkWidth);
			double numberOfLinkParts = (2 * linkData.getMaximalAlignment()) + 2;
			Point2D.Double linkStartCenter = this.calculatePointOnLink(linkData, 0.0, 0.5);
			linkData.setLinkStartCenterPoint(linkStartCenter);
			if (linkData.getLaneData() == null || linkData.getLaneData().isEmpty()){
				//Calculate end point center
				double x = linkData.getLinkEnd().x + (0.5 * linkWidth * linkData.getLinkOrthogonalVector().x);
				double y = linkData.getLinkEnd().y + (0.5 * linkWidth * linkData.getLinkOrthogonalVector().y);
				linkData.setLinkEndCenterPoint(new Point2D.Double(x, y));
			}
			else {
				for (OTFLane lane : linkData.getLaneData().values()){
					double horizontalFraction = this.calculateWidthFraction(lane.getAlignment(), numberOfLinkParts);
					Point2D.Double laneStart = calculatePointOnLink(linkData, lane.getStartPosition(), horizontalFraction);
					Point2D.Double laneEnd = calculatePointOnLink(linkData, lane.getEndPosition(), horizontalFraction);
					lane.setStartPoint(laneStart);
					lane.setEndPoint(laneEnd);
				}
			}
		}
	}

	private void drawLaneEnd(GL gl, OTFLane ld){
		this.setColor(gl, Color.LANECOLOR);
		this.drawQuad(gl, ld.getEndPoint(), quadSizeLaneEnd);
		if (!(ld.getToLanes() == null || ld.getToLanes().isEmpty())){
			for (OTFLane toLane : ld.getToLanes()){
				this.drawVertex(gl, ld.getEndPoint(), toLane.getStartPoint(), 1.0f);
			}
		}
		
	}
	

	private void drawLane(GL gl, OTFLane ld, OTFLinkWLanes laneLinkData){
		//draw lane start
		this.setColor(gl, Color.LANECOLOR);
		this.drawQuad(gl, ld.getStartPoint(), quadSizeLaneStart);
		//draw line between lane start point and lane end point
		this.drawVertex(gl, ld.getStartPoint(), ld.getEndPoint(), ((float)ld.getNumberOfLanes()* 2));
	}

	
	private void drawToLinks(GL gl, Point2D.Double fromPoint, List<OTFLinkWLanes> toLinks) {
		if (!(toLinks == null || toLinks.isEmpty())){
			for (OTFLinkWLanes toLink : toLinks){
				this.drawVertex(gl, fromPoint, toLink.getLinkStartCenterPoint(), 1.0f);
			}
		}
	}


	private void drawVertex(GL gl, Point2D.Double startPoint, Point2D.Double endPoint, float lineWidth){
		gl.glLineWidth(lineWidth);
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3d(startPoint.x, startPoint.y , zCoord);
		gl.glVertex3d(endPoint.x, endPoint.y, zCoord);
		gl.glEnd();
	}
	
	private void drawQuad(GL gl, Point2D.Double point, double quadSize){
		gl.glBegin(GL.GL_QUADS);
		gl.glVertex3d(point.x - quadSize, point.y - quadSize, zCoord);
		gl.glVertex3d(point.x - quadSize, point.y + quadSize, zCoord);
		gl.glVertex3d(point.x + quadSize, point.y + quadSize, zCoord);
		gl.glVertex3d(point.x + quadSize, point.y - quadSize, zCoord);
		gl.glEnd();
	}
	
	private double calculateWidthFraction(int alignment, double numberOfLinkParts){
			return 0.5 - (alignment / numberOfLinkParts);
	}
	
	
	
	private Point2D.Double calculatePointOnLink(OTFLinkWLanes laneLinkData, double position, double horizontalFraction) {
		Point2D.Double lenghtPoint = this.calcPoint(laneLinkData.getLinkStart(), laneLinkData.getNormalizedLinkVector(), position);
		return this.calcPoint(lenghtPoint, laneLinkData.getLinkOrthogonalVector(), horizontalFraction * laneLinkData.getLinkWidth());
	}
	
	private void setColor(GL gl, Color color){
		switch (color) {
			case GREEN:
				gl.glColor3d(0.0, 1.0, 0.0);
				break;
			case RED:
				gl.glColor3d(1.0, 0.0, 0.0);
				break;
			case REDYELLOW:
				gl.glColor3d(1.0, 0.75, 0.0);
				break;
			case YELLOW:
				gl.glColor3d(1.0, 1.0, 0.0);
				break;
			case LANECOLOR:
				gl.glColor3d(1.0, 1.0, 0.8);
				break;
			default:
				gl.glColor3d(1.0, 1.0, 0.8);
				break;
		}
	}


	public void addLaneLinkData(OTFLinkWLanes laneLinkData){
		this.lanesLinkData.put(laneLinkData.getLinkId(), laneLinkData);
	}
	
	public Map<String, OTFLinkWLanes> getLanesLinkData(){
		return this.lanesLinkData;
	}

	public void updateGreenState(String systemId, String groupId, SignalGroupState state) {
		this.signalSystems.get(systemId).getOTFSignalGroups().get(groupId).setState(state);
	}

	public void addOTFSignalSystem(OTFSignalSystem otfsystem) {
		this.signalSystems .put(otfsystem.getId(), otfsystem);
	}
	
	
}


