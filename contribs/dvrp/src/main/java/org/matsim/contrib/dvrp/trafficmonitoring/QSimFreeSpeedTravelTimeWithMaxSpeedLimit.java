/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.trafficmonitoring;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * An extended QSimFreeSpeedTravelTime that allows for adjusting the free-flow speed to the max vehicle speed.
 *
 * @author michalm
 */
public class QSimFreeSpeedTravelTimeWithMaxSpeedLimit implements TravelTime {
	private final double timeStepSize;
	//FIXME use vehicle max speed (currently impossible, because the vehicle passed to path search is often null)
	private final double maxSpeed;

	public QSimFreeSpeedTravelTimeWithMaxSpeedLimit(QSimConfigGroup qsimCfg, VehicleType vehicleType) {
		this(qsimCfg.getTimeStepSize(), vehicleType.getMaximumVelocity());
	}

	public QSimFreeSpeedTravelTimeWithMaxSpeedLimit(double timeStepSize, double maxSpeed) {
		this.timeStepSize = timeStepSize;
		this.maxSpeed = maxSpeed;
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		double freeSpeed = Math.min(link.getFreespeed(time), maxSpeed);
		double freeSpeedTT = link.getLength() / freeSpeed;
		double linkTravelTime = timeStepSize * Math.floor(freeSpeedTT / timeStepSize); // used in QSim for TT at link
		return linkTravelTime + 1;// adds 1 extra second for moving over nodes
	}
}
