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
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * Design remark(s):<ul>
 * <li> These need to be factories since, at least for threads, we may need multiple
 * instances.  (If nothing else, each may set its own person.) kai, mar'12
 * <li> In principle, the above statement is probably no longer correct, since the person is (now) contained 
 * in the getLinkTravelDisutility(...) call.  However, there is a danger that someone memorizes the person in a class-global 
 * (= private) field ... and then the class is no longer thread safe.  Thus, we may be safer by having one instance of
 * of TravelDisutility per router thread.  kai/michaelz/dgrether, apr'13
 * </ul>
 * 
 * @author dgrether
 *
 */
public interface TravelDisutilityFactory extends MatsimFactory {
	
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator);

}
