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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.knowledges.KnowledgesImpl;
import org.matsim.lanes.data.v11.LaneDefinitions;
import org.matsim.lanes.data.v11.LaneDefinitionsImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;


/**
 * @author dgrether
 * @author mrieser
 */
public class ScenarioImpl implements Scenario {
	// setting this to final lead to 97 compile errors (many of them IMO multiple error messages of the same problem). kai, feb'14

	private static final Logger log = Logger.getLogger(ScenarioImpl.class);

	private final Map<String, Object> elements = new HashMap<String, Object>();

	//mandatory attributes
	private final Config config;
	private Network network;
	private Population population;
	private ActivityFacilities facilities;

	private final ConcurrentHashMap<String, Id> idMap = new ConcurrentHashMap<String, Id>();

	//non-mandatory attributes
	private LaneDefinitions laneDefinitions;
	private TransitSchedule transitSchedule = null;

	private Households households;
	private Vehicles vehicles;

	private Knowledges knowledges;

	protected ScenarioImpl(Config config) {
		// setting to "default" results in 6 compile errors.  kai, feb'14
		this.config = config;
		initContainers();
	}

	private void initContainers() {
		this.network = NetworkImpl.createNetwork();
		this.population = new PopulationImpl(this);
		this.facilities = new ActivityFacilitiesImpl();

		if (this.config.scenario().isUseHouseholds()){
			this.createHouseholdsContainer();
		}
		if (this.config.scenario().isUseVehicles()){
			this.createVehicleContainer();
		}
		if (this.config.scenario().isUseLanes()){
			this.createLaneDefinitionsContainer();
		}
		if (this.config.scenario().isUseKnowledges()){
			this.createKnowledges();
		}
		if (this.config.scenario().isUseTransit()) {
			this.createTransitSchedule();
		}
		// this did not do anything (there is no "typed getter" for signals,
		// they are added as a custom scenario element) td, feb'14
		//if (this.config.scenario().isUseSignalSystems()){
		//	this.createSignals();
		//}
	}

	/**
	 * Creates a vehicle container and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useVehicles
	 * config switch to true (see MATSIM-202 for an example).
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	 public boolean createVehicleContainer(){
		if ( this.vehicles != null ) return false;

		if ( !this.config.scenario().isUseVehicles() ) {
			log.info( "creating vehicles container while switch in config set to false. File will not be loaded automatically." );
		}
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
	public boolean createHouseholdsContainer(){
		if ( this.households != null ) return false;

		if ( !this.config.scenario().isUseHouseholds() ) {
			log.info( "creating households container while switch in config set to false. File will not be loaded automatically." );
		}

		this.households = new HouseholdsImpl();
		return true;
	}

	/**
	 * Creates a knowledge container and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useKnowledge
	 * config switch to true.
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	public boolean createKnowledges() {
		if ( this.knowledges != null ) return false;

		if ( !this.config.scenario().isUseKnowledges() ) {
			log.info( "creating knowledges container while switch in config set to false. File will not be loaded automatically." );
		}

		this.knowledges = new KnowledgesImpl();
		return true;
	}

	/**
	 * Creates a LaneDefinition container and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useLanes
	 * config switch to true.
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	public boolean createLaneDefinitionsContainer() {
		if ( this.laneDefinitions != null ) return false;

		if ( !this.config.scenario().isUseLanes() ) {
			log.info( "creating lanes definitions container while switch in config set to false. File will not be loaded automatically." );
		}

		this.laneDefinitions = new LaneDefinitionsImpl();
		return true;
		// thought: this is not the responsibility of the scenario to add 
		// custom elements (the idea being that those elements are not known.
		// td, oct 2013
//		this.addScenarioElement(
//				LaneDefinitions20.ELEMENT_NAME,
//				new LaneDefinitions20Impl());
	}

	/**
	 * Creates a transit schedule and stores it, if it does not exist.
	 * This is necessary only in very special use cases, when one needs
	 * to create such a container <b>without</b> setting the useTransit
	 * config switch to true (see MATSIM-220 for an example).
	 *
	 * @return true if a new container was initialized, false otherwise
	 */
	 public boolean createTransitSchedule() {
		 if ( this.transitSchedule != null ) return false;

		if ( !this.config.scenario().isUseTransit() ) {
			log.info( "creating transit schedule while switch in config set to false. File will not be loaded automatically." );
		}

		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		return true;
	}
	
	@Override
	public ActivityFacilities getActivityFacilities() {
		return this.facilities;
	}

	@Override
	public Network getNetwork() {
		return this.network;
	}

	@Override
	public Population getPopulation() {
		return this.population;
	}

	@Override
	public Coord createCoord(final double d, final double e) {
		return new CoordImpl( d, e ) ;
	}

	@Override
	public Id createId(final String string) {
		Id id = this.idMap.get(string);
		if (id == null) {
			id = new IdImpl(string);
			this.idMap.put(string, id);
		}
		return id;
	}

	@Override
	public Config getConfig() {
		return this.config;
	}

	@Deprecated
	public void setNetwork(Network network) {
		this.network = network;
	}

	@Deprecated
	public void setPopulation(Population population) {
		this.population = population;
	}

	public LaneDefinitions getLaneDefinitions11() {
		if ((this.laneDefinitions == null) && this.config.scenario().isUseLanes()){
			this.createLaneDefinitionsContainer();
		}

		return laneDefinitions;
	}

	@Override
	public Households getHouseholds() {
		if ((this.households == null) && this.config.scenario().isUseHouseholds()){
			this.createHouseholdsContainer();
		}

		return this.households;
	}
	
	@Override
	public Vehicles getVehicles(){
		if ((this.vehicles == null) && this.config.scenario().isUseVehicles()){
			this.createVehicleContainer();
		}

		return this.vehicles;
	}
	
	/**
	 * @deprecated use {@link Population#getPersonAttributes()} instead.
	 */
	@Deprecated // use population.getPopulationAttributes instead
	public Knowledges getKnowledges(){
		if ((this.knowledges == null) && this.config.scenario().isUseKnowledges()){
			this.createKnowledges();
		}

		return this.knowledges;
	}

	@Override
	public TransitSchedule getTransitSchedule() {
		if ((this.transitSchedule == null) && this.config.scenario().isUseTransit()){
			this.createTransitSchedule();
		}

		return this.transitSchedule;
	}

	@Override
	public void addScenarioElement(
			final String name,
			final Object o) {
		if ( o == null ) throw new NullPointerException( name );
		final Object former = elements.put( name , o );
		if ( former != null ) {
			throw new IllegalStateException( former+" is already associated with name "+name+" when adding "+o );
		}
	}

	@Override
	public Object removeScenarioElement(final String name) {
		return elements.remove( name );
	}

	@Override
	public Object getScenarioElement(final String name) {
		return elements.get( name );
	}

}
