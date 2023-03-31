/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.ev.stats;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEvent;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EnergyConsumptionCollector implements DrivingEnergyConsumptionEventHandler, MobsimScopeEventHandler {

	private final Map<Id<Link>, Double> energyConsumptionPerLink = new HashMap<>();

	@Override
	public void handleEvent(DrivingEnergyConsumptionEvent event) {
		energyConsumptionPerLink.merge(event.getLinkId(), event.getEnergy(), Double::sum);
	}

	public Map<Id<Link>, Double> getEnergyConsumptionPerLink() {
		return energyConsumptionPerLink;
	}
}
