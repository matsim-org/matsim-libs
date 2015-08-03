/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.bussharing.passenger;
/**
 * @author  jbischoff
 *
 */
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.api.experimental.events.EventsManager;

public class SBPassengerEngine extends PassengerEngine {

	public SBPassengerEngine(String mode, EventsManager eventsManager, PassengerRequestCreator requestCreator,
			VrpOptimizer optimizer, MatsimVrpContext context) {
		super(mode, eventsManager, requestCreator, optimizer, context);
	}

}
