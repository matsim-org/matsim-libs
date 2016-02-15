/* *********************************************************************** *
 * project: org.matsim.*
 * TransitActRemoverCorrectTravelTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * replaces transit trips by single legs, with time information set
 * to reflect the departure and travel time of the trip.
 *
 * @author thibautd
 */
public class TransitActRemoverCorrectTravelTime implements PlanAlgorithm {

	@Override
	public void run(final Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();
		List<PlanElement> newPlanElements = new ArrayList<PlanElement>();

		PlanElement lastAddedElement = null;
		for (PlanElement pe : planElements) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if ( !PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType()) ) {
					newPlanElements.add( act );
					lastAddedElement = act;
				}
				else if (!( lastAddedElement instanceof Leg) ) {
					throw new RuntimeException( "reaching a pt activity, but previous added element is not a Leg" );
				}
				else {
					accumulateTravelTime(
							(Leg) lastAddedElement,
							pe);
				}
			} else if (pe instanceof Leg) {
				if ( lastAddedElement instanceof Activity ) {
					newPlanElements.add( pe );
					lastAddedElement = pe;
				}
				else {
					accumulateTravelTime(
							(Leg) lastAddedElement,
							pe);
				}
			}
		}

		planElements.clear();
		planElements.addAll( newPlanElements );
	}

	private static void accumulateTravelTime(
			final Leg leg,
			final PlanElement elementToRemove) {
		leg.setRoute( null );
		leg.setMode( TransportMode.pt );
		double tt = leg.getTravelTime();
		double ttSurplus = 0;

		if (elementToRemove instanceof Leg) {
			ttSurplus = ((Leg) elementToRemove).getTravelTime();
		}
		else {
			ttSurplus = ((Activity) elementToRemove).getEndTime() - ((Activity) elementToRemove).getStartTime();
		}

		leg.setTravelTime( tt + ttSurplus );
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
