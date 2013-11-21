/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis;

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

public class ParkingEventDetails {

	public ParkingEventDetails(int legIndex, double score, ParkingSearchStrategy parkingStrategy,
			ParkingActivityAttributes parkingActivityAttributes, String activityType) {
		super();
		this.legIndex = legIndex;
		this.score = score;
		this.parkingStrategy = parkingStrategy;
		this.parkingActivityAttributes = parkingActivityAttributes;
		this.activityType = activityType;
	}
	public int legIndex;
	public double score;
	public ParkingSearchStrategy parkingStrategy;
	public ParkingActivityAttributes parkingActivityAttributes;
	public String activityType;
	
	public String getTabSeparatedLogString(){
		StringBuffer sb=null;
		
		sb=new StringBuffer();
		
		sb.append(parkingActivityAttributes.getPersonId());
		sb.append("\t");
		sb.append(parkingActivityAttributes.getParkingArrivalTime());
		sb.append("\t");
		sb.append(parkingActivityAttributes.getParkingDuration());
		sb.append("\t");
		sb.append(parkingActivityAttributes.getParkingSearchDuration());
		sb.append("\t");
		sb.append(parkingActivityAttributes.getToActWalkDuration());
		sb.append("\t");
		sb.append(parkingActivityAttributes.getWalkDistance());
		sb.append("\t");
		sb.append(parkingActivityAttributes.getFacilityId());
		sb.append("\t");
		sb.append(parkingActivityAttributes.getParkingCost());
		sb.append("\t");
		sb.append(legIndex);
		sb.append("\t");
		sb.append(score);
		sb.append("\t");
		sb.append(parkingStrategy.getName());
		sb.append("\t");
		sb.append(parkingStrategy.getGroupName());
		sb.append("\t");
		sb.append(parkingActivityAttributes.destinationCoord.getX());
		sb.append("\t");
		sb.append(parkingActivityAttributes.destinationCoord.getY());
		sb.append("\t");
		sb.append(activityType);
		
		return sb.toString();
	}
	
	public static String getTabSeparatedTitleString(){
		return "personId\tparkingArrivalTime\tparkingDuration\tparkingSearchDuration\twalkDuration\twalkDistance\tFacilityId\tparkingCost\tlegIndex\tscore\tparkingStrategy\tgroupName\tdestination-X\tdestination-Y\tactivityType";
	}
}

