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

package playground.ikaddoura.decongestion.data;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.ikaddoura.decongestion.DecongestionConfigGroup;

/**
 * Stores the information which is requried during the computation of decongestion prices
 * 
 * @author ikaddoura
 */

public class DecongestionInfo {
		
	@Inject
	private Scenario scenario;
	
	private final Map<Id<Link>, LinkInfo> linkId2info = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>();

	public Scenario getScenario() {
		return scenario;
	}
	
	public Map<Id<Vehicle>, Id<Person>> getVehicleId2personId() {
		return vehicleId2personId;
	}
	
	public Map<Id<Link>, LinkInfo> getlinkInfos() {
		return linkId2info;
	}

	public DecongestionConfigGroup getDecongestionConfigGroup() {
		return ConfigUtils.addOrGetModule(scenario.getConfig(), DecongestionConfigGroup.class);
	}

}

