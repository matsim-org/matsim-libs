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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.network.BasicLaneDefinitions;
import org.matsim.core.basic.network.BasicLaneDefinitionsImpl;
import org.matsim.core.basic.signalsystems.BasicSignalSystems;
import org.matsim.core.basic.signalsystems.BasicSignalSystemsImpl;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurationsImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.FacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.roadpricing.RoadPricingScheme;
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
	private Facilities facilities;
	//non-mandatory attributes
	private BasicLaneDefinitions laneDefinitions;
	private BasicSignalSystems signalSystems;
	private BasicSignalSystemConfigurations signalSystemConfigurations;
	private RoadPricingScheme roadPricingScheme;
	
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
		this.facilities = new FacilitiesImpl();
		this.getWorld().setFacilityLayer((FacilitiesImpl) this.facilities);
		if (this.config.scenario().isUseLanes()){
			this.laneDefinitions = new BasicLaneDefinitionsImpl();
		}
		if (this.config.scenario().isUseSignalSystems()){
			this.signalSystems = new BasicSignalSystemsImpl();
			this.signalSystemConfigurations = new BasicSignalSystemConfigurationsImpl();
		}
		if (this.config.scenario().isUseRoadpricing()){
			this.roadPricingScheme = new RoadPricingScheme(this.getNetwork());
		}
	}
	
	
	@Deprecated
	public World getWorld(){
		return Gbl.getWorld();
	}
	
	
	/**
	 * @see org.matsim.api.core.v01.Scenario#getFacilities()
	 */
	public Facilities getFacilities() {
		return this.facilities;
	}

	/**
	 * @see org.matsim.api.core.v01.Scenario#getNetwork()
	 */
	public Network getNetwork() {
		return this.network;
	}

	/**
	 * @see org.matsim.api.core.v01.Scenario#getPopulation()
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
		return laneDefinitions;
	}

	
	public BasicSignalSystems getSignalSystems() {
		return signalSystems;
	}

	
	public BasicSignalSystemConfigurations getSignalSystemConfigurations() {
		return signalSystemConfigurations;
	}

	
	public RoadPricingScheme getRoadPricingScheme() {
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

}
