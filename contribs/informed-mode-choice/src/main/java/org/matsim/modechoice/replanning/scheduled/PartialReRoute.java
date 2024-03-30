/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoute.java
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

package org.matsim.modechoice.replanning.scheduled;

import jakarta.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;

import java.util.List;

/**
 * Router that only re-routes plans that have changed mode.
 *
 * @see org.matsim.core.replanning.modules.ReRoute
 */
public class PartialReRoute extends AbstractMultithreadedModule {

	private final TimeInterpretation timeInterpretation;
	private final Provider<TripRouter> tripRouterProvider;
	private final ActivityFacilities facilities;

	public PartialReRoute(ActivityFacilities facilities, Provider<TripRouter> tripRouterProvider, GlobalConfigGroup globalConfigGroup, TimeInterpretation timeInterpretation) {
		super(globalConfigGroup);
		this.facilities = facilities;
		this.tripRouterProvider = tripRouterProvider;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public final PlanAlgorithm getPlanAlgoInstance() {
		return new Algorithm(tripRouterProvider.get(), facilities, timeInterpretation);
	}

	/**
	 * Similar to {@link PlanRouter}, but only routes changed trips.
	 */
	private final static class Algorithm implements PlanAlgorithm, PersonAlgorithm {
		private final TripRouter tripRouter;
		private final ActivityFacilities facilities;
		private final TimeInterpretation timeInterpretation;

		Algorithm(final TripRouter tripRouter, final ActivityFacilities facilities, final TimeInterpretation timeInterpretation) {
			this.tripRouter = tripRouter;
			this.facilities = facilities;
			this.timeInterpretation = timeInterpretation;
		}

		@Override
		public void run(final Plan plan) {
			final List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
			TimeTracker timeTracker = new TimeTracker(timeInterpretation);

			for (TripStructureUtils.Trip oldTrip : trips) {
				final String routingMode = TripStructureUtils.identifyMainMode(oldTrip.getTripElements());
				timeTracker.addActivity(oldTrip.getOriginActivity());

				boolean allRouted = oldTrip.getLegsOnly().stream().allMatch(l -> l.getRoute() != null);

				// Only rerouted if no route is present
				if (!allRouted) {
					final List<? extends PlanElement> newTripElements = tripRouter.calcRoute( //
						routingMode, //
						FacilitiesUtils.toFacility(oldTrip.getOriginActivity(), facilities), //
						FacilitiesUtils.toFacility(oldTrip.getDestinationActivity(), facilities), //
						timeTracker.getTime().seconds(), //
						plan.getPerson(), //
						oldTrip.getTripAttributes() //
					);

					PlanRouter.putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTripElements);

					TripRouter.insertTrip(plan, oldTrip.getOriginActivity(), newTripElements, oldTrip.getDestinationActivity());

					timeTracker.addElements(newTripElements);
				} else {
					timeTracker.addElements(oldTrip.getTripElements());
				}

			}
		}

		@Override
		public void run(final Person person) {
			for (Plan plan : person.getPlans()) {
				run(plan);
			}
		}
	}
}
