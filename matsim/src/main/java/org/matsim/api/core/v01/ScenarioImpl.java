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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.knowledges.KnowledgesImpl;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsImpl;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.config.SignalSystemConfigurationsImpl;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.signalsystems.systems.SignalSystemsImpl;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vehicles.BasicVehiclesImpl;
import org.matsim.world.World;


/**
 * @author dgrether
 * @author mrieser
 */
public class ScenarioImpl implements Scenario {

	private static final Logger log = Logger.getLogger(ScenarioImpl.class);

	private static final String NON_ENABLED_ATTRIBUTE_WARNING = "Trying to retrieve not enabled scenario feature, have you enabled the feature in ScenarioConfigGroup?";

	private final Map<Class<?>, Object> elements = new HashMap<Class<?>, Object>();

	//mandatory attributes
	private final Config config;
	private NetworkImpl network;
	private PopulationImpl population;
	private ActivityFacilitiesImpl facilities;

	private final ConcurrentHashMap<String, Id> idMap = new ConcurrentHashMap<String, Id>();

	//non-mandatory attributes
	private LaneDefinitions laneDefinitions;
	private SignalSystems signalSystems;
	private SignalSystemConfigurations signalSystemConfigurations;
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
		this.world.setFacilityLayer(this.facilities);

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
		this.laneDefinitions = new LaneDefinitionsImpl();
	}

	protected void createSignalSystemsContainers() {
		this.signalSystems = new SignalSystemsImpl();
		this.signalSystemConfigurations = new SignalSystemConfigurationsImpl();
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

	@Override
	public NetworkLayer getNetwork() {
		return (NetworkLayer) this.network;
	}

	@Override
	public PopulationImpl getPopulation() {
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


	public LaneDefinitions getLaneDefinitions() {
		if ((this.laneDefinitions == null) && this.config.scenario().isUseLanes()){
			this.createLaneDefinitionsContainer();
		}
		else if (!this.config.scenario().isUseLanes()){
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return laneDefinitions;
	}


	public SignalSystems getSignalSystems() {
		if ((this.signalSystems == null) && this.config.scenario().isUseSignalSystems()){
			this.createSignalSystemsContainers();
		}
		else if (!this.config.scenario().isUseSignalSystems()){
			log.warn(NON_ENABLED_ATTRIBUTE_WARNING);
		}
		return signalSystems;
	}


	public SignalSystemConfigurations getSignalSystemConfigurations() {
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
	public void setNetwork(NetworkImpl network2) {
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

	@Override
	public void addScenarioElement(final Object o) {
		for (Class<?> c : getAllTypes(o.getClass())) {
			this.elements.put(c, o);
		}
	}

	@Override
	public boolean removeScenarioElement(Object o) {
		boolean changed = false;

		for (Class<?> c : getAllTypes(o.getClass())) {
			if (this.elements.get(c) == o) {
				this.elements.remove(c);
				changed = true;
			}
		}

		return changed;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getScenarioElement(java.lang.Class<? extends T> klass) {
		return (T) this.elements.get(klass);
	}

	private Set<Class<?>> getAllTypes(final Class<?> klass) {
		Set<Class<?>> set = new HashSet<Class<?>>();
		Stack<Class<?>> stack = new Stack<Class<?>>();
		stack.add(klass);

		while (!stack.isEmpty()) {
			Class<?> c = stack.pop();
			set.add(c);
			for (Class<?> k : c.getInterfaces()) {
				stack.push(k);
			}
			if (c.getSuperclass() != null) {
				stack.push(c.getSuperclass());
			}
		}

		return set;
	}
}
