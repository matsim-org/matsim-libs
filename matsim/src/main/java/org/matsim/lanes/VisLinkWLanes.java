/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLanesLinkData
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
package org.matsim.lanes;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author dgrether
 *
 */
public final class VisLinkWLanes implements Serializable{

	private Point2D.Double linkStart = null;
	private Point2D.Double linkEnd = null;
	private Point2D.Double normalizedLinkVector;
	private Point2D.Double linkOrthogonalVector;
	private double numberOfLanes = 1.0;
	private int maximalAlignment = 0;
	private Map<String, VisLane> laneData =  null;
	private String id = null;
	private double linkWidth;
	private Point2D.Double linkStartCenterPoint = null;
	private Point2D.Double linkEndCenterPoint = null;
	private Map<String, VisSignal> signals = null;
	private ArrayList<String> toLinkIds;
	private transient List<VisLinkWLanes> toLinks = null;
	private Coord startCoord;
	private Coord endCoord;
	private double euklideanDistance;
	
	public VisLinkWLanes(String id){
		this.id = id;
	}
	
	public String getLinkId() {
		return this.id;
	}
	
	public void setNormalizedLinkVector(Point2D.Double v) {
		this.normalizedLinkVector = v;
	}
	
	public void setLinkOrthogonalVector(Point2D.Double v){
		this.linkOrthogonalVector = v;
	}
	
	public Point2D.Double getLinkStart() {
		return linkStart;
	}

	
	public Point2D.Double getLinkEnd() {
		return linkEnd;
	}

	
	public Double getNormalizedLinkVector() {
		return normalizedLinkVector;
	}

	
	public Double getLinkOrthogonalVector() {
		return linkOrthogonalVector;
	}

	public void setNumberOfLanes(double nrLanes) {
		this.numberOfLanes = nrLanes;
	}

	public double getNumberOfLanes() {
		return this.numberOfLanes;
	}

	public void setMaximalAlignment(int maxAlign) {
		this.maximalAlignment = maxAlign;
	}

	public int getMaximalAlignment(){
		return this.maximalAlignment;
	}
	
	public void addLaneData(VisLane laneData){
		if (this.laneData == null){
			this.laneData = new HashMap<String, VisLane>();
		}
		this.laneData .put(laneData.getId(), laneData);
	}
	
	public Map<String, VisLane> getLaneData(){
		return this.laneData;
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
	
	public void setLinkWidth(double linkWidth) {
		this.linkWidth = linkWidth;
	}
	
	public double getLinkWidth(){
		return this.linkWidth;
	}
	public void setLinkStartEndPoint(Double linkStart, Double linkEnd) {
		this.linkStart = linkStart;
		this.linkEnd = linkEnd;
		this.calcCoords();
	}

	public void setLinkStartCenterPoint(Double linkStartCenter) {
		this.linkStartCenterPoint = linkStartCenter;
	}

	public void setLinkEndCenterPoint(Double linkEndCenter) {
		this.linkEndCenterPoint = linkEndCenter;
	}

	public Coord getLinkStartCoord(){
		return this.startCoord;
	}
	
	public Coord getLinkEndCoord(){
		return this.endCoord;
	}
	
	private void calcCoords(){
		this.startCoord = new Coord(linkStart.x, linkStart.y);
		this.endCoord = new Coord(linkEnd.x, linkEnd.y);
		this.euklideanDistance = CoordUtils.calcEuclideanDistance(startCoord, endCoord);
	}
	
	public double getEuklideanDistance() {
		return euklideanDistance;
	}
	
	public Point2D.Double getLinkStartCenterPoint() {
		return this.linkStartCenterPoint;
	}
	
	public Point2D.Double getLinkEndCenterPoint() {
		return this.linkEndCenterPoint;
	}
	
	public void addToLink(VisLinkWLanes link){
		if (this.toLinks == null){
			this.toLinks = new ArrayList<VisLinkWLanes>();
		}
		this.toLinks.add(link);
	}

	public List<VisLinkWLanes> getToLinks() {
		return this.toLinks ;
	}
	
	public void addToLinkId(String toLinkId){
		if (this.toLinkIds == null)
			this.toLinkIds = new ArrayList<>();
		this.toLinkIds.add(toLinkId);
	}

	public List<String> getToLinkIds() {
		return toLinkIds ;
	}

}
