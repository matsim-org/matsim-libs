/* *********************************************************************** *
 * project: org.matsim.*
 * PlanLegsTravelTimeCalculator.java
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

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.anhorni.locationchoice.preprocess.helper.Utils;

public class PlanLegsTravelTimeCalculator  extends PlanLegsTravelMeasureCalculator {
		
	@Override
	public List<Double> handle(final Plan plan, boolean wayThere) {		
		super.reset();		
		final List<?> actslegs = plan.getPlanElements();
				
		for (int j = 1; j < actslegs.size(); j=j+2) {			
			if (actslegs.get(j) instanceof LegImpl) {
				LegImpl leg = (LegImpl) actslegs.get(j);
				ActivityImpl actPost = (ActivityImpl)actslegs.get(j + 1);
				
				// act type
				// act type
				String actType = actPost.getType();
				if (!wayThere) {
					actType = Utils.getActType(plan, actPost);
				}
				if (super.actType.equals("all") || actType.startsWith(this.actType)) {
					
					// mode
					if ((this.mode.equals("all") || leg.getMode().toString().equals(this.mode)) &&
							!this.actType.equals("tta")) {
						super.sumLegsTravelMeasure += leg.getArrivalTime() - leg.getDepartureTime();
						super.nbrOfLegs++;
						super.legTravelMeasures.add(leg.getArrivalTime() - leg.getDepartureTime());
					}	
				}
			}	
		}
		return super.legTravelMeasures;
	}	
}
