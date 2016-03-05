/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.gui.processing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.lanes.vis.VisLane;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.lanes.vis.VisSignal;
import org.matsim.contrib.signals.otfvis.VisSignalGroup;
import org.matsim.contrib.signals.otfvis.VisSignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

import playground.dgrether.xvis.control.XVisControl;
import playground.dgrether.xvis.control.events.SignalGroupsSelectionEvent;
import playground.dgrether.xvis.control.events.SignalSystemSelectionEvent;
import playground.dgrether.xvis.control.handlers.SignalGroupsSelectionEventListener;
import playground.dgrether.xvis.control.handlers.SignalSystemSelectionEventListener;
import playground.dgrether.xvis.vismodel.VisScenario;
import processing.core.PApplet;


@SuppressWarnings("serial")
public class PWorldPanel extends PApplet  implements MouseWheelListener, SignalSystemSelectionEventListener, SignalGroupsSelectionEventListener {

	private static final double quadSizeLinkEnd = 5.0;

	private VisScenario visScenario;

	private float panTransformX = 0;
	private float panTransfromY = 0;

	private float wheelTransformX = 0;
	private float wheelTransformY = 0;

	private int mousePressedX = 0;
	private int mousePressedY = 0;

	private final Dimension preferredSize = new Dimension(600, 600);

	private Map<String, Set<String>> selectedVisSignalIdsBySignalSystemId = new HashMap<String, Set<String>>();

	private Set<Id> selectedSignalSystemIds = new HashSet<Id>();


	@Override
	public Dimension getPreferredSize() {
		return this.preferredSize;
	}

	@Override
	public void setup(){
		super.setup();
		this.addMouseWheelListener(this);
		this.setMinimumSize(this.getPreferredSize());
		this.frameRate = 10.0f;
	}

	@Override
	public void draw() {
		this.wheelTransformX =  this.getWidth()/2.0f;
		this.wheelTransformY =  this.getHeight()/2.0f;
		super.background(XVisControl.getInstance().getDrawingPreferences().getBackgroundColor().getRGB());
		super.translate(this.wheelTransformX, this.wheelTransformY);
		super.scale(XVisControl.getInstance().getDrawingPreferences().getRealScale());
		super.translate(panTransformX, panTransfromY);
		if (this.visScenario != null){
			this.drawNetwork();
		}
		this.highlightSelectedSignalSystems();
	}

