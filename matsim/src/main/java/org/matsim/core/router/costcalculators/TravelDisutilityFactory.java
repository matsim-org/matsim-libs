/* *********************************************************************** *
 * project: org.matsim.*
 * TravelCostCalculatorFactory
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
package org.matsim.core.router.costcalculators;

import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;

/**
 * Design remark(s):<ul>
 * <li> These need to be factories since, at least for threads, we may need multiple
 * instances.  (If nothing else, each may set its own person.) kai, mar'12
 * </ul>
 * 
 * @author dgrether
 *
 */
public interface TravelDisutilityFactory extends MatsimFactory {
	
	public PersonalizableTravelDisutility createTravelDisutility(PersonalizableTravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup);

}
