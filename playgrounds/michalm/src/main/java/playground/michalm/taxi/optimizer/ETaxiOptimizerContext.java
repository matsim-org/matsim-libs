/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;

import playground.michalm.ev.data.EvData;
import playground.michalm.taxi.scheduler.ETaxiScheduler;

public class ETaxiOptimizerContext extends TaxiOptimizerContext {
	public final EvData evData;

	public ETaxiOptimizerContext(Fleet fleet, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, ETaxiScheduler scheduler, EvData evData) {
		super(fleet, network, timer, travelTime, travelDisutility, scheduler);
		this.evData = evData;
	}
}