	private void highlightSelectedSignalSystems() {
		this.stroke(XVisControl.getInstance().getDrawingPreferences().getSelectionColor().getRGB());
		this.strokeWeight(6.0f);
		this.noFill();
		for (Id systemId : this.selectedSignalSystemIds){
			VisSignalSystem visSystem = this.visScenario.getVisSignalSystemsByIdMap().get(systemId.toString());
			this.ellipse(visSystem.getVisCoordinate().x, visSystem.getVisCoordinate().y, 200.0f, 200.0f);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		float units = e.getUnitsToScroll();
		XVisControl.getInstance().getDrawingPreferences().incrementScale(- units / 10.0f);
	}

	@Override
	public void mousePressed(MouseEvent e){
		this.mousePressedX = e.getX();
		this.mousePressedY = e.getY();
	}

	@Override
	public void mouseDragged(MouseEvent e){
		super.mouseDragged(e);
		if ((mouseEvent.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			float scale = XVisControl.getInstance().getDrawingPreferences().getRealScale();
			this.panTransformX += 1/scale * (e.getX() - this.mousePressedX);
			this.panTransfromY +=  1/scale * (e.getY() - this.mousePressedY);
			this.mousePressedX = e.getX();
			this.mousePressedY = e.getY();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e){
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void drawNetwork() {
		for (VisLinkWLanes  link : this.visScenario.getLanesLinkData().values()){
			this.setColor(XVisControl.getInstance().getDrawingPreferences().getLinkColor());
			this.strokeWeight(1.0f);
			if (link.getLaneData() == null){
				if (XVisControl.getInstance().getDrawingPreferences().isShowLinkIds()){
					this.drawLinkLaneId(link.getLinkId(), link.getLinkStart(), link.getLinkOrthogonalVector(), 0.5f);
				}
				line((float)link.getLinkStartCenterPoint().x, (float)link.getLinkStartCenterPoint().y, (float)link.getLinkEndCenterPoint().x, (float)link.getLinkEndCenterPoint().y);
				if (link.getSignals() != null){
					this.drawSignals(link.getSignals(), link.getLinkEndCenterPoint(), link.getLinkOrthogonalVector(), link.getToLinks());
				}
			}
			else {
				for (VisLane ld : link.getLaneData().values()){
					this.drawLane(ld, link);
				}
			}
		}
	}

	private void setColor(Color c){
		this.stroke(c.getRGB());
		this.fill(c.getRGB());
	}

	private void drawSignals(Map<String, VisSignal> signals, Point2D.Double point, Point2D.Double ortho, List<VisLinkWLanes> toLinks){
		double dist = signals.size() - 1;
		Point2D.Double startPoint = this.calcPoint(point, ortho, (quadSizeLinkEnd * - dist));
		int i = 0;
		for (VisSignal signal : signals.values()){
			i++;
			if (SignalGroupState.GREEN.equals(signal.getSignalGroupState())) {
				this.setColor(XVisControl.getInstance().getDrawingPreferences().getGreenColor());
			}
			else if (SignalGroupState.RED.equals(signal.getSignalGroupState())) {
				this.setColor(XVisControl.getInstance().getDrawingPreferences().getRedColor());
			}
			else if (SignalGroupState.REDYELLOW.equals(signal.getSignalGroupState())) {
				this.setColor(XVisControl.getInstance().getDrawingPreferences().getRedYellowColor());
			}
			else if (SignalGroupState.YELLOW.equals(signal.getSignalGroupState())) {
				this.setColor(XVisControl.getInstance().getDrawingPreferences().getYellowColor());
			}
			else {
				this.setColor(XVisControl.getInstance().getDrawingPreferences().getLinkColor());
			}

			Set<String> selectedSignalIds = this.selectedVisSignalIdsBySignalSystemId.get(signal.getSignalSystemId());
			if (selectedSignalIds != null && selectedSignalIds.contains(signal.getId())){
				this.setColor(XVisControl.getInstance().getDrawingPreferences().getSelectionColor());
			}

			this.drawQuad(startPoint, quadSizeLinkEnd);
			if (!(signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty())){
				this.drawToLinks(startPoint, signal.getTurningMoveRestrictions());
			}
			else{
				this.drawToLinks(startPoint, toLinks);
			}
		}
		startPoint = this.calcPoint(point, ortho, (quadSizeLinkEnd * i));
	}

	private void drawToLinks(Point2D.Double fromPoint, List<VisLinkWLanes> toLinks) {
		if (XVisControl.getInstance().getDrawingPreferences().isShowLink2LinkLines()){
			if (!(toLinks == null || toLinks.isEmpty())){
				for (VisLinkWLanes toLink : toLinks){
					line((float)fromPoint.x, (float)fromPoint.y, (float)toLink.getLinkStartCenterPoint().x, (float)toLink.getLinkStartCenterPoint().y);
				}
			}
		}
	}


	private void drawQuad(Point2D.Double startPoint, double quadSize) {
		this.rect((float)(startPoint.x - quadSize/2), (float)(startPoint.y - quadSize/2), (float)quadSize, (float)quadSize);
	}

	private Point2D.Double calcPoint(Point2D.Double start, Point2D.Double vector, double distance){
		double x = start.getX() + (distance * vector.x);
		double y = start.getY() + (distance * vector.y);
		return new Point2D.Double(x, y);
	}


	private void drawLinkLaneId(String id, Point2D.Double startpoint, Point2D.Double orthogonalVector, float pos){
		float length = 5.0f;
		this.text(id, (float)startpoint.x + ((float)orthogonalVector.x * pos * length), (float)startpoint.y - ((float)orthogonalVector.y * pos * length));
	}


	private void drawLane(VisLane ld, VisLinkWLanes laneLinkData){
		this.setColor(XVisControl.getInstance().getDrawingPreferences().getLinkColor());
		if (XVisControl.getInstance().getDrawingPreferences().isShowLaneIds()){
			this.drawLinkLaneId(ld.getId().toString(), ld.getStartPoint(), laneLinkData.getLinkOrthogonalVector(), 0.5f);
		}
		line((float)ld.getStartPoint().x, (float)ld.getStartPoint().y,(float) ld.getEndPoint().x, (float)ld.getEndPoint().y);

		if (ld.getSignals() != null){
			this.drawSignals(ld.getSignals(), ld.getEndPoint(), laneLinkData.getLinkOrthogonalVector(), ld.getToLinks());
		}
		//draw lane2 lane lines
		if (!(ld.getToLanes() == null || ld.getToLanes().isEmpty())){
			for (VisLane toLane : ld.getToLanes()){
				line((float)ld.getEndPoint().x, (float)ld.getEndPoint().y, (float)toLane.getStartPoint().x, (float)toLane.getStartPoint().y);
			}
		}
		if (!(ld.getToLinks() == null || ld.getToLinks().isEmpty())){
			this.drawToLinks(ld.getEndPoint(), ld.getToLinks());
		}
	}

	public void showNetwork(VisScenario visNet) {
		this.visScenario = visNet;
		this.panTransformX = - (visNet.getBoundingBoxMax().x + visNet.getBoundingBoxMin().x) /2;
		this.panTransfromY = - (visNet.getBoundingBoxMax().y - visNet.getBoundingBoxMin().y) /2;
		this.calcCenterScale();
		this.redraw();
	}

	private void calcCenterScale() {
		double panelHeight = this.getSize().getHeight();
		float netHeight = this.visScenario.getBoundingBoxMax().y - this.visScenario.getBoundingBoxMin().y;
		XVisControl.getInstance().getDrawingPreferences().setRealScale((float) (panelHeight/netHeight));
	}

	@Override
	public void handleEvent(SignalGroupsSelectionEvent e) {
		if (e.doOverwriteSelection()){
			this.selectedVisSignalIdsBySignalSystemId.clear();
		}
		for (Id systemId : e.getSelectedSignalGroupIdsBySystemId().keySet()){
			VisSignalSystem visSignalSystem = this.visScenario.getVisSignalSystemsByIdMap().get(systemId.toString());
			for (Id signalGroupId : e.getSelectedSignalGroupIdsBySystemId().get(systemId)){
				VisSignalGroup visSignalGroup = visSignalSystem.getOTFSignalGroups().get(signalGroupId.toString());
				this.selectedVisSignalIdsBySignalSystemId.put(visSignalSystem.getId().toString(), visSignalGroup.getSignals().keySet());
			}
		}
	}

	@Override
	public void handleEvent(SignalSystemSelectionEvent e) {
		if (e.doOverwriteSelection()){
			this.selectedSignalSystemIds.clear();
		}
		this.selectedSignalSystemIds .addAll(e.getSignalSystemIds());
	}

}
