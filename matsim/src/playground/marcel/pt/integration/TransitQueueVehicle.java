/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.integration;

import org.matsim.core.api.network.Link;
import org.matsim.core.mobsim.queuesim.QueueVehicle;

import playground.marcel.pt.interfaces.Vehicle;

public class TransitQueueVehicle extends QueueVehicle {

	private final Vehicle vehicle;
	private final QueueTransitDriver driver;
	
	public TransitQueueVehicle(Vehicle vehicle, QueueTransitDriver driver) {
		super(driver.getPerson().getId());
		this.vehicle = vehicle;
		this.driver = driver;
		super.setDepartureTime_s(driver.getDepartureTime());
	}
	
	public Link getCurrentLink() {
		return this.driver.getCurrentLink(); //this.vehicle.getDriver().getCurrentLink();
	}
	
}
