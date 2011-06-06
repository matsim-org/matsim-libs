/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioLoader
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

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.vehicles.VehicleReaderV1;

/**
 * Loads elements of Scenario from file. Non standardized elements
 * can also be loaded however they require a specific instance of
 * Scenario.
 * {@link #loadScenario()} reads the complete scenario from files while the
 * other load...() methods only load specific parts
 * of the scenario assuming that required parts are already
 * loaded or created by the user.
 * <p/>
 * Design thoughts:<ul>
 * <li> Given what we have now, does it make sense to leave this class public?  yy kai, mar'11
 * </ul> 
 *
 * @see org.matsim.core.scenario.ScenarioImpl
 *
 * @author dgrether
 */
public class ScenarioLoaderImpl {

	private static final Logger log = Logger.getLogger(ScenarioLoaderImpl.class);

	
	static Scenario loadScenario(Config config) {
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(config);
		Scenario scenario = scenarioLoader.loadScenario();
		return scenario;
	}

	static void loadScenario(Scenario scenario) {
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();
	}

	/**
	 * @deprecated  This used to be a constructor with a global side effect, which is absolutely evil.
	 *				Please just load the Scenario with ScenarioUtils.loadScenario instead.
	 */
	@Deprecated
	public static ScenarioLoaderImpl createScenarioLoaderImplAndResetRandomSeed(String configFilename) {
		Config config = ConfigUtils.loadConfig(configFilename);
		MatsimRandom.reset(config.global().getRandomSeed());
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		return new ScenarioLoaderImpl(scenario);
	}

	private final Config config;

	private final ScenarioImpl scenario;

	/**
	 * yy Does it make sense to leave this constructor public?  kai, mar'11
	 */
	public ScenarioLoaderImpl(Config config) {
		this.config = config;
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(this.config);
	}

	/**
	 * yy Does it make sense to leave this constructor public?  kai, mar'11
	 */
	public ScenarioLoaderImpl(Scenario scenario) {
		this.scenario = (ScenarioImpl) scenario;
		this.config = this.scenario.getConfig();
	}

	
	/**
	 * @deprecated  Please use the static calls in ScenarioUtils instead.
	 * 
	 */
	@Deprecated
	public Scenario getScenario() {
		return this.scenario;
	}

	/**
	 * Loads all mandatory Scenario elements and
	 * if activated in config's scenario module/group
	 * optional elements.
	 * @deprecated  Please use the static calls in ScenarioUtils instead.
	 * @return the Scenario
	 */
	@Deprecated
	public Scenario loadScenario() {
		String currentDir = new File("tmp").getAbsolutePath();
		currentDir = currentDir.substring(0, currentDir.length() - 3);
		log.info("loading scenario from base directory: " + currentDir);
		this.loadNetwork();
		this.loadActivityFacilities();
		this.loadPopulation();
		if (this.config.scenario().isUseHouseholds()) {
			this.loadHouseholds();
		}
		if (this.config.scenario().isUseTransit()) {
			this.loadTransit();
		}
		if (this.config.scenario().isUseVehicles()) {
			this.loadVehicles();
		}
		if (this.config.scenario().isUseLanes()) {
			this.loadLanes();
		}
		if (this.config.scenario().isUseSignalSystems()){
			this.loadSignalSystems();
		}
		return getScenario();
	}

