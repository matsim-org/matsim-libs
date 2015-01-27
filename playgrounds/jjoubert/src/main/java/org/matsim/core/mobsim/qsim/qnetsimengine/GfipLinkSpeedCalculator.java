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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.GfipQueuePassingControler.GfipMode;
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
		
		double dumbDensity = pcuEquivalents / pcuCapacity;
		
		if(infoCount < 5){
			if(infoCount == 5){
				log.warn(Gbl.FUTURE_SUPPRESSED);
			}
		}
		
		double rho = (pcuEquivalents + vehicle.getSizeInEquivalents()) / ( (link.getLength()/1000)*link.getNumberOfLanes());
		
		GfipMode mode = GfipMode.valueOf(vehicle.getVehicle().getType().getId().toString().toUpperCase());
		double classSpeed;
		double classRhoJam_freeFlow;
		double classRhoJam_congested = Double.POSITIVE_INFINITY;
		double classDensity;
		double reactionTime;
		
		/* TODO This is currently only the free flow case... not congestion. */
		switch (mode) {
		case GFIP_A1:
			classSpeed = (114.4 + 1.200*MatsimRandom.getLocalInstance().nextGaussian()) / 3.6;
			classRhoJam_freeFlow = 248 + 1.210*MatsimRandom.getLocalInstance().nextGaussian();
			classRhoJam_congested = -17.0 + 2.03*MatsimRandom.getLocalInstance().nextGaussian();
			reactionTime = 4.33 + 0.24*MatsimRandom.getLocalInstance().nextGaussian();
			break;
		case GFIP_A2:
			classSpeed = (103.6 + 0.119*MatsimRandom.getLocalInstance().nextGaussian()) / 3.6;
			classRhoJam_freeFlow = 238 + 0.120*MatsimRandom.getLocalInstance().nextGaussian();
			reactionTime = 4.00 + 0.79*MatsimRandom.getLocalInstance().nextGaussian();
			break;
		case GFIP_B:
			classSpeed = (85.7 + 0.191*MatsimRandom.getLocalInstance().nextGaussian()) / 3.6;
			classRhoJam_freeFlow = 379 + 0.191*MatsimRandom.getLocalInstance().nextGaussian();
			reactionTime = 3.86 + 1.10*MatsimRandom.getLocalInstance().nextGaussian();
			break;
		case GFIP_C:
			classSpeed = (77.8 + 0.132*MatsimRandom.getLocalInstance().nextGaussian()) / 3.6;
			classRhoJam_freeFlow = 1012 + 0.133*MatsimRandom.getLocalInstance().nextGaussian();
			reactionTime = 4.21 + 1.11*MatsimRandom.getLocalInstance().nextGaussian();
			break;
		default:
			throw new RuntimeException("Don't know how to handle velocity for mode " + mode.toString());
		}
		
		if(dumbDensity > 0.5 || (rho / classRhoJam_freeFlow) > 0.5){
			log.debug("Debug here.");
		}
		
		double maximumSpeed = Math.min(link.getFreespeed(), vehicle.getVehicle().getType().getMaximumVelocity());

		double oldPassingVelocity = Math.min(vehicle.getMaximumVelocity(), link.getFreespeed(time));
		double newPassingVelocity = maximumSpeed*(1.0 - dumbDensity);
		double uberPassingVelocity_freeflow = classSpeed*(1.0 - (rho / classRhoJam_freeFlow));
		double uberPassingVelocity_congested;
		if(rho == 0){
			uberPassingVelocity_congested = Double.MAX_VALUE;
		} else{
			uberPassingVelocity_congested = (1000 / (rho*reactionTime))*(1.0 - (rho / classRhoJam_congested ));
		}
		double uberPassingVelocity = Math.min(uberPassingVelocity_freeflow, uberPassingVelocity_congested);
		double velocity = uberPassingVelocity;
		
		if(link.getId().equals(Id.createLinkId("45"))){
			if(infoCount < 20000){
				log.info(String.format("  -----> Type: %s; Vehicles in front: %d (pcu: %.2f); density: %.4f (%.4f)", vehicleType.toString(), numberOfVehiclesAhead, pcuEquivalents, dumbDensity, rho / classRhoJam_freeFlow));
				log.info(String.format("     \\-> Basic PassingQ velocity: %.4f; Gfip velocity: %.4f; Fitted velocity: %.4f", oldPassingVelocity*3.6, newPassingVelocity*3.6, uberPassingVelocity*3.6));
				infoCount++;
				if(infoCount == 20000){
					log.warn(Gbl.FUTURE_SUPPRESSED);
				}
			}		
		}
	
		return velocity;
	}

}
