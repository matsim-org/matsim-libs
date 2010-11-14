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
package org.matsim.lanes.otfvis.io;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.signalsystems.otfvis.io.OTFSignal;


/**
 * @author dgrether
 *
 */
public class OTFLinkWLanes {

	private Point2D.Double linkStart = null;
	private Point2D.Double linkEnd = null;
	private Double normalizedLinkVector;
	private Double linkOrthogonalVector;
	private double numberOfLanes = 1.0;
	private int maximalAlignment = 0;
	private Map<String, OTFLane> laneData =  null;
	private String id = null;
	private double linkWidth;
	private Point2D.Double linkStartCenterPoint = null;
	private Point2D.Double linkEndCenterPoint = null;
	private List<OTFSignal> signals = null;
	private List<OTFLinkWLanes> toLinks = null;
	
	
	public OTFLinkWLanes(String id){
		this.id = id;
	}
	
	public String getLinkId() {
		return this.id;
	}
	
	public void setLinkStart(double x, double y) {
		this.linkStart = new Point2D.Double(x, y);
	}

	public void setLinkEnd(double x, double y) {
		this.linkEnd = new Point2D.Double(x, y);
	}

	public void setNormalizedLinkVector(double x, double y) {
		this.normalizedLinkVector = new Point2D.Double(x, y);
	}
	
	public void setLinkOrthogonalVector(double x, double y){
		this.linkOrthogonalVector = new Point2D.Double(x, y);
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
	
	public void addLaneData(OTFLane laneData){
		if (this.laneData == null){
			this.laneData = new HashMap<String, OTFLane>();
		}
		this.laneData .put(laneData.getId(), laneData);
	}
	
	public Map<String, OTFLane> getLaneData(){
		return this.laneData;
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
	
	public void setLinkWidth(double linkWidth) {
		this.linkWidth = linkWidth;
	}
	
	public double getLinkWidth(){
		return this.linkWidth;
	}

	public void setLinkStartCenterPoint(Point2D.Double linkStartCenter) {
		this.linkStartCenterPoint = linkStartCenter;
	}
	
	public Point2D.Double getLinkStartCenterPoint() {
		return this.linkStartCenterPoint;
	}
	
	public void setLinkEndCenterPoint(Point2D.Double linkStartCenter) {
		this.linkEndCenterPoint = linkStartCenter;
	}
	
	public Point2D.Double getLinkEndCenterPoint() {
		return this.linkEndCenterPoint;
	}
	
	public void addToLink(OTFLinkWLanes link){
		if (this.toLinks == null){
			this.toLinks = new ArrayList<OTFLinkWLanes>();
		}
		this.toLinks.add(link);
	}

	public List<OTFLinkWLanes> getToLinks() {
		return this.toLinks ;
	}
	

	
}
