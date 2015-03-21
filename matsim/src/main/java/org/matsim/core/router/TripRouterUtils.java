/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.core.router;

import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author nagel
 *
 */
public class TripRouterUtils {
	private TripRouterUtils(){} // do not instantiate
	
	public static TripRouterFactoryBuilderWithDefaults createDefaultTripRouterFactoryBuilder() {
		return new TripRouterFactoryBuilderWithDefaults() ;
	}
	
	public static RoutingContext createRoutingContext( TravelDisutility utl, TravelTime tim ) {
		return new RoutingContextImpl( utl, tim ) ;
	}

}
