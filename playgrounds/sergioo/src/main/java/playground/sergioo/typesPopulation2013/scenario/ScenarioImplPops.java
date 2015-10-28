/* *********************************************************************** *
 * project: org.matsim.*
 * StatelessScenarioImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.sergioo.typesPopulation2013.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;


/**
 * @author dgrether
 * @author mrieser
 */
public class ScenarioImplPops implements Scenario {
	
	private final Scenario delegate ;

	protected ScenarioImplPops(Config config) {
		delegate = ScenarioUtils.createScenario( config );
	}

	@Override
	public Network getNetwork() {
		return delegate.getNetwork();
	}

	@Override
	public Population getPopulation() {
		return delegate.getPopulation();
	}

	@Override
	public TransitSchedule getTransitSchedule() {
		return delegate.getTransitSchedule();
	}

	@Override
	public Config getConfig() {
		return delegate.getConfig();
	}

	@Override
	public void addScenarioElement(String name, Object o) {
		delegate.addScenarioElement(name, o);
	}


	@Override
	public Object getScenarioElement(String name) {
		return delegate.getScenarioElement(name);
	}

	@Override
	public ActivityFacilities getActivityFacilities() {
		return delegate.getActivityFacilities();
	}

	@Override
	public Vehicles getTransitVehicles() {
		return delegate.getTransitVehicles();
	}

	@Override
	public Vehicles getVehicles() {
		return delegate.getVehicles();
	}

	@Override
	public Households getHouseholds() {
		return delegate.getHouseholds();
	}

	@Override
	public LaneDefinitions20 getLanes() {
		return delegate.getLanes();
	}

}
