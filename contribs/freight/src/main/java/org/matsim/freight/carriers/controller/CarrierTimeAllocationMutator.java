/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */
package org.matsim.freight.carriers.controller;

import java.util.ArrayList;
import java.util.Collection;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.ScheduledTour;

/**
 * @author nagel
 *
 */
public class CarrierTimeAllocationMutator implements GenericPlanStrategyModule<CarrierPlan> {
	public static final class Factory {
		private double probability = 1.;
		private double mutationRange = 3600.*3;
		public CarrierTimeAllocationMutator build() {
			return new CarrierTimeAllocationMutator( probability, mutationRange );
		}
		public Factory setProbability( double probability ) {
			this.probability = probability;
			return this;
		}
		public Factory setMutationRange( double mutationRange ){
			this.mutationRange = mutationRange;
			return this;
		}
	}

	private double probability = 1.;

	/**
	 * max. departure time mutation: +/- 0.5 * mutationRange
	 */
	private double mutationRange = 3600. * 3.;

	/**
	 * @deprecated -- use {@link Factory}
	 */
	@Deprecated public CarrierTimeAllocationMutator() {
	}

	/**
	 * @deprecated -- use {@link Factory}
	 */
	@Deprecated public CarrierTimeAllocationMutator(double probability, double mutationRange ) {
		this.probability = probability;
		this.mutationRange = mutationRange;
	}

	@Override
	public void handlePlan(CarrierPlan carrierPlan) {
		Collection<ScheduledTour> newTours = new ArrayList<>() ;
		for ( ScheduledTour tour : carrierPlan.getScheduledTours() ) {
			if(MatsimRandom.getRandom().nextDouble() < probability) {
				double departureTime = tour.getDeparture() + (MatsimRandom.getRandom().nextDouble() - 0.5) * mutationRange;
				if ( departureTime < tour.getVehicle().getEarliestStartTime() ) {
					departureTime = tour.getVehicle().getEarliestStartTime();
				}
				newTours.add(ScheduledTour.newInstance(tour.getTour(), tour.getVehicle(), departureTime));
			}
			else newTours.add(tour);
		}
		carrierPlan.getScheduledTours().clear();
		carrierPlan.getScheduledTours().addAll( newTours ) ;
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void finishReplanning() {
	}

}
