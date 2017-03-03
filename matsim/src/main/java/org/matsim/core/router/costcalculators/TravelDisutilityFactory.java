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

import org.matsim.core.api.internal.MatsimExtensionPoint;
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
 * <li> Seems to me that the above arguments are obsolete with guice ... each inject could pull a new instance.  HOWEVER, it seems that we 
 * either need to pass the TravelTime object into the creator, or at least the mode (so it can pull the correct TravelTime).  Neither is
 * possible with a syntax of type
 * <pre>
 *    addTravelDisutilityBinding(mode).to???</pre>
 * The only option I can think about would be something like
 * <pre>
 * addTravelDistuilityBinding(mode).toProvider( new TravelDisutilityProvider(mode) ) ;
 * </pre>
 * However, MZ says that there are also instances that pass non-standard TravelTime objects into the standard TravelDisutiity, and 
 * such a behavior would be difficult to express when extension point would be the TravelDisutility, and not its factory.  kai, feb'17
 * </ul>
 * 
 * @author dgrether
 *
 */
public interface TravelDisutilityFactory extends MatsimFactory, MatsimExtensionPoint {
	
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator);

}
