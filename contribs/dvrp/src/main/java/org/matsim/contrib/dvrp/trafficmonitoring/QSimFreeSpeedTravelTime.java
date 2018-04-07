/* *********************************************************************** *
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
 * *********************************************************************** */

package org.matsim.contrib.dvrp.trafficmonitoring;

import javax.inject.Inject;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * Precise version of FreeSpeedTravelTime that takes into account the way QSim moves vehicles along links and over
 * nodes. Useful for simulations with congestion-free (i.e. via super high flow/storage capacity factors) QSim.
 * 
 * @author michalm
 */
public class QSimFreeSpeedTravelTime implements TravelTime {
	private final double timeStepSize;

	@Inject
	public QSimFreeSpeedTravelTime(QSimConfigGroup qsimCfg) {
		this.timeStepSize = qsimCfg.getTimeStepSize();
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		double freeSpeedTT = link.getLength() / link.getFreespeed(time); // equiv. to FreeSpeedTravelTime
		double linkTravelTime = timeStepSize * Math.floor(freeSpeedTT / timeStepSize); // used in QSim for TT at link
		return linkTravelTime + 1;// adds 1 extra second for moving over nodes
	}
}
