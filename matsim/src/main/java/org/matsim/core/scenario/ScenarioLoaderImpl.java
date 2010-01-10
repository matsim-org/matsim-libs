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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsReader;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.basic.BasicSignalSystems;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.world.MatsimWorldReader;
import org.xml.sax.SAXException;

/**
 * Loads elements of Scenario from file. Non standardized elements
 * can also be loaded however they require a specific instance of
 * Scenario. 
 * {@link #loadScenario()} reads the complete scenario from files while the
 * other load...() methods only load specific parts
 * of the scenario assuming that required parts are already 
 * loaded or created by the user.
 * 
 * @see org.matsim.api.core.v01.ScenarioImpl
 * 
 * @author dgrether
 */
public class ScenarioLoaderImpl implements ScenarioLoader {

	private static final Logger log = Logger.getLogger(ScenarioLoaderImpl.class);

	private Config config;

	private Scenario scenario;

	public ScenarioLoaderImpl(Config config) {
		this.config = config;
		this.scenario = new ScenarioImpl(this.config);
	}

	public ScenarioLoaderImpl(Scenario scenario) {
		this.scenario = scenario;
		this.config = this.scenario.getConfig();
	}

	public ScenarioLoaderImpl(String configFilename) {
		this.scenario = new ScenarioImpl();
		this.config = this.scenario.getConfig();
		MatsimConfigReader reader = new MatsimConfigReader(this.config);
		reader.readFile(configFilename);
		Gbl.setConfig(this.config);
		MatsimRandom.reset(config.global().getRandomSeed());
	}
	
	@Override
	public ScenarioImpl getScenario() {
		return (ScenarioImpl)this.scenario;
	}

	/**
	 * Loads all mandatory Scenario elements and
	 * if activated in config's scenario module/group 
	 * optional elements.
	 * @return the Scenario
	 */
	@Override
	public Scenario loadScenario() {
		this.loadWorld();
		this.loadNetwork();
		this.loadActivityFacilities();
		this.loadPopulation();
		this.loadHouseholds();
		
		if (this.config.scenario().isUseLanes()) {
			this.loadLanes();
		}
		if (this.config.scenario().isUseSignalSystems()) {
			this.loadSignalSystems();
			this.loadSignalSystemConfigurations();
		}
		return getScenario();
	}

	/**
	 * Loads the network into the scenario of this class
	 */
	public void loadNetwork() {
		String networkFileName = null;
		if ((this.config.network() != null) && (this.config.network().getInputFile() != null)) {
			networkFileName = this.config.network().getInputFile();
			log.info("loading network from " + networkFileName);
			NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
			if (this.config.network().isTimeVariantNetwork()) {
				log.info("use TimeVariantLinks in NetworkFactory.");
				network.getFactory().setLinkFactory(new TimeVariantLinkFactory());
			}
				if (network instanceof NetworkLayer) {
					try {
						new MatsimNetworkReader(network).parse(networkFileName);
					} catch (SAXException e) {
						throw new RuntimeException(e);
					} catch (ParserConfigurationException e) {
						throw new RuntimeException(e);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else {
					throw new IllegalStateException(
							"Implementation of Network interface not supported, a specific parser is needed for this implementation of Network interface!");
				}
			if ((config.network().getChangeEventsInputFile() != null) && config.network().isTimeVariantNetwork()) {
				log.info("loading network change events from " + config.network().getChangeEventsInputFile());
				NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network);
				try {
					parser.parse(config.network().getChangeEventsInputFile());
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				network.setNetworkChangeEvents(parser.getEvents());
			}
		}
	}

	
	private void loadHouseholds() {
			if ((this.getScenario().getHouseholds() != null) && (this.config.households() != null) && (this.config.households().getInputFile() != null) ) {
				String hhFileName = this.config.households().getInputFile();
				log.info("loading households from " + hhFileName);
				try {
					new HouseholdsReaderV10(this.getScenario().getHouseholds()).parse(hhFileName);
				} catch (SAXException e) {
					throw new RuntimeException(e);
				} catch (ParserConfigurationException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				log.info("households loaded.");
			}
			else {
				log.info("no households file set in config or feature disabled, not able to load anything");
			}
	}

	private void loadSignalSystemConfigurations() {
			if ((this.getScenario().getSignalSystemConfigurations() != null)
					&& (this.config.signalSystems().getSignalSystemConfigFile() != null)) {
				BasicSignalSystemConfigurations signalSystemConfigurations = this.getScenario()
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

	private void loadSignalSystems() {
			if (( this.getScenario().getSignalSystems() != null)
					&& (this.config.signalSystems().getSignalSystemFile() != null)) {
				BasicSignalSystems signalSystems = this.getScenario().getSignalSystems();
				MatsimSignalSystemsReader reader = new MatsimSignalSystemsReader(signalSystems);
				log.info("loading signalsystems from " + this.config.signalSystems().getSignalSystemFile());
				reader.readFile(this.config.signalSystems().getSignalSystemFile());
			}
			else {
				log.info("no signal system definition file set in config or feature disabled, not able to load anything");
			}
	}

	private void loadLanes() {
			BasicLaneDefinitions laneDefinitions;
			if ((this.getScenario().getLaneDefinitions() != null)
					&& (this.config.network().getLaneDefinitionsFile() != null)) {
				laneDefinitions = this.getScenario().getLaneDefinitions();
				MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(laneDefinitions);
				reader.readFile(this.config.network().getLaneDefinitionsFile());
			}
			else {
				log.info("no lane definition file set in config or feature disabled, not able to load anything");
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
				new MatsimWorldReader(getScenario().getWorld()).parse(worldFileName);
			} catch (SAXException e) {
				throw new RuntimeException(e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}



	@SuppressWarnings("deprecation")
	public void loadActivityFacilities() {
		if ((this.config.facilities() != null) && (this.config.facilities().getInputFile() != null)) {
			String facilitiesFileName = this.config.facilities().getInputFile();
			log.info("loading facilities from " + facilitiesFileName);
			try {
				new MatsimFacilitiesReader(this.getScenario().getActivityFacilities()).parse(facilitiesFileName);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if (this.getScenario().getWorld() != null) {
				this.getScenario().getWorld().complete();
			}
		}
		else {
			log.info("no facilities file set in config, not able to load them!");
		}
	}

	public void loadPopulation() {
		// make sure that world, facilities and network are loaded as well
		if ((this.config.plans() != null) && (this.config.plans().getInputFile() != null)) {
			String populationFileName = this.config.plans().getInputFile();
			log.info("loading population from " + populationFileName);
			try {
				new MatsimPopulationReader(this.getScenario()).parse(populationFileName);
			} catch (SAXException e) {
				throw new RuntimeException(e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			log.info("no population file set in config, not able to load population");
		}
	}

}
