/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.router;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

/**
 * @author thibaut
 */
public interface MainModeIdentifier {
	/*
	 * Not sure whether ROUTING_MODE_IDENTIFIER should be replaceable. There are lots of classes which use
	 * TripStructureUtils.getRoutingModeIdentifier without injection and it's not clear whether we can 
	 * easily make all of them use injection. Binding a ROUTING_MODE_IDENTIFIER suggests that another
	 * ROUTING_MODE_IDENTIFIER can be bound later and would be used all over Matsim, but it isn't. - gl-nov'19
	 */
//	public static final String ROUTING_MODE_IDENTIFIER = "routingModeIdentifier";
	public static final String ANALYSIS_MAIN_MODE_IDENTIFIER = "analysisMainModeIdentifier";
	public static final String BACKWARD_COMPATIBILITY_ROUTING_MODE_IDENTIFIER = "backwardCompatibilityRoutingModeIdentifier";
	
	String identifyMainMode(List<? extends PlanElement> tripElements);
}