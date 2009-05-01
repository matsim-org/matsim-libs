/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.roadpricing;

import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author dgrether
 */
public class RoadPricingUtilities {

	public static boolean hasActInTollArea(Plan plan1,
			RoadPricingScheme roadPricingScheme) {
		for (PlanElement pe : plan1.getPlanElements()) {
			if (pe instanceof BasicActivity) {
				BasicActivity act = (BasicActivity) pe;
				if (roadPricingScheme.getLinkIds().contains(act.getLinkId())){
					return true;
				}
			}
		}
		return false;
	}

}
