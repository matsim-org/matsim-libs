/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.GfipQueuePassingControler.GfipMode;
import org.matsim.core.mobsim.qsim.qnetsimengine.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vehicles.VehicleType;

/**
 * Class to implement the vehicle and density-dependent travel time estimation
 * for the passing queue simulation based on the Gauteng Freeway Improvement
 * Project (GFIP) gantry data.
 * 
 * @author jwjoubert
 */
public class GfipLinkSpeedCalculator implements LinkSpeedCalculator {
	final private double CELL_SIZE = 7.5;
	final private Logger log = Logger.getLogger(GfipLinkSpeedCalculator.class);
	final private QSim qsim;
	private int infoCount = 0;
	
	public GfipLinkSpeedCalculator(final QSim qsim) {
		this.qsim = qsim;
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		Id<VehicleType> vehicleType = vehicle.getVehicle().getType().getId();
		
		int numberOfVehiclesAhead = this.qsim.getNetsimNetwork().getNetsimLink(link.getId()).getAllDrivingVehicles().size();
		
		/* Get the current consumed capacity of the link (in pcu equivalents) */
		NetsimLink thisLink = this.qsim.getNetsimNetwork().getNetsimLink(link.getId());
		double pcuEquivalents = (double) thisLink.getCustomAttributes().get("pcu");
		
		/* Get link's actual capacity (in pcu equivalents) */
		double pcuCapacity = 1.1*(link.getLength()*link.getNumberOfLanes()/
				((NetworkImpl)this.qsim.getNetsimNetwork().getNetwork()).getEffectiveCellSize());
		double density = pcuEquivalents / pcuCapacity;
		
		if(infoCount < 5){
			if(infoCount == 5){
				log.warn(Gbl.FUTURE_SUPPRESSED);
			}
		}
		
		GfipMode mode = GfipMode.valueOf(vehicle.getVehicle().getType().getId().toString().toUpperCase());
		double maximumSpeed = Math.min(link.getFreespeed(), vehicle.getVehicle().getType().getMaximumVelocity());
		switch (mode) {
		case GFIP_A1:
			break;
		case GFIP_A2:
			break;
		case GFIP_B:
			break;
		case GFIP_C:
			break;
		default:
			throw new RuntimeException("Don't know how to handle velocity for mode " + mode.toString());
		}
		
//		double velocity = vehicle.getVehicle().getType().getMaximumVelocity()*(1 - (rho / rho_jam));
		double newPassingVelocity = maximumSpeed*(1.0 - density);
		double oldPassingVelocity = Math.min(vehicle.getMaximumVelocity(), link.getFreespeed(time));
		double velocity = newPassingVelocity;
		
		if(infoCount < 5){
			log.info(String.format("  -----> Type: %s; Vehicles in front: %d (pcu: %.2f); density: %.8f", vehicleType.toString(), numberOfVehiclesAhead, pcuEquivalents, density));
			log.info(String.format("     \\-> Basic PassingQ velocity: %.8f; Gfip velocity: %.8f", oldPassingVelocity, newPassingVelocity));
			infoCount++;
			if(infoCount == 5){
				log.warn(Gbl.FUTURE_SUPPRESSED);
			}
		}		
	
		return velocity;
	}

}
