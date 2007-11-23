/* *********************************************************************** *
 * project: org.matsim.*
 * Config.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.marcel.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import playground.marcel.config.groups.ControlerConfigGroup;
import playground.marcel.config.groups.EventsConfigGroup;
import playground.marcel.config.groups.FacilitiesConfigGroup;
import playground.marcel.config.groups.GlobalConfigGroup;
import playground.marcel.config.groups.MatricesConfigGroup;
import playground.marcel.config.groups.NetworkConfigGroup;
import playground.marcel.config.groups.PlansConfigGroup;
import playground.marcel.config.groups.ScoringConfigGroup;
import playground.marcel.config.groups.SimulationConfigGroup;
import playground.marcel.config.groups.StrategiesConfigGroup;
import playground.marcel.config.groups.WorldConfigGroup;

public class Config {

	private final Map<String, ConfigGroupI> groups = new LinkedHashMap<String, ConfigGroupI>();

	private final GlobalConfigGroup global;
	private final WorldConfigGroup world;
	private final NetworkConfigGroup network;
	private final PlansConfigGroup plans;
	private final FacilitiesConfigGroup facilities;
	private final EventsConfigGroup events;
	private final ScoringConfigGroup scoring;
	private final MatricesConfigGroup matrices;
	private final SimulationConfigGroup simulation;
	private final StrategiesConfigGroup strategies;
	private final ControlerConfigGroup controler;
	
	public Config() {

		this.global = new GlobalConfigGroup();
		addGroup(GlobalConfigGroup.GROUP_NAME, this.global);
		
		this.world = new WorldConfigGroup();
		addGroup(WorldConfigGroup.GROUP_NAME, this.world);

		this.network = new NetworkConfigGroup();
		addGroup(NetworkConfigGroup.GROUP_NAME, this.network);
		
		this.plans = new PlansConfigGroup();
		addGroup(PlansConfigGroup.GROUP_NAME, this.plans);
		
		this.facilities = new FacilitiesConfigGroup();
		addGroup(FacilitiesConfigGroup.GROUP_NAME, this.facilities);
		
		this.events = new EventsConfigGroup();
		addGroup(EventsConfigGroup.GROUP_NAME, this.events);
		
		this.scoring = new ScoringConfigGroup();
		addGroup(ScoringConfigGroup.GROUP_NAME, this.scoring);
		
		this.matrices = new MatricesConfigGroup();
		addGroup(MatricesConfigGroup.GROUP_NAME, this.matrices);
		
		this.simulation = new SimulationConfigGroup();
		addGroup(SimulationConfigGroup.GROUP_NAME, this.simulation);
		
		this.strategies = new StrategiesConfigGroup();
		addGroup(StrategiesConfigGroup.GROUP_NAME, this.strategies);
		
		this.controler = new ControlerConfigGroup();
		addGroup(ControlerConfigGroup.GROUP_NAME, this.controler);

	}
	
	/* general access to the groups */
	
	public ConfigGroupI addGroup(String groupName, ConfigGroupI group) {
		return this.groups.put(groupName, group);
	}
	
	public ConfigGroupI getGroup(String groupName) {
		return this.groups.get(groupName);
	}

	public ConfigGroupI removeGroup(String groupName) {
		return this.groups.remove(groupName);
	}

	public Set<String> getGroupNames() {
		return this.groups.keySet();
	}
	
	/* direct access to standard group */
	
	public GlobalConfigGroup global() {
		return this.global;
	}

	public WorldConfigGroup world() {
		return this.world;
	}

	public NetworkConfigGroup network() {
		return this.network;
	}

	public PlansConfigGroup plans() {
		return this.plans;
	}
	
	public FacilitiesConfigGroup facilities() {
		return this.facilities;
	}

	public EventsConfigGroup events() {
		return this.events;
	}

	public ScoringConfigGroup scoring() {
		return this.scoring;
	}
	
	public MatricesConfigGroup matrices() {
		return this.matrices;
	}
	
	public SimulationConfigGroup simulation() {
		return this.simulation;
	}
	
	public StrategiesConfigGroup strategies() {
		return this.strategies;
	}
	
	public ControlerConfigGroup  controler() {
		return this.controler;
	}
}
