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
package org.matsim.contrib.signals.otfvis;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.jogamp.opengl.GL2;

import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.vis.VisLane;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.lanes.vis.VisSignal;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawableReceiver;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;


public class OTFLaneSignalDrawer extends OTFGLAbstractDrawableReceiver {

//	private static final Logger log = Logger.getLogger(OTFLaneSignalDrawer.class);
	
	private static final int glListName = 2342;
	
	private static final double zCoord = 1.0;
	private static final double quadSizeLinkEnd = 5.0;
	private static final double quadSizeLinkStart = 3.5;
	private static final double quadSizeLaneStart = 3.5;
	private static final double quadSizeLaneEnd = 4;

	private Map<String, VisLinkWLanes> lanesLinkData =  new HashMap<String, VisLinkWLanes>();
	
	private Map<String, VisSignalSystem> signalSystems = new HashMap<String, VisSignalSystem>();

	private enum Color {LANECOLOR, GREEN, REDYELLOW, YELLOW, RED};
	
	private double currentLinkWidthCorrection = java.lang.Double.NEGATIVE_INFINITY;
	
	private int glNetList = -1;
	
	private VisLaneModelBuilder laneModelBuilder = new VisLaneModelBuilder();
	
	@Override
	public void onDraw(GL2 gl) {
		if (this.currentLinkWidthCorrection != OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth()){
			this.currentLinkWidthCorrection = OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth();
			this.recalculatePositions();
			gl.glDeleteLists(this.glNetList, 1);
			this.glNetList = -1;
		}
		if (this.glNetList < 0){
			this.updateNetList(gl);
		}
		gl.glCallList(this.glNetList);
		
		for (VisLinkWLanes laneLinkData : this.lanesLinkData.values()){
			this.drawLinkEndsAndSignals(gl, laneLinkData);
		}
	}
	
	@Override
	public void addToSceneGraph(SceneGraph graph) {
		graph.addItem(this);
	}
	
	private void updateNetList(GL2 gl){
		this.glNetList = gl.glGenLists(glListName);
		gl.glNewList(this.glNetList, GL2.GL_COMPILE);
		for (VisLinkWLanes laneLinkData : this.lanesLinkData.values()){
			this.drawLink(gl, laneLinkData);
		}
		gl.glEndList();
	}
	

	private void drawLinkEndsAndSignals(GL2 gl, VisLinkWLanes link) {
		if (link.getLaneData() != null) {
			for (VisLane ld : link.getLaneData().values()){
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
				this.drawToLinks(gl, link.getLinkEndCenterPoint(), link.getToLinks());
			}
		}
	}

	private void drawLink(GL2 gl, VisLinkWLanes link){
		//draw a rect around linkStart
		this.setColor(gl, Color.LANECOLOR);
		this.drawQuad(gl, link.getLinkStartCenterPoint(), quadSizeLinkStart);
		if (link.getLaneData() != null) { //draw the lanes
			for (VisLane ld : link.getLaneData().values()){
				this.drawLane(gl, ld, link);
			}
		}
		else { //draw the link
			this.drawVertex(gl, link.getLinkStartCenterPoint(), link.getLinkEndCenterPoint(), (float) (link.getNumberOfLanes() * 2));
		}
	}
	
	private void drawSignals(GL2 gl, Map<String, VisSignal> signals, Point2D.Double point, Point2D.Double ortho, List<VisLinkWLanes> toLinks){
		double dist = signals.size() - 1;
		Point2D.Double startPoint = this.laneModelBuilder.calcPoint(point, ortho, (quadSizeLinkEnd * -dist));
		int i = 0;
		for (VisSignal signal : signals.values()){
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
			startPoint = this.laneModelBuilder.calcPoint(point, ortho, (quadSizeLinkEnd * i));
		}
	}

	private void recalculatePositions() {
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLaneWidth(OTFClientControl.getInstance().getOTFVisConfig().getEffectiveLaneWidth());
		linkWidthCalculator.setLinkWidthForVis(OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth());
		for (VisLinkWLanes linkData : this.lanesLinkData.values()){
			this.laneModelBuilder.recalculatePositions(linkData, linkWidthCalculator);
		}
	}

	private void drawLaneEnd(GL2 gl, VisLane ld){
		this.setColor(gl, Color.LANECOLOR);
		this.drawQuad(gl, ld.getEndPoint(), quadSizeLaneEnd);
		if (!(ld.getToLanes() == null || ld.getToLanes().isEmpty())){
			for (VisLane toLane : ld.getToLanes()){
				this.drawVertex(gl, ld.getEndPoint(), toLane.getStartPoint(), 1.0f);
			}
		}
		
	}
	

	private void drawLane(GL2 gl, VisLane ld, VisLinkWLanes laneLinkData){
		//draw lane start
		this.setColor(gl, Color.LANECOLOR);
		this.drawQuad(gl, ld.getStartPoint(), quadSizeLaneStart);
		//draw line between lane start point and lane end point
		this.drawVertex(gl, ld.getStartPoint(), ld.getEndPoint(), ((float)ld.getNumberOfLanes()* 2));
	}

	
	private void drawToLinks(GL2 gl, Point2D.Double fromPoint, List<VisLinkWLanes> toLinks) {
		if (!(toLinks == null || toLinks.isEmpty())){
			for (VisLinkWLanes toLink : toLinks){
				this.drawVertex(gl, fromPoint, toLink.getLinkStartCenterPoint(), 1.0f);
			}
		}
	}


	private void drawVertex(GL2 gl, Point2D.Double startPoint, Point2D.Double endPoint, float lineWidth){
		gl.glLineWidth(lineWidth);
		gl.glBegin(GL2.GL_LINES);
		gl.glVertex3d(startPoint.x, startPoint.y , zCoord);
		gl.glVertex3d(endPoint.x, endPoint.y, zCoord);
		gl.glEnd();
	}
	
	private void drawQuad(GL2 gl, Point2D.Double point, double quadSize){
		gl.glBegin(GL2.GL_QUADS);
		gl.glVertex3d(point.x - quadSize, point.y - quadSize, zCoord);
		gl.glVertex3d(point.x - quadSize, point.y + quadSize, zCoord);
		gl.glVertex3d(point.x + quadSize, point.y + quadSize, zCoord);
		gl.glVertex3d(point.x + quadSize, point.y - quadSize, zCoord);
		gl.glEnd();
	}
	
	
	private void setColor(GL2 gl, Color color){
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


	public void addLaneLinkData(VisLinkWLanes laneLinkData){
		this.lanesLinkData.put(laneLinkData.getLinkId(), laneLinkData);
	}
	
	public Map<String, VisLinkWLanes> getLanesLinkData(){
		return this.lanesLinkData;
	}

	public void updateGreenState(String systemId, String groupId, SignalGroupState state) {
		this.signalSystems.get(systemId).getOTFSignalGroups().get(groupId).setState(state);
	}

	public void addOTFSignalSystem(VisSignalSystem otfsystem) {
		this.signalSystems .put(otfsystem.getId(), otfsystem);
	}
	
	
}


