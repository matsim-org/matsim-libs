/* *********************************************************************** *
 * project: org.matsim.*
 * PlanLegsTravelDistanceCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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


package playground.anhorni.locationchoice.analysis.plans;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.locationchoice.preprocess.helper.Utils;

public class PlanLegsTravelDistanceCalculator  extends PlanLegsTravelMeasureCalculator {

	public PlanLegsTravelDistanceCalculator(boolean crowFly) {
		super();
		super.crowFly = crowFly;
	}


	@Override
	public List<Double> handle(final Plan plan, boolean wayThere) {
		super.reset();
		final List<?> actslegs = plan.getPlanElements();

		for (int j = 1; j < actslegs.size(); j=j+2) {
			if (actslegs.get(j) instanceof Leg) {
				Leg leg = (Leg) actslegs.get(j);
				Activity actStartLeg = (ActivityImpl)actslegs.get(j-1);
				Activity actEndLeg = (ActivityImpl)actslegs.get(j+1);

				// act type
				String actType = actEndLeg.getType();
				if (!wayThere) {
					actType = Utils.getActType(plan, actEndLeg);
				}
				if (super.actType.equals("all") || actType.startsWith(this.actType)) {

					// mode
					if ((this.mode.equals("all") || leg.getMode().toString().equals(this.mode)) &&
							!this.actType.equals("tta")) {

						if (leg.getMode().toString().equals("car") && !this.crowFly) {
							double dist = leg.getRoute().getDistance();
							super.sumLegsTravelMeasure += dist;
							super.legTravelMeasures.add(dist);
						}
						else {
							double dist = ((CoordImpl)actStartLeg.getCoord()).calcDistance(actEndLeg.getCoord());
							super.sumLegsTravelMeasure += dist;
							super.legTravelMeasures.add(dist);
						}
						super.nbrOfLegs++;
					}
				}
			}
		}
		return super.legTravelMeasures;
	}
}
