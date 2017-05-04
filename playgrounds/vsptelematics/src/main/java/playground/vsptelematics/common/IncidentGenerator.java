/* *********************************************************************** *
 * project: org.matsim.*
 * IncidentGenerator
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
package playground.vsptelematics.common;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IncidentGenerator implements BeforeMobsimListener {

	private Map<Integer, List<NetworkChangeEvent>> changeEvents;

	@Inject
	IncidentGenerator(Config config, Network network) {
		IncidentsReader reader = new IncidentsReader(network);
		TelematicsConfigGroup telematicsConfigGroup = ConfigUtils.addOrGetModule(config,
				TelematicsConfigGroup.GROUPNAME, TelematicsConfigGroup.class);
		changeEvents = reader.read(ConfigGroup.getInputFileURL(config.getContext(), telematicsConfigGroup.getIncidentsFile()).getFile());
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		List<NetworkChangeEvent> events = changeEvents.get(event.getIteration());
		if (events != null) {
            final List<NetworkChangeEvent> events1 = events;
		NetworkUtils.setNetworkChangeEvents(((Network) event.getServices().getScenario().getNetwork()),events1);
		} else
			NetworkUtils.setNetworkChangeEvents(((Network) event.getServices().getScenario().getNetwork()),(List<NetworkChangeEvent>) new LinkedList<NetworkChangeEvent>());
	}

}