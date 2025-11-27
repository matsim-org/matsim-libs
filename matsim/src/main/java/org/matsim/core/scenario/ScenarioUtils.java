
/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import java.util.Collections;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.config.Config;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.Lanes;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.vehicles.Vehicles;


/**
 * Provides ways to get a Scenario from the implementation in this package.
 *
 * @author michaz
 *
 */
public final class ScenarioUtils {

	private ScenarioUtils() {
		// make it private, so it cannot be instantiated
	}

	/**
	 *
	 * Creates an unpopulated scenario. The configuration passed into this method is
	 * a) used to determine which containers are required, depending on the options set in the scenario config group, and
	 * b) wrapped in the Scenario
	 *
	 * User code surrenders the config to the scenario. The config should not be externally changed afterwards.
	 *
	 * @param config A {@link Config} object, must not be <code>null</code>
	 *
	 * @see org.matsim.core.config.ConfigUtils#createConfig()
	 */
	public static Scenario createScenario(final Config config) {
		if (config == null) {
			throw new NullPointerException("config must not be null!");
		}
		return new MutableScenario(config);
	}
	public static MutableScenario createMutableScenario(final Config config) {
		if (config == null) {
			throw new NullPointerException("config must not be null!");
		}
		return new MutableScenario(config);
	}

	/**
	 *
	 * Initializes a scenario and populates it with data read from the input files which are named in the config.
	 *
	 */
	public static Scenario loadScenario(final Config config) {
		return loadScenario(config, Collections.emptyMap());
	}

	/**
	 *
	 * Initializes a scenario and populates it with data read from the input files which are named in the config.
	 * Uses provided {@link AttributeConverter}s when loading the scenario.
	 *
	 */
	public static Scenario loadScenario(final Config config, Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(config);
		scenarioLoader.setAttributeConverters(attributeConverters);
		return scenarioLoader.loadScenario();
	}

	/**
	 *
	 * Populates a scenario with data read from the input files which are named in the config which is wrapped
	 * in the scenario.
	 *
	 */
	public static void loadScenario(final Scenario scenario) {
		loadScenario(scenario, Collections.emptyMap());
	}

	/**
	 *
	 * Populates a scenario with data read from the input files which are named in the config which is wrapped
	 * in the scenario. Uses provided {@link AttributeConverter}s when loading the scenario.
	 *
	 */
	public static void loadScenario(final Scenario scenario, Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.setAttributeConverters(attributeConverters);
		scenarioLoader.loadScenario();
	}
	
	public final static class ScenarioBuilder {
		private MutableScenario scenario;
		public ScenarioBuilder( Config config ) {
			this.scenario = new MutableScenario( config ) ;
		}
		public ScenarioBuilder addScenarioElement(String name, Object o) {
			scenario.addScenarioElement(name, o); 
			return this ;
		}
		public ScenarioBuilder setHouseholds( Households households ) {
			scenario.setHouseholds(households);
			return this ;
		}
		public ScenarioBuilder setTransitSchedule( TransitSchedule schedule ) {
			scenario.setTransitSchedule(schedule);
			return this ;
		}
		public ScenarioBuilder setVehicles( Vehicles vehicles ) {
			scenario.setTransitVehicles(vehicles);
			return this;
		}
		public ScenarioBuilder setNetwork( Network network ) {
			scenario.setNetwork(network);
			return this ;
		}
		public ScenarioBuilder setPopulation( Population population ) {
			scenario.setPopulation(population);
			return this ;
		}
		public ScenarioBuilder setActivityFacilities( ActivityFacilities facilities ) {
			scenario.setActivityFacilities(facilities);
			return this ;
		}
		public ScenarioBuilder setLanes( Lanes lanes ) {
			scenario.setLanes(lanes);
			return this ;
		}
		// final creational method:
		public Scenario build() {
			this.scenario.setLocked(); // prevents that one can cast to ScenarioImpl and change the containers again. kai, nov'14
			return this.scenario ;
		}
	}

	/**
	 * Name of the attribute to add to top-level containers to specify the scale.
	 * When possible, the utility methods should be used instead of directly querying the attributes.
	 */
	public static final String INPUT_SCALE_ATT = "scale";

	public static <T extends MatsimToplevelContainer & Attributable> Double getScale(T container) {
		return (Double) container.getAttributes().getAttribute(INPUT_SCALE_ATT);
	}

	/**
	 * Adds scale metadata to the given container. The scale is meant for documentation and could be considered when
	 * consuming the data. Potential meaningful containers:
	 * - population (fraction of agents)
	 * - network (flow capacities)
	 * - vehicles (pce definition)
	 */
	public static <T extends MatsimToplevelContainer & Attributable> void putScale(T container, Double scale) {
		container.getAttributes().putAttribute(INPUT_SCALE_ATT, scale);
	}

}
