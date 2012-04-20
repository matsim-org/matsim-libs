/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.withinday.events;

import org.matsim.core.api.experimental.events.PersonEvent;

/**
 * A ReplanningEvent documents any change in an agent's plan by a within-day replanner.
 * 
 * @author cdobler
 */
public interface ReplanningEvent extends PersonEvent {
	
	public String getReplannerType();
}
