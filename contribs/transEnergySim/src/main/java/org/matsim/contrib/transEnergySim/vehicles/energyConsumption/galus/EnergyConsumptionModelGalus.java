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

package org.matsim.contrib.transEnergySim.vehicles.energyConsumption.galus;

import java.util.Iterator;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingchoice.lib.DebugLib;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.AbstractInterpolatedEnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumption;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

/**
 * This module provides the energy consumption by an PHEV/EV(?? TODO: check) based on
 * different speeds. This work is based on the following paper (TODO: double check with Galus again)
 * 
 * Galus, M. D. and G. Andersson (2009b) Power system considerations of plug-in
 * hybrid electric vehicles based on a multi energy carrier model, paper
 * presented at Power and Energy Society (PES) General Meeting, Calgary, Canada.
 * 
 * TODO: also need to explain the discontinuation at speed 50.
 * 
 * @author rashid_waraich
 * 
 */
public class EnergyConsumptionModelGalus extends AbstractInterpolatedEnergyConsumptionModel {

	//TODO discuss how to return a simplified consumption for range estimation purposes
	private double averageEnergyConsumptionPerMeterTraveled = 4.14E+02;
	

	public EnergyConsumptionModelGalus() {
		initModell();
	}

	private void initModell() {
		queue.add(new EnergyConsumption(5.555555556, 3.173684E+02));
		queue.add(new EnergyConsumption(8.333333333, 4.231656E+02));
		queue.add(new EnergyConsumption(11.11111111, 5.549931E+02));
		queue.add(new EnergyConsumption(13.88888889, 1.039878E+03));
		queue.add(new EnergyConsumption(16.66666667, 4.056338E+02));
		queue.add(new EnergyConsumption(19.44444444, 4.784535E+02));
		queue.add(new EnergyConsumption(22.22222222, 5.580053E+02));
		queue.add(new EnergyConsumption(25, 6.490326E+02));
		queue.add(new EnergyConsumption(27.77777778, 7.502112E+02));
		queue.add(new EnergyConsumption(30.55555556, 8.614505E+02));
		queue.add(new EnergyConsumption(33.33333333, 1.179291E+03));
		queue.add(new EnergyConsumption(36.11111111, 1.825931E+03));
		queue.add(new EnergyConsumption(38.88888889, 2.418100E+03));
		queue.add(new EnergyConsumption(41.66666667, 2.905639E+03));
	}

	
	@Override
	public double getEnergyConsumptionRateInJoulesPerMeter() {
		return averageEnergyConsumptionPerMeterTraveled;
	}

	

	

	

}
