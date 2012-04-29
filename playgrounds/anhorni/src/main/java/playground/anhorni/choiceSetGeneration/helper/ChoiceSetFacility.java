/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.choiceSetGeneration.helper;

public class ChoiceSetFacility {
	
	private ZHFacility facility;
	private double	travelTimeStartShopEnd;
	private double travelDistanceStartShopEnd;
	
	private double additionalTime;
	private double additionalDistance;
	
	public ChoiceSetFacility(ZHFacility facility,
			double travelTimeStartShopEnd, double travelDistanceStartShopEnd) {
		super();
		this.facility = facility;
		this.travelTimeStartShopEnd = travelTimeStartShopEnd;
		this.travelDistanceStartShopEnd = travelDistanceStartShopEnd;
	}
		
	public ZHFacility getFacility() {
		return facility;
	}
	public void setFacility(ZHFacility facility) {
		this.facility = facility;
	}
	public double getTravelTimeStartShopEnd() {
		return travelTimeStartShopEnd;
	}
	public void setTravelTimeStartShopEnd(double travelTimeStartShopEnd) {
		this.travelTimeStartShopEnd = travelTimeStartShopEnd;
	}
	public double getTravelDistanceStartShopEnd() {
		return travelDistanceStartShopEnd;
	}
	public void setTravelDistanceStartShopEnd(double travelDistanceStartShopEnd) {
		this.travelDistanceStartShopEnd = travelDistanceStartShopEnd;
	}

	public double getAdditionalTime() {
		return additionalTime;
	}

	public void setAdditionalTime(double additionalTime) {
		this.additionalTime = additionalTime;
	}

	public double getAdditionalDistance() {
		return additionalDistance;
	}

	public void setAdditionalDistance(double additionalDistance) {
		this.additionalDistance = additionalDistance;
	}
}
