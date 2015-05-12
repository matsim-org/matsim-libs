/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.population.algorithms.PlanAlgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * methods for easy adatpation of code in the time TripRouter
 * provided tripsToLegs methods
 * @author thibautd
 */
public class RoutingUtils {
	private RoutingUtils() {}
	private final static PooledPlanFactory planFactory = new PooledPlanFactory();

	public static List<PlanElement> tripsToLegs(
			final Plan plan,
			final StageActivityTypes stageActivities,
			final MainModeIdentifier mainModeIdentifier) {
		final List<Trip> trips = TripStructureUtils.getTrips( plan , stageActivities );

		final List<PlanElement> structure = new ArrayList<PlanElement>( plan.getPlanElements() );

		for (Trip trip : trips) {
			final int origin = structure.indexOf( trip.getOriginActivity() );
			final int destination = structure.indexOf( trip.getDestinationActivity() );
			final List<PlanElement> tripInPlan = structure.subList( origin + 1 , destination );

			tripInPlan.clear();
			tripInPlan.add(
					new LegImpl(
						mainModeIdentifier.identifyMainMode(
							trip.getTripElements() ) ) );
		}

		return structure;
	}

	public static List<PlanElement> route(
			final PlanAlgorithm planRouter,
			final Person person,
			final List<PlanElement> structure) {
		final InternalPlan plan = planFactory.getInstance();

		plan.setPerson( person );
		plan.setPlanElements( new ArrayList<PlanElement>( structure ) );

		planRouter.run( plan );

		return plan.getPlanElements();
	}

	private static class PooledPlanFactory {
		public InternalPlan getInstance() {
			return new InternalPlan();
		}

		//public void pool(final InternalPlan plan) {
		//	plan.clear();
		//	// TODO pool if cost of construction too high in TMC
		//}
	}

	private static class InternalPlan implements Plan {
		private Person person = null;
		private List<PlanElement> elements = null;

		@Override
		public Person getPerson() {
			return person;
		}

		@Override
		public void setPerson(final Person person) {
			this.person = person;
		}

		public void setPlanElements(final List<PlanElement> elements) {
			this.elements = elements;
		}

		@Override
		public List<PlanElement> getPlanElements() {
			return elements;
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addLeg(Leg leg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addActivity(Activity act) {
			throw new UnsupportedOperationException();
		}

        @Override
        public String getType() {
            return null;
        }

        @Override
        public void setType(String type) {

        }

        @Override
		public boolean isSelected() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setScore(Double score) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Double getScore() {
			throw new UnsupportedOperationException();
		}
	}
}

