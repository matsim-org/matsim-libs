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
package playground.agarwalamit.qStartPosition;

import org.matsim.api.core.v01.network.Link;


/**
 * @author amit
 */
public class PersonOnLinkInformation {

	private double freeSpeedLinkTravelTime;
	private double linkEnterTime;
	private double linkLeaveTime;
	private String legMode;
	private Link link;
	private boolean addVehicleInQ;
	private final String travelModes [] = new String [] {"cars","motorbikes","bicycles"}; //{"fast","med","truck"};
	private double availableLinkSpace ;
	private double queuingTime;

	public double getFreeSpeedLinkTravelTime() {
		setFreeSpeedLinkTravelTime();
		return freeSpeedLinkTravelTime;
	}

	private void setFreeSpeedLinkTravelTime() {
		//		double tt = getLinkLength() / Math.min(link.getFreespeed(), getVehicleSpeed(this.legMode));
		double tt = availableLinkSpace / Math.min(link.getFreespeed(), getVehicleSpeed(this.legMode));
		this.freeSpeedLinkTravelTime = tt;
	}

	public double getLinkEnterTime() {
		return linkEnterTime;
	}

	public void setLinkEnterTime(double linkEnterTime) {
		this.linkEnterTime = linkEnterTime;
	}

	public double getLinkLeaveTime() {
		return linkLeaveTime;
	}

	public void setLinkLeaveTime(double linkLeaveTime) {
		this.linkLeaveTime = linkLeaveTime;
	}

	public String getLegMode() {
		return legMode;
	}

	public void setLegMode(String legMode) {
		this.legMode = legMode;
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public double getLinkLength(){
		return link.getLength();
		//		return 666/9;
	}

	private double getVehicleSpeed(String travelMode) {
		double vehicleSpeed =0;
		if(travelMode.equals(travelModes[0])||travelMode.equals("fast")) {
			vehicleSpeed= 16.67;
		} else if(travelMode.equals(travelModes[1])||travelMode.equals("med")) {
			vehicleSpeed = 16.67;
		} else if(travelMode.equals(travelModes[2])||travelMode.equals("truck")){
			vehicleSpeed= 4.167;
		}
		return vehicleSpeed;
	}

	public void checkIfVehicleWillGoInQ(double currentTimeStep){
		double travelTimeSincePersonHasEntered = currentTimeStep - getLinkEnterTime();
		if(currentTimeStep!=getLinkLeaveTime()){
			this.addVehicleInQ= travelTimeSincePersonHasEntered > Math.floor(getFreeSpeedLinkTravelTime()) + 1;
		} else this.addVehicleInQ=false;
	}

	public boolean addVehicleInQ() {
		return addVehicleInQ;
	}

	public void setAvailableLinkSpace(double availableLinkSpace) {
		if(availableLinkSpace<0) this.availableLinkSpace=0;
		else this.availableLinkSpace = availableLinkSpace;
	}
	public double getQueuingTime(){
		return this.queuingTime;
	}
	public void setQueuingTime(double availableSpaceSoFar){
		// to set actual queuing time (45.56) not like earlier (45.0).
		this.queuingTime = linkEnterTime + availableSpaceSoFar / Math.min(link.getFreespeed(), getVehicleSpeed(this.legMode));
	}
}
