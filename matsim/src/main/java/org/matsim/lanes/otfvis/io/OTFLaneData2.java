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


/**
 * @author dgrether
 */
public class OTFLaneData2 {
	private String id;
	private double endPoint;
	private double startPoint;
	private int alignment;
	private double numberOfLanes;
	private List<LaneToLinkData> toLinksData;
	private SignalGroupState state = null;

	
	public OTFLaneData2() {
	}
	
	public void setId(String id){
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setEndPoint(double pos) {
		this.endPoint = pos;
	}
	
	public double getEndPoint() {
		return endPoint;
	}

	
	public double getStartPoint() {
		return startPoint;
	}

	
	public void setStartPoint(double startPoint) {
		this.startPoint = startPoint;
	}

	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}
	
	public int getAlignment(){
		return this.alignment;
	}
	
	public List<LaneToLinkData> getLaneToLinkData(){
		return this.toLinksData;
	}

	public double getNumberOfLanes() {
		return this.numberOfLanes;
	}
	
	public void setNumberOfLanes(double noLanes){
		this.numberOfLanes = noLanes;
	}

	public void addToLinkData(double toLinkStartX, double toLinkStartY, double normalX, double normalY, double toLinkNumberOfLanes) {
		if (this.toLinksData == null) {
			this.toLinksData = new ArrayList<LaneToLinkData>();
		}
		this.toLinksData.add(new LaneToLinkData(new Point2D.Double(toLinkStartX, toLinkStartY), 
				new Point2D.Double(normalX, normalY), toLinkNumberOfLanes));
	}
	
	public void setSignalGroupState(SignalGroupState state) {
		this.state = state;
	}
	
	public SignalGroupState getSignalGroupState(){
		return this.state ;
	}

}



