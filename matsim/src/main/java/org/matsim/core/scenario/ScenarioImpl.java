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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;


/**
 * @author dgrether
 * @author mrieser
 */
public final class ScenarioImpl implements Scenario {
	// setting this to final lead to 97 compile errors (many of them IMO multiple error messages of the same problem). kai, feb'14

	private static final Logger log = Logger.getLogger(ScenarioImpl.class);

	private boolean locked = false ;

	private final Map<String, Object> elements = new HashMap<String, Object>();

	//mandatory attributes
	private final Config config;
	private Network network;
	private Population population;
	private ActivityFacilities facilities;

	//non-mandatory attributes
	private TransitSchedule transitSchedule = null;

	private Households households;
	private Vehicles transitVehicles;

	private Vehicles vehicles ;

	protected ScenarioImpl(Config config) {
		this.config = config;
		this.network = NetworkImpl.createNetwork();
		this.population = PopulationUtils.createPopulation(this.config, this.network);
		this.facilities = new ActivityFacilitiesImpl();
		if (this.config.scenario().isUseHouseholds()){
			this.createHouseholdsContainer();
		}
		if (this.config.scenario().isUseTransit()) {
			this.createTransitSchedule();
			this.createTransitVehicleContainer();
		}
	}

	/**
	 * Creates a vehicle container and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useVehicles
	 * config switch to true (see MATSIM-202 for an example).
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	public final boolean createTransitVehicleContainer(){
		if ( this.transitVehicles != null ) return false;

		if ( !this.config.scenario().isUseTransit() ) {
			log.info( "creating transit vehicles container while transit switch in config set to false. File will not be loaded automatically." );
		}
		this.transitVehicles = VehicleUtils.createVehiclesContainer();
		return true;
	}
	public final boolean createVehicleContainer(){
		if ( this.vehicles != null ) return false;
		this.vehicles = VehicleUtils.createVehiclesContainer();
		return true;
	}

	/**
	 * Creates a household container and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useHouseholds
	 * config switch to true.
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	public final boolean createHouseholdsContainer(){
		if ( this.households != null ) return false;

		if ( !this.config.scenario().isUseHouseholds() ) {
			log.info( "creating households container while switch in config set to false. File will not be loaded automatically." );
		}

		this.households = new HouseholdsImpl();
		return true;
	}

	/**
	 * Creates a transit schedule and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useTransit
	 * config switch to true (see MATSIM-220 for an example).
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	public final boolean createTransitSchedule() {
		if ( this.transitSchedule != null ) return false;

		if ( !this.config.scenario().isUseTransit() ) {
			log.info( "creating transit schedule while switch in config set to false. File will not be loaded automatically." );
		}

		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		return true;
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
	public final Coord createCoord(final double d, final double e) {
		return new CoordImpl( d, e ) ;
	}

	@Override
	public final Config getConfig() {
		// yy should throw an exception if null. kai, based on https://matsim.atlassian.net/browse/MATSIM-301 , may'15
		return this.config;
	}

	@Deprecated // please use ScenarioUtils.ScenarioBuilder
	public final void setNetwork(Network network) {
		testForLocked();
		this.network = network;
	}

	@Deprecated // please use ScenarioUtils.ScenarioBuilder
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
		// yy should throw an exception if null. kai, based on https://matsim.atlassian.net/browse/MATSIM-301 , may'15
		if ( this.households == null ) {
			if ( this.config.scenario().isUseHouseholds() ) {
				this.createHouseholdsContainer();
			}
			else {
				// throwing an exception should be the right approach,
				// but it requires some testing (there may be places in the code
				// which are happy with getting a null pointer, and would then
				// not work anymore)
				// throw new IllegalStateException(
				log.info(
						"no households, and households not activated from config. You must first call the create method of ScenarioImpl." );
			}
		}

		return this.households;
	}

	@Override
	public final Vehicles getTransitVehicles() {
		// yy should throw an exception if null. kai, based on https://matsim.atlassian.net/browse/MATSIM-301 , may'15
		if ( this.transitVehicles == null ) {
			if ( this.config.scenario().isUseTransit() ) {
				this.createTransitVehicleContainer();
			}
			else {
				// throwing an exception should be the right approach,
				// but it requires some testing (there may be places in the code
				// which are happy with getting a null pointer, and would then
				// not work anymore)
				// throw new IllegalStateException(
				log.info(
						"no transit vehicles container, and transit not activated from config. You must first call the create method of ScenarioImpl." );
			}
		}

		return this.transitVehicles;
	}

	@Override
	final public Vehicles getVehicles() {
		// yy should throw an exception if null. kai, based on https://matsim.atlassian.net/browse/MATSIM-301 , may'15
		if ( this.vehicles == null ) {
				// throwing an exception should be the right approach,
				// but it requires some testing (there may be places in the code
				// which are happy with getting a null pointer, and would then
				// not work anymore)
				// throw new IllegalStateException(
				log.info(
						"no vehicles container, and vehicles not activated from config. You must first call the create method of ScenarioImpl." );
		}

		return this.vehicles;
	}



	@Override
	public final TransitSchedule getTransitSchedule() {
		// yy should throw an exception if null. kai, based on https://matsim.atlassian.net/browse/MATSIM-301 , may'15
		if ( this.transitSchedule == null ) {
			if ( this.config.scenario().isUseTransit() ) {
				this.createTransitSchedule();
			}
			else {
				// throwing an exception should be the right approach,
				// but it requires some testing (there may be places in the code
				// which are happy with getting a null pointer, and would then
				// not work anymore)
				//				 throw new IllegalStateException(
				log.info(
						"no transit schedule, and transit not activated from config. You must first call the create method of ScenarioImpl." );
				// yyyy Could we please avoid warnings in logfiles that one cannot get rid of?
				// Why is returning null such a problem?  If you do as you are doing here, there is no way to test of the transit schedule exists:
				// you could have created it but it is switched off in the config.
				// ???
				// kai, nov'14
			}
		}

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

	@Override
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
			throw new RuntimeException( "ScenarioImpl is locked; too late to do this.  See comments in code.") ;
			/* The decision is roughly as follows:
			 * - It is ok to set network, population, etc. in the Scenario during initial demand generation.
			 * - It is NOT ok to do this once the controler is running.
			 * - But we do not want to make a defensive copy of the whole thing at controler startup.
			 * - We also want to be able to plug alternative Scenario implementations into the controler.
			 * - But then the controler only gets the "Scenario" not the "ScenarioImpl", so it does not have to worry about setNetwork and the lik
			 * since it does not exist in the published interface.
			 * kai, sep'14
			 */
		}
	}

	// the following are there for ScenarioUtils.ScenarioBuilder.  They are deliberately package-private; please do not change. kai, nov'14
	final void setActivityFacilities( ActivityFacilities facilities ) {
		testForLocked() ;
		this.facilities = facilities ;
	}
	final void setHouseholds( Households households ) {
		testForLocked() ;
		this.households = households ;
	}
	final void setTransitSchedule( TransitSchedule schedule ) {
		this.transitSchedule = schedule ;
	}
	final void setTransitVehicles( Vehicles vehicles ) {
		this.transitVehicles = vehicles ;
	}

}
