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
package org.matsim.lanes.vis;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author dgrether
 */
public class VisLane implements Serializable {
	
	private String id = null;
	private double endPosition;
	private double startPosition;
	private int alignment;
	private double numberOfLanes;
	private List<VisLane> toLanes = null;
	private SignalGroupState state = null;
	private Point2D.Double startPoint = null;
	private Point2D.Double endPoint = null;
	private Map<String, VisSignal> signals = null;
	private List<String> toLinkIds;
	private transient List<VisLinkWLanes> toLinksData = null;
	private double euklideanDistance;
	private Coord startCoord;
	private Coord endCoord; 
	private Map<Integer, Tuple<Coord, Coord>> drivingLaneMap = null;
	
	public VisLane(String id) {
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

	public void addToLink(VisLinkWLanes toLink) {
		if (this.toLinksData == null) {
			this.toLinksData = new ArrayList<VisLinkWLanes>();
		}
		this.toLinksData.add(toLink);
	}

	public void addToLane(VisLane toLane) {
		if (this.toLanes == null){
			this.toLanes = new ArrayList<VisLane>();
		}
		this.toLanes.add(toLane);
	}

	
	public Point2D.Double getStartPoint() {
		return startPoint;
	}

	
	public void setStartEndPoint(Point2D.Double startPoint, Point2D.Double endPoint) {
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		this.calcCoords();
	}
	
	public Point2D.Double getEndPoint() {
		return endPoint;
	}
	
	private void calcCoords(){
		this.startCoord = new Coord(startPoint.x, startPoint.y);
		this.endCoord = new Coord(endPoint.x, endPoint.y);
		this.euklideanDistance = CoordUtils.calcEuclideanDistance(startCoord, endCoord);
	}

	public void addDrivingLane(int laneNumber, Point2D.Double  drivingLaneStart, Point2D.Double drivingLaneEnd) {
		if (this.drivingLaneMap == null){
			this.drivingLaneMap = new HashMap<Integer, Tuple<Coord, Coord>>();
		}
		Tuple<Coord, Coord> tuple = new Tuple<Coord, Coord>(new Coord(drivingLaneStart.x, drivingLaneStart.y), new Coord(drivingLaneEnd.x, drivingLaneEnd.y));
		this.drivingLaneMap.put(laneNumber, tuple);
	}
	
	public Tuple<Coord, Coord> getDrivingLaneStartEndCoord(int laneNumber){
		return this.drivingLaneMap.get(laneNumber);
	}

	
	public Coord getStartCoord() {
		return this.startCoord;
	}

	public Coord getEndCoord() {
		return this.endCoord;
	}

	public void addSignal(VisSignal signal) {
		if (this.signals == null){
			this.signals = new HashMap<String, VisSignal>();
		}
		this.signals.put(signal.getId(), signal);
	}
	
	public Map<String, VisSignal> getSignals(){
		return this.signals;
	}

	
	public List<VisLinkWLanes> getToLinks() {
		return toLinksData;
	}
	
	public void addToLinkId(String toLinkId){
		if (this.toLinkIds == null)
			this.toLinkIds = new ArrayList<>();
		this.toLinkIds.add(toLinkId);
	}

	public List<String> getToLinkIds() {
		return toLinkIds ;
	}
	
	public List<VisLane> getToLanes() {
		return toLanes;
	}

	
	public double getEuklideanDistance() {
		return euklideanDistance;
	}



}



