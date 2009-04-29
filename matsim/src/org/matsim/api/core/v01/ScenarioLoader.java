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
package org.matsim.api.core.v01;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.api.network.Network;
import org.matsim.core.basic.network.BasicLaneDefinitions;
import org.matsim.core.basic.signalsystems.BasicSignalSystems;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimLaneDefinitionsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsReader;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.world.MatsimWorldReader;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 * 
 */
public class ScenarioLoader {

	private static final Logger log = Logger.getLogger(ScenarioLoader.class);

	private Config config;

	private Scenario scenario;

	public ScenarioLoader(Config config) {
		this.config = config;
		this.scenario = new ScenarioImpl(this.config);
	}

	public ScenarioLoader(Scenario scenario) {
		this.scenario = scenario;
		this.config = this.scenario.getConfig();
	}

	public ScenarioLoader(String configFilename) {
		this.config = new Config();
		this.config.addCoreModules();
		MatsimConfigReader reader = new MatsimConfigReader(this.config);
		reader.readFile(configFilename);
		MatsimRandom.reset(config.global().getRandomSeed());
		this.scenario = new ScenarioImpl(this.config);
	}

	public Scenario getScenario() {
		return this.scenario;
	}

	public Scenario loadScenario() {
		this.loadWorld();
		this.loadNetwork();
		this.loadFacilities();
		this.loadPopulation();
		if (this.config.scenario().isUseLanes()) {
			this.loadLanes();
		}
		if (this.config.scenario().isUseSignalSystems()) {
			this.loadSignalSystems();
			this.loadSignalSystemConfigurations();
		}
		return this.scenario;
	}

	private void loadSignalSystemConfigurations() {
		if (this.scenario instanceof ScenarioImpl) {
			if ((((ScenarioImpl) this.scenario).getSignalSystemConfigurations() != null)
					&& (this.config.signalSystems().getSignalSystemConfigFile() != null)) {
				BasicSignalSystemConfigurations signalSystemConfigurations = ((ScenarioImpl) this.scenario)
						.getSignalSystemConfigurations();
				MatsimSignalSystemConfigurationsReader reader = new MatsimSignalSystemConfigurationsReader(
						signalSystemConfigurations);
				log.info("loading signalsystemsconfiguration from " + this.config.signalSystems().getSignalSystemConfigFile());
				reader.readFile(this.config.signalSystems().getSignalSystemConfigFile());
			}
			else {
				log.info("no signal system configurations file set in config or feature disabled, not able to load anything");
			}
		}
	}

	private void loadSignalSystems() {
		if (this.scenario instanceof ScenarioImpl) {
			if ((((ScenarioImpl) this.scenario).getSignalSystems() != null)
					&& (this.config.signalSystems().getSignalSystemFile() != null)) {
				BasicSignalSystems signalSystems = ((ScenarioImpl) this.scenario).getSignalSystems();
				MatsimSignalSystemsReader reader = new MatsimSignalSystemsReader(signalSystems);
				log.info("loading signalsystems from " + this.config.signalSystems().getSignalSystemFile());
				reader.readFile(this.config.signalSystems().getSignalSystemFile());
			}
			else {
				log.info("no signal system definition file set in config or feature disabled, not able to load anything");
			}
		}

	}

	private void loadLanes() {
		if (this.scenario instanceof ScenarioImpl) {
			BasicLaneDefinitions laneDefinitions;
			if ((((ScenarioImpl) this.scenario).getLaneDefinitions() != null)
					&& (this.config.network().getLaneDefinitionsFile() != null)) {
				laneDefinitions = ((ScenarioImpl) this.scenario).getLaneDefinitions();
				MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(laneDefinitions);
				reader.readFile(this.config.network().getLaneDefinitionsFile());
			}
			else {
				log.info("no lane definition file set in config or feature disabled, not able to load anything");
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void loadWorld() {
		String worldFileName = null;
		if (this.config.world() != null) {
			worldFileName = this.config.world().getInputFile();
		}
		if (worldFileName != null) {
			log.info("loading world from " + worldFileName);
			try {
				new MatsimWorldReader(Gbl.getWorld()).parse(worldFileName);
			} catch (SAXException e) {
				throw new RuntimeException(e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Loads the network into the scenario of this class
	 */
	public void loadNetwork() {
		String networkFileName = null;
		if ((this.config.network() != null) && (this.config.network().getInputFile() != null)) {
			networkFileName = this.config.network().getInputFile();
			log.info("loading network from " + networkFileName);
			Network network = this.scenario.getNetwork();
			if (this.config.network().isTimeVariantNetwork()) {
				log.info("use TimeVariantLinks in NetworkFactory.");
				network.getFactory().setLinkFactory(new TimeVariantLinkFactory());
			}
			try {
				if (network instanceof NetworkLayer) {
					new MatsimNetworkReader((NetworkLayer) network).parse(networkFileName);
				}
				else {
					throw new IllegalStateException(
							"Implementation of Network interface not supported, a specific parser is needed for this implementation of Network interface!");
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if ((config.network().getChangeEventsInputFile() != null) && config.network().isTimeVariantNetwork()) {
				log.info("loading network change events from " + config.network().getChangeEventsInputFile());
				NetworkChangeEventsParser parser = new NetworkChangeEventsParser((NetworkLayer) network);
				try {
					parser.parse(config.network().getChangeEventsInputFile());
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				((NetworkLayer) network).setNetworkChangeEvents(parser.getEvents());
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void loadFacilities() {
		if ((this.config.facilities() != null) && (this.config.facilities().getInputFile() != null)) {
			String facilitiesFileName = this.config.facilities().getInputFile();
			log.info("loading facilities from " + facilitiesFileName);
			try {
				new MatsimFacilitiesReader(this.scenario.getFacilities()).parse(facilitiesFileName);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if (((ScenarioImpl) this.scenario).getWorld() != null) {
				((ScenarioImpl) this.scenario).getWorld().complete();
			}
		}
		else {
			log.info("no facilities file set in config, not able to load them!");
		}
	}

	@SuppressWarnings("deprecation")
	public void loadPopulation() {
		// make sure that world, facilities and network are loaded as well
		if ((this.config.plans() != null) && (this.config.plans().getInputFile() != null)) {
			String populationFileName = this.config.plans().getInputFile();
			log.info("loading population from " + populationFileName);
			try {
				new MatsimPopulationReader(this.scenario).parse(populationFileName);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		else {
			log.info("no population file set in config, not able to load population");
		}
	}

}
