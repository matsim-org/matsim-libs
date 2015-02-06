/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.congestionPricing;

import org.matsim.core.events.handler.EventHandler;

/**
 * A common congestion pricing handler to handle different implementations of congestion pricing schemes.
 * Available options are -
 *
 * <list>
 * <li> implv3
 * <li> implv4
 * <li> implv6
 * </list>
 * 
 * @author amit
 */

public interface CongestionPricingHandler extends EventHandler {
	
	public void writeCongestionStats(String outputFile);
	
}
