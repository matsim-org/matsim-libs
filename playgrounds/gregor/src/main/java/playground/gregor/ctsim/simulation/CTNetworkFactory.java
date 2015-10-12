package playground.gregor.ctsim.simulation;
/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.gregor.ctsim.simulation.physics.CTNetsimEngine;
import playground.gregor.ctsim.simulation.physics.CTNetwork;

/**
 * Created by laemmel on 12/10/15.
 */
public class CTNetworkFactory {
	public CTNetwork createCANetwork(Network network, EventsManager eventsManager, CTNetsimEngine ctNetsimEngine) {
		throw new RuntimeException("implement me!");
	}
}
