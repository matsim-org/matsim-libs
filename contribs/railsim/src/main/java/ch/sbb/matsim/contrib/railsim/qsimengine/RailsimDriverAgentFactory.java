/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentFactory;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;
import org.matsim.pt.transitSchedule.api.TransitStopArea;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory to create specific drivers for the rail engine.
 */
public class RailsimDriverAgentFactory implements TransitDriverAgentFactory {

	private final Set<String> modes;
	private final Map<Id<TransitStopArea>, List<TransitStopFacility>> stopAreas;

	@Inject
	public RailsimDriverAgentFactory(Config config, Scenario scenario) {
		this.modes = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class).getNetworkModes();

		this.stopAreas = scenario.getTransitSchedule().getFacilities().values().stream()
			.filter(t -> t.getStopAreaId() != null)
			.collect(Collectors.groupingBy(TransitStopFacility::getStopAreaId, Collectors.toList()));
	}

	@Override
	public AbstractTransitDriverAgent createTransitDriver(Umlauf umlauf, InternalInterface internalInterface, TransitStopAgentTracker transitStopAgentTracker) {

		String mode = umlauf.getUmlaufStuecke().get(0).getRoute().getTransportMode();

		if (this.modes.contains(mode)) {
			return new RailsimTransitDriverAgent(stopAreas, umlauf, mode, transitStopAgentTracker, internalInterface);
		}

		return new TransitDriverAgentImpl(umlauf, TransportMode.car, transitStopAgentTracker, internalInterface);
	}
}
