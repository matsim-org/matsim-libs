/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.contrib.minibus.hook;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;


/**
 * @author droeder
 *
 */
abstract class AbstractPersonReRouteStuck extends AbstractPersonAlgorithm  {
	
	final PlanAlgorithm router;

    private static final Logger log = Logger.getLogger(PersonReRouteStuck.class);
	
	final Set<Id<Person>> agentsStuck;

	AbstractPersonReRouteStuck(final PlanAlgorithm router, final MutableScenario scenario, Set<Id<Person>> agentsStuck) {
		super();
		this.router = router;
        Network network = scenario.getNetwork();
		Network net = network;
		if (NetworkUtils.isMultimodal(network)) {
			log.info("Network seems to be multimodal. XY2Links will only use car links.");
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
			net = NetworkUtils.createNetwork();
			HashSet<String> modes = new HashSet<>();
			modes.add(TransportMode.car);
			filter.filter(net, modes);
		}
		this.agentsStuck = agentsStuck;
		log.info("initialized");
	}

}

