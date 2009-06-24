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
package org.matsim.core.api;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.network.BasicLaneDefinitions;
import org.matsim.core.basic.signalsystems.BasicSignalSystems;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.core.basic.v01.BasicScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimLaneDefinitionsReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsReader;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
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
 * @see org.matsim.api.core.v01.Scenario
 * 
 * @author dgrether
 */
public class ScenarioLoader extends BasicScenarioLoader {

	private static final Logger log = Logger.getLogger(ScenarioLoader.class);

	public ScenarioLoader(Config config) {
		super(config);
		super.setScenario(new ScenarioImpl(this.config));
	}

	public ScenarioLoader(Scenario scenario) {
		super(scenario);
	}

	public ScenarioLoader(String configFilename) {
		super(configFilename);
		Gbl.setConfig(this.config);
		MatsimRandom.reset(config.global().getRandomSeed());
		super.setScenario(new ScenarioImpl(this.config));
	}

	
	@Override
	public Scenario getScenario() {
		return (Scenario)super.getScenario();
	}

	/**
	 * Loads all mandatory Scenario elements and
	 * if activated in config's scenario module/group 
	 * optional elements.
	 * @return the Scenario
	 */
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

	private void loadHouseholds() {
		if ((this.config.households() != null) && (this.config.households().getInputFile() != null)) {
			String hhFileName = this.config.households().getInputFile();
			log.info("loading households from " + hhFileName);
			try {
				new HouseholdsReaderV10((ScenarioImpl) this.getScenario()).parse(hhFileName);
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
			log.info("no households file set in config, not able to load households");
		}		
	}

	private void loadSignalSystemConfigurations() {
		if (this.getScenario() instanceof ScenarioImpl) {
			if ((((ScenarioImpl) this.getScenario()).getSignalSystemConfigurations() != null)
					&& (this.config.signalSystems().getSignalSystemConfigFile() != null)) {
				BasicSignalSystemConfigurations signalSystemConfigurations = ((ScenarioImpl) this.getScenario())
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
		if (this.getScenario() instanceof ScenarioImpl) {
			if ((((ScenarioImpl) this.getScenario()).getSignalSystems() != null)
					&& (this.config.signalSystems().getSignalSystemFile() != null)) {
				BasicSignalSystems signalSystems = ((ScenarioImpl) this.getScenario()).getSignalSystems();
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
		if (this.getScenario() instanceof ScenarioImpl) {
			BasicLaneDefinitions laneDefinitions;
			if ((((ScenarioImpl) this.getScenario()).getLaneDefinitions() != null)
					&& (this.config.network().getLaneDefinitionsFile() != null)) {
				laneDefinitions = ((ScenarioImpl) this.getScenario()).getLaneDefinitions();
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
			if (((ScenarioImpl) this.getScenario()).getWorld() != null) {
				((ScenarioImpl) this.getScenario()).getWorld().complete();
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
