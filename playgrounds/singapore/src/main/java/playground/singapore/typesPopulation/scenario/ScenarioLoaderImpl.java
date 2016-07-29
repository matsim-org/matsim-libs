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
package playground.singapore.typesPopulation.scenario;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LaneDefinitionsReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.VehicleReaderV1;

import playground.singapore.typesPopulation.population.PopulationReader;

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
 * @see org.matsim.core.scenario.ScenarioImplPops
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
		ScenarioImplPops scenario = (ScenarioImplPops) ScenarioUtils.createScenario(config);
		return new ScenarioLoaderImpl(scenario);
	}

	private final Config config;

	private final ScenarioImplPops scenario;

	/**
	 * yy Does it make sense to leave this constructor public?  kai, mar'11
	 */
	public ScenarioLoaderImpl(Config config) {
		this.config = config;
		this.scenario = (ScenarioImplPops) ScenarioUtils.createScenario(this.config);
	}

	/**
	 * yy Does it make sense to leave this constructor public?  kai, mar'11
	 */
	public ScenarioLoaderImpl(Scenario scenario) {
		this.scenario = (ScenarioImplPops) scenario;
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
		if (this.config.households().getInputFile()!=null) {
			this.loadHouseholds();
		}
		if (this.config.transit().isUseTransit()) {
			this.loadTransit();
		}
		if (this.config.vehicles().getVehiclesFile() != null ) {
			this.loadVehicles();
		}
		if (this.config.network().getLaneDefinitionsFile()!=null ) {
			this.loadLanes();
		}
		if (ConfigUtils.addOrGetModule(this.config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()){
			this.loadSignalSystems();
		}
		return this.scenario;
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
			Network network = (Network) this.scenario.getNetwork();
			if (this.config.network().isTimeVariantNetwork()) {
				log.info("use TimeVariantLinks in NetworkFactory.");
				network.getFactory().setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
			}
			new MatsimNetworkReader(this.scenario.getNetwork()).readFile(networkFileName);
			if ((config.network().getChangeEventsInputFile() != null) && config.network().isTimeVariantNetwork()) {
				log.info("loading network change events from " + config.network().getChangeEventsInputFile());
				List<NetworkChangeEvent> events = new ArrayList<>() ;
				NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network, events );
				parser.readFile(config.network().getChangeEventsInputFile());
				NetworkUtils.setNetworkChangeEvents(network,events );
			}
		}
	}

	/**
	 * @deprecated  Please use the static calls in ScenarioUtils to load a scenario.
	 * 				If you want only Facilities, use the MatsimFacilitiesReader directly.
	 * 
	 */
	@Deprecated
	private void loadActivityFacilities() {
		if ((this.config.facilities() != null) && (this.config.facilities().getInputFile() != null)) {
			String facilitiesFileName = this.config.facilities().getInputFile();
			log.info("loading facilities from " + facilitiesFileName);
			try {
				new MatsimFacilitiesReader(this.scenario).readFile(facilitiesFileName);
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
		if ((this.config.plans() != null) && (this.config.plans().getInputFile() != null)) {
			String populationFileName = this.config.plans().getInputFile();
			log.info("loading population from " + populationFileName);
			new PopulationReader(this.scenario).readFile(populationFileName);
			
			if (this.scenario.getPopulation() instanceof Population) {
				PopulationUtils.printPlansCount(((Population)this.scenario.getPopulation())) ;
			}
		}
		else {
			log.info("no population file set in config, not able to load population");
		}
		if ((this.config.plans() != null) && (this.config.plans().getInputPersonAttributeFile() != null)) {
			String personAttributesFileName = this.config.plans().getInputPersonAttributeFile();
			log.info("loading person attributes from " + personAttributesFileName);
			new ObjectAttributesXmlReader(this.scenario.getPopulation().getPersonAttributes()).readFile(personAttributesFileName);
		}
		else {
			log.info("no person-attributes file set in config, not loading any person attributes");
		}
	}

	private void loadHouseholds() {
		if ((this.scenario.getHouseholds() != null) && (this.config.households() != null) && (this.config.households().getInputFile() != null) ) {
			String hhFileName = this.config.households().getInputFile();
			log.info("loading households from " + hhFileName);
			new HouseholdsReaderV10(this.scenario.getHouseholds()).readFile(hhFileName);
			log.info("households loaded.");
		}
		else {
			log.info("no households file set in config or feature disabled, not able to load anything");
		}
	}

	private void loadTransit() throws UncheckedIOException {
		new TransitScheduleReader(this.scenario).readFile(this.config.transit().getTransitScheduleFile());
		if ((this.config.transit() != null) && (this.config.transit().getTransitLinesAttributesFile() != null)) {
			String transitLinesAttributesFileName = this.config.transit().getTransitLinesAttributesFile();
			log.info("loading transit lines attributes from " + transitLinesAttributesFileName);
			new ObjectAttributesXmlReader(this.scenario.getTransitSchedule().getTransitLinesAttributes()).readFile(transitLinesAttributesFileName);
		}
		if ((this.config.transit() != null) && (this.config.transit().getTransitStopsAttributesFile() != null)) {
			String transitStopsAttributesFileName = this.config.transit().getTransitStopsAttributesFile();
			log.info("loading transit stop facilities attributes from " + transitStopsAttributesFileName);
			new ObjectAttributesXmlReader(this.scenario.getTransitSchedule().getTransitStopsAttributes()).readFile(transitStopsAttributesFileName);
		}
	}

	private void loadVehicles() throws UncheckedIOException {
		Logger.getLogger(this.getClass()).fatal("cannot say if the following should be vehicles or transit vehicles; aborting ... .  kai, feb'15");
		System.exit(-1); 

		new VehicleReaderV1(this.scenario.getTransitVehicles()).readFile(this.config.transit().getVehiclesFile());
	}

	private void loadLanes() {
		Lanes laneDefinitions = scenario.getLanes();
		String filename = this.config.network().getLaneDefinitionsFile();
		if (filename != null){
			MatsimFileTypeGuesser fileTypeGuesser = new MatsimFileTypeGuesser(filename);
			if (!LaneDefinitionsReader.SCHEMALOCATIONV20.equalsIgnoreCase(fileTypeGuesser
					.getSystemId())) {
				log.error("Lanes: Wrong file format. With the 0.5 version of matsim the scenario only accepts lane definitions in the "
						+ "file format version 2.0, i.e. "
						+ LaneDefinitionsReader.SCHEMALOCATIONV20
						+ ". An automatic conversion of the 1.1 file format is no longer provided, please call the "
						+ "LaneDefinitonsV11ToV20Converter manually in the preprocessing phase.");
				throw new UncheckedIOException("Wrong lane file format: " + fileTypeGuesser.getSystemId());
			}
		}
		if ((laneDefinitions != null) && (filename != null)) {
			LaneDefinitionsReader reader = new LaneDefinitionsReader(scenario);
			reader.readFile(filename);
		}
		else {
			log.info("no lane definition file set in config or feature disabled, not able to load anything");
		}
	}

	private void loadSignalSystems() {
		this.scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(ConfigUtils.addOrGetModule(this.config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class)).loadSignalsData());
	}

}
