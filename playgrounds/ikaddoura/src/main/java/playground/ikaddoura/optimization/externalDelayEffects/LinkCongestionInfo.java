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

/**
 * 
 */
package playground.ikaddoura.optimization.externalDelayEffects;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * @author ikaddoura
 *
 */
public class LinkCongestionInfo {
	
	private Id linkId;
	private double freeTravelTime;

	private double marginalDelayPerLeavingVehicle_sec;
	private int storageCapacity_cars;
	
	private List<MarginalCongestionEvent> congestionEvents_FlowCapacity = new ArrayList<MarginalCongestionEvent>();
	private List<PersonDelayInfo> personDelayInfos = new ArrayList<PersonDelayInfo>();
	private List<LinkEnterLeaveInfo> personId2enterLeaveInfo = new ArrayList<LinkEnterLeaveInfo>();
	
	public Id getLinkId() {
		return linkId;
	}
	public void setLinkId(Id linkId) {
		this.linkId = linkId;
	}
	public void setMarginalDelayPerLeavingVehicle(double flowCapacity_hour) {
		this.marginalDelayPerLeavingVehicle_sec = (1 / (flowCapacity_hour / 3600.));
	}
	public int getStorageCapacity_cars() {
		return storageCapacity_cars;
	}
	public void setStorageCapacity_cars(int storageCapacity_cars) {
		this.storageCapacity_cars = storageCapacity_cars;
	}
	public double getMarginalDelayPerLeavingVehicle_sec() {
		return marginalDelayPerLeavingVehicle_sec;
	}
	public double getFreeTravelTime() {
		return freeTravelTime;
	}
	public void setFreeTravelTime(double freeTravelTime) {
		this.freeTravelTime = freeTravelTime;
	}
	public List<PersonDelayInfo> getPersonDelayInfos() {
		return personDelayInfos;
	}
	public void setPersonDelayInfos(List<PersonDelayInfo> personDelayInfos) {
		this.personDelayInfos = personDelayInfos;
	}
	
	@Override
	public String toString() {
		return "LinkCongestionInfo [linkId=" + linkId + ", freeTravelTime="
				+ freeTravelTime + ", marginalDelayPerLeavingVehicle_sec="
				+ marginalDelayPerLeavingVehicle_sec + ", storageCapacity="
				+ storageCapacity_cars + ", personDelayInfos=" + personDelayInfos
				+ ", personId2enterLeaveInfo=" + personId2enterLeaveInfo + "]";
	}
	public List<LinkEnterLeaveInfo> getPersonEnterLeaveInfos() {
		return personId2enterLeaveInfo;
	}
	public void setPersonId2enterLeaveInfo(List<LinkEnterLeaveInfo> personId2enterLeaveInfo) {
		this.personId2enterLeaveInfo = personId2enterLeaveInfo;
	}
	public List<MarginalCongestionEvent> getflowCapacityCongestionEvents() {
		return congestionEvents_FlowCapacity;
	}
	public void setflowCapacityCongestionEvents(
			List<MarginalCongestionEvent> flowCapacityCongestionEvents) {
		this.congestionEvents_FlowCapacity = flowCapacityCongestionEvents;
	}
}
