/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleLabels;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author dziemke
 */
public class BicycleTravelTime implements TravelTime {

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		// TODO make this speed adjustable
		double minimumSpeedForDedicatedCyclingInfrastructure = 15.0/3.6;
		
		String type = (String) link.getAttributes().getAttribute("type");
		String cycleway = (String) link.getAttributes().getAttribute(BicycleLabels.CYCLEWAY);
		double cyclingInfrastructureSpeed = computeMinimumCyclingInfrastructureSpeed(type,
				cycleway, minimumSpeedForDedicatedCyclingInfrastructure);
		double infrastructureSpeed = Math.max(link.getFreespeed(), cyclingInfrastructureSpeed);

		double vehicleLinkSpeed = Math.min(infrastructureSpeed, BicycleUtils.getSpeed("bicycle"));

		double gradientSpeed = computeGradientSpeed(link, vehicleLinkSpeed);

		String surface = (String) link.getAttributes().getAttribute(BicycleLabels.SURFACE);
		double surfaceSpeed = vehicleLinkSpeed;
		if (surface != null) {
			surfaceSpeed = computeSurfaceSpeed(vehicleLinkSpeed, surface, type);
		}
		
		double effectiveSpeed = Math.min(gradientSpeed, surfaceSpeed);
		
		return (link.getLength() / effectiveSpeed);
	}
	
	/**
	 * If there is a dedicated cycling infrastructure, it is unlikely that the speed (before later consideration
	 * of slopes) falls under a certain minimum
	 * @return
	 */
	private double computeMinimumCyclingInfrastructureSpeed(String type, String cycleway, double minimumSpeedForDedicatedCyclingInfrastructure) {
		double cyclingInfrastructureSpeed = 0.;
		if (type.equals("cycleway")) {
			cyclingInfrastructureSpeed = minimumSpeedForDedicatedCyclingInfrastructure; // Assume that this speed is always feasible on a dedicated cycleway
		}
		
		if (cycleway != null) {
			if (cycleway.equals("track") || cycleway.equals("track")) {
				cyclingInfrastructureSpeed = minimumSpeedForDedicatedCyclingInfrastructure; // Assume that this speed is always feasible on a dedicated cycleway
			}
		}
		return cyclingInfrastructureSpeed;
	}

	/**
	 * Based on "Flügel et al. -- Empirical speed models for cycling in the Oslo road network" (not yet published!)
	 * Positive gradients (uphill): Roughly linear decrease in speed with increasing gradient
	 * At 9% gradient, cyclists are 42.7% slower
	 * Negative gradients (downhill):
	 * Not linear; highest speeds at 5% or 6% gradient; at gradients higher than 6% braking
	 */
	private double computeGradientSpeed(Link link, double vehicleLinkSpeed) {
		double gradientSpeedFactor = 1.;
		Double fromNodeZ = link.getFromNode().getCoord().getZ();
		Double toNodeZ = link.getToNode().getCoord().getZ();
		if ((fromNodeZ != null) && (toNodeZ != null)) {
			if (toNodeZ > fromNodeZ) { // No positive speed increase for downhill, only decrease for uphill
				gradientSpeedFactor = 1 - 5. * ((toNodeZ - fromNodeZ) / link.getLength()); // 50% reducation at 10% up-slope
			}
		}
		if (gradientSpeedFactor < 0.1) {
			gradientSpeedFactor = 0.1;
		}
		return vehicleLinkSpeed * gradientSpeedFactor;
	}
	
	// TODO combine this with comfort
	private double computeSurfaceSpeed(double vehicleLinkSpeed, String surface, String type) {
		if (type.equals("cycleway")) {
			return vehicleLinkSpeed; // Assuming that dedicated cycleways are on good surface like ashalt
		}
		double surfaceSpeedFactor;
		switch (surface) {
			case "paved":
			case "asphalt": surfaceSpeedFactor = 1.; break;
			case "cobblestone": surfaceSpeedFactor = 0.5; break;
			case "cobblestone (bad)": surfaceSpeedFactor = 0.4; break;
			case "cobblestone;flattened":
			case "cobblestone:flattened":
			case "sett": surfaceSpeedFactor = 0.6; break;
			case "concrete": surfaceSpeedFactor =  0.9; break;
			case "concrete:lanes":
			case "concrete_plates":
			case "concrete:plates": surfaceSpeedFactor = 0.8; break;
			case "paving_stones":
			case "paving_stones:35":
			case "paving_stones:30": surfaceSpeedFactor = 0.7; break;
			case "unpaved": surfaceSpeedFactor = 0.5; break;
			case "compacted": surfaceSpeedFactor = 0.9; break;
			case "dirt": surfaceSpeedFactor = 0.5; break;
			case "earth": surfaceSpeedFactor = 0.6; break;
			case "fine_gravel":
			case "gravel":
			case "ground": surfaceSpeedFactor = 0.7; break;
			case "wood": surfaceSpeedFactor =  0.5; break;
			case "pebblestone": surfaceSpeedFactor = 0.7; break;
			case "sand": surfaceSpeedFactor = 0.2; break;
			case "bricks":
			case "stone": surfaceSpeedFactor = 0.7; break;
			case "grass": surfaceSpeedFactor = 0.4; break;
			case "compressed": surfaceSpeedFactor =  0.7; break;
			case "asphalt;paving_stones:35": surfaceSpeedFactor = 0.9; break;
			case "paving_stones:3": surfaceSpeedFactor = 0.8; break;
			default: surfaceSpeedFactor = 0.5;
		}
		return vehicleLinkSpeed * surfaceSpeedFactor;
	}
}