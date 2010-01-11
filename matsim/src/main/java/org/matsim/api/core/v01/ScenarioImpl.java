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
package org.matsim.api.core.v01;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.knowledges.KnowledgesImpl;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.lanes.basic.BasicLaneDefinitionsImpl;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.signalsystems.basic.BasicSignalSystems;
import org.matsim.signalsystems.basic.BasicSignalSystemsImpl;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsImpl;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vehicles.BasicVehiclesImpl;
import org.matsim.world.World;


/**
 * @author dgrether
 */
public class ScenarioImpl implements Scenario {

	private static final Logger log = Logger.getLogger(ScenarioImpl.class);
	
	private static final String NON_ENABLED_ATTRIBUTE_WARNING = "Trying to retrieve not enabled scenario feature, have you enabled the feature in ScenarioConfigGroup?";
	
	//mandatory attributes 
	private Config config;
	private NetworkLayer network;
	private PopulationImpl population;
	private ActivityFacilitiesImpl facilities;
	
	//non-mandatory attributes
	private BasicLaneDefinitions laneDefinitions;
	private BasicSignalSystems signalSystems;
	private BasicSignalSystemConfigurations signalSystemConfigurations;
	private RoadPricingScheme roadPricingScheme;
	private TransitSchedule transitSchedule = null;
	
	private Households households;
  private BasicVehicles vehicles;
  
  private Knowledges knowledges;
  
  private World world;
	
	public ScenarioImpl(){
		this.config = new Config();
		this.config.addCoreModules();
		Gbl.setConfig(config);
		initContainers();
	}
	
	public ScenarioImpl(Config config) {
		this.config = config;
		initContainers();
	}
	
	private void initContainers() {
		this.world = new World();
		this.network = new NetworkLayer();
		this.world.setNetworkLayer((NetworkLayer) this.network);
		this.world.complete();
		this.population = new PopulationImpl(this);
		this.facilities = new ActivityFacilitiesImpl();
		this.world.setFacilityLayer((ActivityFacilitiesImpl) this.facilities);
	
		if (this.config.scenario().isUseHouseholds()){
			this.createHouseholdsContainer();
		}
		if (this.config.scenario().isUseVehicles()){
			this.createVehicleContainer();
		}
		if (this.config.scenario().isUseLanes()){
			this.createLaneDefinitionsContainer();
		}
		if (this.config.scenario().isUseSignalSystems()){
			this.createSignalSystemsContainers();
		}
		if (this.config.scenario().isUseRoadpricing()){
			this.createRoadPricingScheme();
		}
		if (this.config.scenario().isUseKnowledges()){
			this.createKnowledges();
		}
		if (this.config.scenario().isUseTransit()) {
			this.createTransit();
		}
	}
	
	protected void createVehicleContainer(){
		this.vehicles = new BasicVehiclesImpl();
	}
	
	protected void createHouseholdsContainer(){
		this.households = new HouseholdsImpl();
	}
	
	protected void createKnowledges() {
		this.knowledges = new KnowledgesImpl();
	}

	protected void createRoadPricingScheme() {
		this.roadPricingScheme = new RoadPricingScheme();		
	}
	
	protected void createLaneDefinitionsContainer() {
		this.laneDefinitions = new BasicLaneDefinitionsImpl();
	}
	
	protected void createSignalSystemsContainers() {
		this.signalSystems = new BasicSignalSystemsImpl();
		this.signalSystemConfigurations = new BasicSignalSystemConfigurationsImpl();
	}
	
	protected void createTransit() {
		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
	}
	
	@Deprecated
	public World getWorld() {
		return this.world;
	}
	
	public ActivityFacilitiesImpl getActivityFacilities() {
		return this.facilities;
	}

	public NetworkLayer getNetwork() {
		return this.network;
	}

	public PopulationImpl getPopulation() {
		return this.population;
	}

	public Coord createCoord(final double d, final double e) {
		return new CoordImpl( d, e ) ;
	}

	public Id createId(final String string) {
		return new IdImpl( string) ;
	}

	public Config getConfig() {
		return this.config;
	}

	
	public BasicLaneDefinitions getLaneDefinitions() {
		if ((this.laneDefinitions == null) && this.config.scenario().isUseLanes()){
			this.createLaneDefinitionsContainer();
		}
		else if (!this.config.scenario().isUseLanes()){
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return laneDefinitions;
	}

	
	public BasicSignalSystems getSignalSystems() {
		if ((this.signalSystems == null) && this.config.scenario().isUseSignalSystems()){
			this.createSignalSystemsContainers();
		}
		else if (!this.config.scenario().isUseSignalSystems()){
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return signalSystems;
	}

	
	public BasicSignalSystemConfigurations getSignalSystemConfigurations() {
		if ((this.signalSystemConfigurations == null) && this.config.scenario().isUseSignalSystems()){
			this.createSignalSystemsContainers();
		}
		else if (!this.config.scenario().isUseSignalSystems()){
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return signalSystemConfigurations;
	}

	
	public RoadPricingScheme getRoadPricingScheme() {
		if ((this.roadPricingScheme == null) && this.config.scenario().isUseRoadpricing()){
			this.createRoadPricingScheme();
		}
		else if (!this.config.scenario().isUseRoadpricing()){
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return roadPricingScheme;
	}

	@Deprecated
	public void setNetwork(NetworkLayer network2) {
		this.network = network2;
	}

	@Deprecated
	public void setPopulation(PopulationImpl population2) {
		this.population = population2;
	}

	public Households getHouseholds() {
		if ((this.households == null) && this.config.scenario().isUseHouseholds()){
			this.createHouseholdsContainer();
		}
		else if (!this.config.scenario().isUseHouseholds()){
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return this.households;
	}
	
	public BasicVehicles getVehicles(){
		if ((this.vehicles == null) && this.config.scenario().isUseVehicles()){
			this.createVehicleContainer();
		}
		else if (!this.config.scenario().isUseVehicles()){
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return this.vehicles;
	}
	
	public Knowledges getKnowledges(){
		if ((this.knowledges == null) && this.config.scenario().isUseKnowledges()){
			this.createKnowledges();
		}
		else if (!this.config.scenario().isUseKnowledges()){
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return this.knowledges;
	}

	public TransitSchedule getTransitSchedule() {
		if ((this.transitSchedule == null) && this.config.scenario().isUseTransit()){
			this.createTransit();
		}
		else if (!this.config.scenario().isUseTransit()) {
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return this.transitSchedule;
	}

}
