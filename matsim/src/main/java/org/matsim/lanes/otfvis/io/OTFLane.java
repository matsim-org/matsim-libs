/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLaneData2
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
package org.matsim.lanes.otfvis.io;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.otfvis.io.OTFSignal;


/**
 * @author dgrether
 */
public class OTFLane {
	private String id = null;
	private double endPosition;
	private double startPosition;
	private int alignment;
	private double numberOfLanes;
	private List<OTFLinkWLanes> toLinksData = null;
	private List<OTFLane> toLanes = null;
	private SignalGroupState state = null;
	private Point2D.Double startPoint = null;
	private Point2D.Double endPoint = null;
	private List<OTFSignal> signals = null;
	
	public OTFLane(String id) {
		this.id = id;
	}
	
	public void setId(String id){
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setEndPosition(double pos) {
		this.endPosition = pos;
	}
	
	public double getEndPosition() {
		return endPosition;
	}

	
	public double getStartPosition() {
		return startPosition;
	}

	
	public void setStartPosition(double pos) {
		this.startPosition = pos;
	}

	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}
	
	public int getAlignment(){
		return this.alignment;
	}
	
	public double getNumberOfLanes() {
		return this.numberOfLanes;
	}
	
	public void setNumberOfLanes(double noLanes){
		this.numberOfLanes = noLanes;
	}

	public void setSignalGroupState(SignalGroupState state) {
		this.state = state;
	}
	
	public SignalGroupState getSignalGroupState(){
		return this.state ;
	}

	public void addToLink(OTFLinkWLanes toLink) {
		if (this.toLinksData == null) {
			this.toLinksData = new ArrayList<OTFLinkWLanes>();
		}
		this.toLinksData.add(toLink);
	}

	public void addToLane(OTFLane toLane) {
		if (this.toLanes == null){
			this.toLanes = new ArrayList<OTFLane>();
		}
		this.toLanes.add(toLane);
	}

	
	public Point2D.Double getStartPoint() {
		return startPoint;
	}

	
	public void setStartPoint(Point2D.Double startPoint) {
		this.startPoint = startPoint;
	}

	
	public Point2D.Double getEndPoint() {
		return endPoint;
	}

	
	public void setEndPoint(Point2D.Double endPoint) {
		this.endPoint = endPoint;
	}

	public void addSignal(OTFSignal signal) {
		if (this.signals == null){
			this.signals = new ArrayList<OTFSignal>();
		}
		this.signals.add(signal);
	}
	
	public List<OTFSignal> getSignals(){
		return this.signals;
	}

	
	public List<OTFLinkWLanes> getToLinks() {
		return toLinksData;
	}

	
	public List<OTFLane> getToLanes() {
		return toLanes;
	}

}



