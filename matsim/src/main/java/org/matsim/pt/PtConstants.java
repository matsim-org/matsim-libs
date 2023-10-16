
/* *********************************************************************** *
 * project: org.matsim.*
 * PtConstants.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.pt;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.groups.ScoringConfigGroup;

/**
 * An abstract class containing some constants used for public transportation.
 *
 * @author mrieser
 */
public abstract class PtConstants implements MatsimParameters {

	/**
	 * Type of an activity that somehow interacts with pt, e.g. to connect a walk leg
	 * to a pt leg, or to connect two pt legs together where agents have to change lines.
	 *
	 * @see Activity#setType(String)
	 */
	public final static String TRANSIT_ACTIVITY_TYPE = ScoringConfigGroup.createStageActivityType(TransportMode.pt);

	// this is currently used for wait2link events where the mode is not clear (bus, rail...?!), theresa sep'2015
	public final static String NETWORK_MODE = "pt unspecified";

}
