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
package playground.agarwalamit.mixedTraffic.plots;

import org.matsim.api.core.v01.network.Link;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;


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
	private double availableLinkSpace ;
	private double queuingTime;

	public double getFreeSpeedLinkTravelTime() {
		setFreeSpeedLinkTravelTime();
		return this.freeSpeedLinkTravelTime;
	}

	private void setFreeSpeedLinkTravelTime() {
		//		double tt = getLinkLength() / Math.min(link.getFreespeed(), getVehicleSpeed(this.legMode));
		double tt = this.availableLinkSpace / Math.min(this.link.getFreespeed(), MixedTrafficVehiclesUtils.getSpeed(this.legMode));
		this.freeSpeedLinkTravelTime = tt;
	}

	public double getLinkEnterTime() {
		return this.linkEnterTime;
	}

	public void setLinkEnterTime(double linkEnterTime) {
		this.linkEnterTime = linkEnterTime;
	}

	public double getLinkLeaveTime() {
		return this.linkLeaveTime;
	}

	public void setLinkLeaveTime(double linkLeaveTime) {
		this.linkLeaveTime = linkLeaveTime;
	}

	public String getLegMode() {
		return this.legMode;
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

	public void checkIfVehicleWillGoInQ(double currentTimeStep){
		double travelTimeSincePersonHasEntered = currentTimeStep - getLinkEnterTime();
		if(currentTimeStep!=getLinkLeaveTime()){
			this.addVehicleInQ= travelTimeSincePersonHasEntered > Math.floor(getFreeSpeedLinkTravelTime()) + 1;
		} else this.addVehicleInQ=false;
	}

	public boolean addVehicleInQ() {
		return this.addVehicleInQ;
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
		this.queuingTime = this.linkEnterTime + availableSpaceSoFar / Math.min(this.link.getFreespeed(), MixedTrafficVehiclesUtils.getSpeed(this.legMode));
	}
}