	/**
	 * Loads the network into the scenario of this class
	 * 
	 * @deprecated  Please use the static calls in ScenarioUtils to load a scenario.
	 * 				If you want only a network, use the MatsimNetworkReader directly.
	 * 
	 */
	@Deprecated
	public void loadNetwork() {
		String networkFileName = null;
		if ((this.config.network() != null) && (this.config.network().getInputFile() != null)) {
			networkFileName = this.config.network().getInputFile();
			log.info("loading network from " + networkFileName);
			NetworkImpl network = this.scenario.getNetwork();
			if (this.config.network().isTimeVariantNetwork()) {
				log.info("use TimeVariantLinks in NetworkFactory.");
				network.getFactory().setLinkFactory(new TimeVariantLinkFactory());
			}
			if (this.config.scenario().isUseTransit()) {
				network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
			}
			new MatsimNetworkReader(this.scenario).parse(networkFileName);
			if ((config.network().getChangeEventsInputFile() != null) && config.network().isTimeVariantNetwork()) {
				log.info("loading network change events from " + config.network().getChangeEventsInputFile());
				NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network);
				parser.parse(config.network().getChangeEventsInputFile());
				network.setNetworkChangeEvents(parser.getEvents());
			}
		}
	}

	/**
	 * @deprecated  Please use the static calls in ScenarioUtils to load a scenario.
	 * 				If you want only Facilities, use the MatsimFacilitiesReader directly.
	 * 
	 */
	@Deprecated
	public void loadActivityFacilities() {
		if ((this.config.facilities() != null) && (this.config.facilities().getInputFile() != null)) {
			String facilitiesFileName = this.config.facilities().getInputFile();
			log.info("loading facilities from " + facilitiesFileName);
			try {
				new MatsimFacilitiesReader(this.scenario).parse(facilitiesFileName);
				
				this.scenario.getActivityFacilities().printFacilitiesCount();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		else {
			log.info("no facilities file set in config, therefore not loading any facilities.  This is not a problem except if you are using facilities");
		}
	}

	/**
	 * @deprecated  Please use the static calls in ScenarioUtils to load a scenario.
	 * 				If you want only a Population, use the MatsimPopulationReader directly.
	 * 
	 */
	@Deprecated
	public void loadPopulation() {
		// make sure that world, facilities and network are loaded as well
		if ((this.config.plans() != null) && (this.config.plans().getInputFile() != null)) {
			String populationFileName = this.config.plans().getInputFile();
			log.info("loading population from " + populationFileName);
			new MatsimPopulationReader(this.getScenario()).parse(populationFileName);
			
			if (this.scenario.getPopulation() instanceof PopulationImpl) {
				((PopulationImpl)this.scenario.getPopulation()).printPlansCount();
			}
		}
		else {
			log.info("no population file set in config, not able to load population");
		}
	}

	private void loadHouseholds() {
		if ((this.scenario.getHouseholds() != null) && (this.config.households() != null) && (this.config.households().getInputFile() != null) ) {
			String hhFileName = this.config.households().getInputFile();
			log.info("loading households from " + hhFileName);
			new HouseholdsReaderV10(this.scenario.getHouseholds()).parse(hhFileName);
			log.info("households loaded.");
		}
		else {
			log.info("no households file set in config or feature disabled, not able to load anything");
		}
	}

	private void loadTransit() throws UncheckedIOException {
		new TransitScheduleReader(this.scenario).readFile(this.config.transit().getTransitScheduleFile());
	}

	private void loadVehicles() throws UncheckedIOException {
		new VehicleReaderV1(this.scenario.getVehicles()).readFile(this.config.transit().getVehiclesFile());
	}

	private void loadLanes() {
		LaneDefinitions laneDefinitions;
		if ((this.scenario.getLaneDefinitions() != null)
				&& (this.config.network().getLaneDefinitionsFile() != null)) {
			laneDefinitions = this.scenario.getLaneDefinitions();
			MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(laneDefinitions);
			reader.readFile(this.config.network().getLaneDefinitionsFile());
			this.getScenario().addScenarioElement(laneDefinitions);
			if (!MatsimLaneDefinitionsReader.SCHEMALOCATIONV20.equals(reader.getLastReadFileFormat())){
				log.warn("No laneDefinitions_v2.0 file specified in scenario. Trying to convert the v1.1 format to " +
				"the v2.0 format. For details see LaneDefinitionsV11ToV20Conversion.java.");
				LaneDefinitionsV11ToV20Conversion conversion = new LaneDefinitionsV11ToV20Conversion();
				laneDefinitions = conversion.convertTo20(laneDefinitions, scenario.getNetwork());
				this.scenario.setLaneDefinitions(laneDefinitions);
			}
		}
		else {
			log.info("no lane definition file set in config or feature disabled, not able to load anything");
		}
	}

	private void loadSignalSystems() {
		this.scenario.addScenarioElement(new SignalsScenarioLoader(this.config.signalSystems()).loadSignalsData());
	}

}
