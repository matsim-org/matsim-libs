/* *********************************************************************** *
 * project: org.matsim.*
 * Lane
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
package org.matsim.lanes;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


/**
 * Serves as meta structure between the data classes and the mobility simulation. 
 * The lane data classes do not have any lane length specified. To calculate the 
 * lane length the link is required that is not available if data is separated in 
 * different containers. 
 * Further, there is a List of downstream LaneImpl instances. 
 *  
 * @author dgrether
 */
public final class ModelLane {

	private final Lane laneData;
	private double length;
	private double endsAtMetersFromLinkEnd;
	private final List<Id<Link>> destinationLinkIds = new ArrayList<>();
	private final List<ModelLane> toLanes = new ArrayList<>();

	ModelLane(Lane data) {
		this.laneData = data;
	}
	
	public Lane getLaneData(){
		return this.laneData;
	}
	
	public List<ModelLane> getToLanes(){
		return toLanes;
	}

	void addAToLane(ModelLane toLane) {
		this.toLanes.add(toLane);
	}

	void setLength(double lengthMeter) {
		this.length = lengthMeter;
	}

	public double getLength() {
		return this.length;
	}

	
	double getEndsAtMeterFromLinkEnd(){
		return this.endsAtMetersFromLinkEnd;
	}

	void setEndsAtMetersFromLinkEnd(double endsAtMetersFromLinkEnd) {
		this.endsAtMetersFromLinkEnd = endsAtMetersFromLinkEnd;
	}

	void addDestinationLink(Id<Link> toLinkId) {
		this.destinationLinkIds.add(toLinkId);
	}

	List<Id<Link>> getDestinationLinkIds() {
		return this.destinationLinkIds;
	}

	
	
}
