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
package org.matsim.core.scenario;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LaneDefinitions20Impl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;


/**
 * @author dgrether
 * @author mrieser
 */
public final class MutableScenario implements Scenario {
	private static final Logger log = Logger.getLogger(MutableScenario.class);

	private boolean locked = false ;

	private final Map<String, Object> elements = new HashMap<String, Object>();

	//mandatory attributes
	private final Config config;
	private Network network;
	private Population population;
	private ActivityFacilities facilities;

	//non-mandatory attributes
	private TransitSchedule transitSchedule = null;
	private Lanes lanes = null;
	private Households households;
	private Vehicles transitVehicles;

	private Vehicles vehicles ;

	MutableScenario(Config config) {
		this.config = config;
		this.network = NetworkUtils.createNetwork(this.config);
		this.population = PopulationUtils.createPopulation(this.config, this.network);
		this.facilities = new ActivityFacilitiesImpl();
		this.households = new HouseholdsImpl();
		this.lanes = new LaneDefinitions20Impl();
		this.vehicles = VehicleUtils.createVehiclesContainer();
		this.transitVehicles = VehicleUtils.createVehiclesContainer();
		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
	}

	@Override
	public final ActivityFacilities getActivityFacilities() {
		return this.facilities;
	}

	@Override
	public final Network getNetwork() {
		return this.network;
	}

	@Override
	public final Population getPopulation() {
		return this.population;
	}

	@Override
	public final Config getConfig() {
		// yy should throw an exception if null. kai, based on https://matsim.atlassian.net/browse/MATSIM-301 , may'15
		return this.config;
	}

	public final void setNetwork(Network network) {
		testForLocked();
		this.network = network;
	}

	public final void setPopulation(Population population) {
		testForLocked();
		this.population = population;
	}

	// NOTE: Thibaut is not so happy about just returning null, since someone may code something like
	// Vehicles vehicles = scenario.getVehicles() ; // returning null
	// ... // lots of code
	// ... vehicles.getVehicles() ... // throwing a null pointer exception
	// That is, in such a situation the null pointer exception is thrown much later than when the null pointer was obtained.
	// He rather wants to have this fail when it is called.
	// I (kn) am a bit sceptic if it makes sense to establish such a convention when most other people use "if ... == null" use as the
	// official dialect to check if something is there (since this would cause an exception with Thibaut's approach). kai, feb'15

	@Override
	public final Households getHouseholds() {
		return this.households;
	}

	@Override
	public Lanes getLanes() {
		return this.lanes;
	}

	@Override
	public final Vehicles getTransitVehicles() {
		return this.transitVehicles;
	}

	@Override
	final public Vehicles getVehicles() {
		return this.vehicles;
	}



	@Override
	public final TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}

	@Override
	public final void addScenarioElement(
			final String name,
			final Object o) {
		// Once the "removal" operation is locked, you cannot add under the same name. kai, sep'14
		if ( o == null ) throw new NullPointerException( name );
		final Object former = elements.put( name , o );
		if ( former != null ) {
			throw new IllegalStateException( former+" is already associated with name "+name+" when adding "+o );
		}
	}

	public final Object removeScenarioElement(final String name) {
		testForLocked();
		return elements.remove( name );
	}

	@Override
	public final Object getScenarioElement(final String name) {
		// yy should throw an exception if null. kai, based on https://matsim.atlassian.net/browse/MATSIM-301 , may'15
		return elements.get( name );
	}

	public final void setLocked() {
		this.locked = true ;
	}

	private void testForLocked() {
		if ( locked ) {
			throw new RuntimeException( "Scenario is locked; too late to do this.  See comments in code.") ;
			/* The decision is roughly as follows:
			 * - It is ok to set network, population, etc. in the Scenario during initial demand generation.
			 * - It is NOT ok to do this once the services is running.
			 * - But we do not want to make a defensive copy of the whole thing at services startup.
			 * - We also want to be able to plug alternative Scenario implementations into the services.
			 * - But then the services only gets the "Scenario" not the "MutableScenario", so it does not have to worry about setNetwork and the like
			 * since it does not exist in the published interface.
			 * kai, sep'14
			 */
		}
	}

	public final void setActivityFacilities( ActivityFacilities facilities ) {
		testForLocked() ;
		this.facilities = facilities ;
	}
	public final void setHouseholds( Households households ) {
		testForLocked() ;
		this.households = households ;
	}
	public final void setTransitSchedule( TransitSchedule schedule ) {
		testForLocked();
		this.transitSchedule = schedule ;
	}
	public final void setTransitVehicles( Vehicles vehicles ) {
		testForLocked();
		this.transitVehicles = vehicles ;
	}
	public final void setLanes( Lanes lanes ) {
		testForLocked();
		this.lanes = lanes ;
	}

}
