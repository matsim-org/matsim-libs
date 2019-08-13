/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
* @author ikaddoura, mmayobre
*/

public class AccidentLinkInfo {
	
	private final Id<Link> linkId;
	
	private ArrayList<Integer> roadTypeBVWP;	
	private LinkAccidentsComputationMethod computationMethod;
	private double speedLimit;
	private double roadWidth;
	private int numberSideRoads; //number of side Roads per km
	private ParkingType parkingType; 
	private double numberOfLanes; 
	private AccidentAreaType areaType;
	private Planequal_Planfree_Tunnel planequal_planfree_tunnel;
	private String landUseType;

	private final Map<Integer, TimeBinInfo> timeSpecificInfo = new HashMap<>();
	
	public AccidentLinkInfo(Id<Link> linkId) {
		this.linkId = linkId;
	}
	
	public Id<Link> getLinkId() {
		return linkId;
	}

	public ArrayList<Integer> getRoadTypeBVWP() {
		return roadTypeBVWP;
	}

	public void setRoadTypeBVWP(ArrayList<Integer> roadType) {
		this.roadTypeBVWP = roadType;
	}

	public double getNumberOfLanes() {
		return numberOfLanes;
	}

	public void setNumberOfLanes(double numberOfLanes) {
		this.numberOfLanes = numberOfLanes;
	}

	public AccidentAreaType getAreaType() {
		return areaType;
	}

	public void setAreaType(AccidentAreaType areaType) {
		this.areaType = areaType;
	}

	public Planequal_Planfree_Tunnel getPlanequal_planfree_tunnel() {
		return planequal_planfree_tunnel;
	}

	public void setPlanequal_planfree_tunnel(Planequal_Planfree_Tunnel planequal_planfree_tunnel) {
		this.planequal_planfree_tunnel = planequal_planfree_tunnel;
	}

	public String getLandUseType() {
		return landUseType;
	}

	public void setLandUseType(String landUseType) {
		this.landUseType = landUseType;
	}

	public Map<Integer, TimeBinInfo> getTimeSpecificInfo() {
		return timeSpecificInfo;
	}

	public double getSpeedLimit() {
		return speedLimit;
	}

	public void setSpeedLimit(double speedLimit) {
		this.speedLimit = speedLimit;
	}

	public double getRoadWidth() {
		return roadWidth;
	}

	public void setRoadWidth(double roadWidth) {
		this.roadWidth = roadWidth;
	}

	public int getNumberSideRoads() {
		return numberSideRoads;
	}

	public void setNumberSideRoads(int numberSideRoads) {
		this.numberSideRoads = numberSideRoads;
	}

	public ParkingType getParkingType() {
		return parkingType;
	}

	public void setParkingType(ParkingType parkingType) {
		this.parkingType = parkingType;
	}

	public LinkAccidentsComputationMethod getComputationMethod() {
		return computationMethod;
	}

	public void setComputationMethod(LinkAccidentsComputationMethod computationMethod) {
		this.computationMethod = computationMethod;
	}

}

