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
package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author dziemke
 */
class BicycleTravelTime implements TravelTime {

	@Inject
	private BicycleConfigGroup bicycleConfigGroup;

	/**
	 * for unit testing
	 */
	BicycleTravelTime(BicycleConfigGroup configGroup) {
		this.bicycleConfigGroup = configGroup;
	}

    @Inject
    private BicycleTravelTime() {
    }

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		if (hasNotAttribute(link, BicycleLabels.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR)) {
			throw new RuntimeException("Infrastructure speed factors must be set for all links that allow the bicycle mode!");
		}

		// This is not yet available, but might be at some point, see https://matsim.atlassian.net/browse/MATSIM-700
		// double bicycleVelocity = vehicle.getType().getMaximumVelocity()

		double maxBicycleSpeed = bicycleConfigGroup.getMaxBicycleSpeedForRouting();
		double bicycleInfrastructureFactor = Double.parseDouble(link.getAttributes().getAttribute(BicycleLabels.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR).toString());
		double surfaceFactor = computeSurfaceFactor(link);
		double gradientFactor = computeGradientFactor(link);
		double speed = maxBicycleSpeed * bicycleInfrastructureFactor * surfaceFactor * gradientFactor;
		return link.getLength() / speed;
	}

	/**
	 * Based on "Flügel et al. -- Empirical speed models for cycling in the Oslo road network" (not yet published!)
	 * Positive gradients (uphill): Roughly linear decrease in speed with increasing gradient
	 * At 9% gradient, cyclists are 42.7% slower
	 * Negative gradients (downhill):
	 * Not linear; highest speeds at 5% or 6% gradient; at gradients higher than 6% braking
	 */
	private double computeGradientFactor(Link link) {

		double factor = 1;
		if (link.getFromNode().getCoord().hasZ() && link.getToNode().getCoord().hasZ()) {
			double fromZ = link.getFromNode().getCoord().getZ();
			double toZ = link.getToNode().getCoord().getZ();
			if (toZ > fromZ) { // No positive speed increase for downhill, only decrease for uphill
				double reduction = 1 - 5 * ((toZ - fromZ) / link.getLength());
				factor = Math.max(0.1, reduction); // maximum reduction is 0.1
			}
		}

		return factor;
	}

	// TODO combine this with comfort
	private double computeSurfaceFactor(Link link) {
		if (hasNotAttribute(link, BicycleLabels.WAY_TYPE)
				|| link.getAttributes().getAttribute(BicycleLabels.WAY_TYPE).equals(BicycleLabels.CYCLEWAY)
				|| hasNotAttribute(link, BicycleLabels.SURFACE)
		) {
			return 1.0;
		}
		//so, the link is NOT a cycleway, and has a surface attribute
		String surface = link.getAttributes().getAttribute(BicycleLabels.SURFACE).toString();
		switch (surface) {
			case "paved":
			case "asphalt":
				return 1.0;

			case "cobblestone (bad)":
			case "grass":
				return 0.4;

			case "cobblestone;flattened":
			case "cobblestone:flattened":
			case "sett":
			case "earth":
				return 0.6;

			case "concrete":
			case "asphalt;paving_stones:35":
			case "compacted":
				return 0.9;

			case "concrete:lanes":
			case "concrete_plates":
			case "concrete:plates":
			case "paving_stones:3":
				return 0.8;

			case "paving_stones":
			case "paving_stones:35":
			case "paving_stones:30":
			case "compressed":
			case "bricks":
			case "stone":
			case "pebblestone":
			case "fine_gravel":
			case "gravel":
			case "ground":
				return 0.7;

			case "sand":
				return 0.2;

			default:
				return 0.5;
		}
	}

	private boolean hasNotAttribute(Link link, String attributeName) {
		return link.getAttributes().getAttribute(attributeName) == null;
	}
}