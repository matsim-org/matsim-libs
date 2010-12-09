/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentalHybridMobsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;


/**Design thoughts:<ul>
 * <li>QSim creates network which needs to work for NetsimEngine.
 * <li>NetsimEngine uses this network.
 * <li>2Dengine uses the "service" part of the network (the NetsimNetwork interface)
 * <li> -----
 * <li>Mobsim produces table which engine is responsible for which link.
 * <li>Engine, at moveOverNode, may find that it is not longer responble and calls something like<ul>
 * <li>if ( this.getMobsim().otherLinkHasSpace() ) {
 * <li>    this.getMobsim().engineNoLongerResponsible( agent ) ;
 * </ul>
 * </ul>
 * @author nagel, laemmel
 */
public class ExperimentalHybridMobsim extends QSim {

	public ExperimentalHybridMobsim(Scenario scenario, EventsManager events) {
		super(scenario, events);
		// TODO Auto-generated constructor stub
	}

}
