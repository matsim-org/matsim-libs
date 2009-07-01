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
package org.matsim.core.api.experimental;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.network.Network;
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
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vehicles.BasicVehiclesImpl;
import org.matsim.world.World;


/**
 * @author dgrether
 *
 */
public class ScenarioImpl implements Scenario {

	//mandatory attributes 
	private Config config;
	private Network network;
	private Population population;
	private ActivityFacilities facilities;
	
	//non-mandatory attributes
	private BasicLaneDefinitions laneDefinitions;
	private BasicSignalSystems signalSystems;
	private BasicSignalSystemConfigurations signalSystemConfigurations;
	private RoadPricingScheme roadPricingScheme;
	
	private Households households;
  private BasicVehicles vehicles;
  
  private Knowledges knowledges; 
	
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
	
	private void initContainers(){
		this.network = new NetworkLayer();
		this.getWorld().setNetworkLayer((NetworkLayer) this.network);
		this.getWorld().complete();
		this.population = new PopulationImpl();
		this.facilities = new ActivityFacilitiesImpl();
		this.getWorld().setFacilityLayer((ActivityFacilitiesImpl) this.facilities);
	
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

	protected void createRoadPricingScheme(){
		this.roadPricingScheme = new RoadPricingScheme(this.getNetwork());		
	}
	
	protected void createLaneDefinitionsContainer(){
		this.laneDefinitions = new BasicLaneDefinitionsImpl();
	}
	
	protected void createSignalSystemsContainers(){
		this.signalSystems = new BasicSignalSystemsImpl();
		this.signalSystemConfigurations = new BasicSignalSystemConfigurationsImpl();
	}
	
	@Deprecated
	public World getWorld(){
		return Gbl.getWorld();
	}
	
	
	/**
	 * @see org.matsim.core.api.experimental.Scenario#getActivityFacilities()
	 */
	public ActivityFacilities getActivityFacilities() {
		return this.facilities;
	}

	/**
	 * @see org.matsim.core.api.experimental.Scenario#getNetwork()
	 */
	public Network getNetwork() {
		return this.network;
	}

	/**
	 * @see org.matsim.core.api.experimental.Scenario#getPopulation()
	 */
	public Population getPopulation() {
		return this.population;
	}

	/**
	 * @see org.matsim.api.basic.v01.BasicScenario#createCoord(double, double)
	 */
	public Coord createCoord(final double d, final double e) {
		return new CoordImpl( d, e ) ;
	}


	/**
	 * @see org.matsim.api.basic.v01.BasicScenario#createId(java.lang.String)
	 */
	public Id createId(final String string) {
		return new IdImpl( string) ;
	}

	/**
	 * @see org.matsim.api.basic.v01.BasicScenario#getConfig()
	 */
	public Config getConfig() {
		return this.config;
	}

	
	public BasicLaneDefinitions getLaneDefinitions() {
		if ((this.laneDefinitions == null) && this.config.scenario().isUseLanes()){
			this.createLaneDefinitionsContainer();
		}
		return laneDefinitions;
	}

	
	public BasicSignalSystems getSignalSystems() {
		if ((this.signalSystems == null) && this.config.scenario().isUseSignalSystems()){
			this.createSignalSystemsContainers();
		}
		return signalSystems;
	}

	
	public BasicSignalSystemConfigurations getSignalSystemConfigurations() {
		if ((this.signalSystemConfigurations == null) && this.config.scenario().isUseSignalSystems()){
			this.createSignalSystemsContainers();
		}
		return signalSystemConfigurations;
	}

	
	public RoadPricingScheme getRoadPricingScheme() {
		if ((this.roadPricingScheme == null) && this.config.scenario().isUseRoadpricing()){
			this.createRoadPricingScheme();
		}
		return roadPricingScheme;
	}

	@Deprecated
	public void setNetwork(NetworkLayer network2) {
		this.network = network2;
	}

	@Deprecated
	public void setPopulation(Population population2) {
		this.population = population2;
	}

	public Households getHouseholds() {
		if ((this.households == null) && this.config.scenario().isUseHouseholds()){
			this.createHouseholdsContainer();
		}
		return this.households;
	}
	
	public BasicVehicles getVehicles(){
		if ((this.vehicles == null) && this.config.scenario().isUseVehicles()){
			this.createVehicleContainer();
		}
		return this.vehicles;
	}
	
	public Knowledges getKnowledges(){
		if ((this.knowledges == null) && this.config.scenario().isUseKnowledges()){
			this.createKnowledges();
		}
		return this.knowledges;
	}

}
