/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.workbench;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.xml.sax.SAXException;
import contrib.publicTransitMapping.tools.NetworkTools;
import contrib.publicTransitMapping.tools.ScheduleCleaner;
import contrib.publicTransitMapping.tools.ScheduleTools;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.matsim.contrib.accessibility.FacilityTypes.*;

/**
 * workbench class to do some scenario preparation
 *
 * @author polettif
 */
public class PrepareScenarios {

	protected static Logger log = Logger.getLogger(PrepareScenarios.class);

	private Config config;

	private Network network;
	private TransitSchedule schedule;
	private Vehicles vehicles;
	private Population population;

	public static void main(final String[] args) {

		String inputNetwork = args[0]+"/network.xml.gz";
		String inputSchedule = args[0]+"/schedule.xml.gz";
		String inputVehicles = args[0]+"/transitVehicles.xml.gz";
		String inputPopulation;
		String scenName;

		// Zurich 0.1%
		scenName = "zurich_micro";
		inputPopulation = "population/zh_1prct.xml.gz";

		final Config micro = createConfig(scenName);
		micro.qsim().setFlowCapFactor(1.0);

		PrepareScenarios prepMicro = new PrepareScenarios(micro, inputNetwork, inputSchedule, inputVehicles, inputPopulation);
		prepMicro.removeInvalidLines();
		prepMicro.cutSchedule();
		prepMicro.population();
		prepMicro.popSubset(10);
		prepMicro.vehicles(1.0);
		prepMicro.writeFiles(scenName);


		// Switzerland 10%
		scenName = "ch_ten";
		inputPopulation = "population/ch_10prct.xml.gz";
		final Config configCH = createConfig(scenName);
		configCH.qsim().setFlowCapFactor(0.1);

		PrepareScenarios prepareCH = new PrepareScenarios(configCH, inputNetwork, inputSchedule, inputVehicles, inputPopulation);
		prepareCH.removeInvalidLines();
//		prepareCH.cutSchedule();
		prepareCH.population();
		prepareCH.vehicles(0.1);
		prepareCH.writeFiles(scenName);

		try {
			TransitScheduleValidator.main(new String[]{scenName+"/"+configCH.transit().getTransitScheduleFile(), scenName+"/"+configCH.network().getInputFile()});
		} catch (IOException | SAXException | ParserConfigurationException e) {
			e.printStackTrace();
		}

		// Zurich 1%
		// no delay
		scenName = "zurich_one";
		inputPopulation = "population/zh_1prct.xml.gz";

		final Config configZH1 = createConfig(scenName);
		configZH1.qsim().setFlowCapFactor(1.0);

		PrepareScenarios prepareZH1 = new PrepareScenarios(configZH1, inputNetwork, inputSchedule, inputVehicles, inputPopulation);
		prepareZH1.removeInvalidLines();
		prepareZH1.cutSchedule();
		prepareZH1.population();
		prepareZH1.vehicles(1.0);
		prepareZH1.writeFiles(scenName);

		// Zurich 10%
		// delayed pt
		scenName = "zurich_ten";
		inputPopulation = "population/zh_10prct.xml.gz";

		final Config configZH10 = createConfig(scenName);
		configZH10.qsim().setFlowCapFactor(0.1);

		PrepareScenarios prepareZH10 = new PrepareScenarios(configZH10, inputNetwork, inputSchedule, inputVehicles, inputPopulation);
		prepareZH10.removeInvalidLines();
		prepareZH10.cutSchedule();
		prepareZH10.population();
		prepareZH10.vehicles(0.1);
		prepareZH10.writeFiles(scenName);


		// Zurich 10%
		// teleport pt
		scenName = "zurich_ten_teleport";
		inputPopulation = "population/zh_10prct.xml.gz";

		final Config configZH10Tel = createConfig(scenName);
		configZH10Tel.qsim().setFlowCapFactor(0.1);
		configZH10Tel.transit().setUseTransit(false);

		PrepareScenarios prepareZH10Tel = new PrepareScenarios(configZH10Tel, inputNetwork, inputSchedule, inputVehicles, inputPopulation);
		prepareZH10Tel.removeInvalidLines();
		prepareZH10Tel.cutSchedule();
		prepareZH10Tel.population();
		prepareZH10Tel.vehicles(0.1);
		prepareZH10Tel.writeFiles(scenName);
	}

	public PrepareScenarios(Config config, String inputNetwork, String inputSchedule, String inputVehicles, String inputPopulation) {
		this.config = config;
		this.network = NetworkTools.readNetwork(inputNetwork);
		this.schedule = ScheduleTools.readTransitSchedule(inputSchedule);
		this.vehicles = ScheduleTools.readVehicles(inputVehicles);

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader reader = new PopulationReader(sc);
		reader.readFile(inputPopulation);
		this.population = sc.getPopulation();
	}


	public PrepareScenarios(Config config, Network network, String inputSchedule, String inputVehicles, String inputPopulation) {
		this.config = config;
		this.network = network;
		this.schedule = ScheduleTools.readTransitSchedule(inputSchedule);
		this.vehicles = ScheduleTools.readVehicles(inputVehicles);

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader reader = new PopulationReader(sc);
		reader.readFile(inputPopulation);
		this.population = sc.getPopulation();
	}

	private void writeFiles(String folder) {
		new ConfigWriter(config).write(folder+"/config.xml");
		ScheduleTools.writeVehicles(vehicles, folder + "/" + config.transit().getVehiclesFile());
		ScheduleTools.writeTransitSchedule(schedule, folder + "/" + config.transit().getTransitScheduleFile());
		NetworkTools.writeNetwork(network, folder + "/" + config.network().getInputFile());
		new PopulationWriter(population, network).write(folder + "/" + config.plans().getInputFile());
	}

	private void vehicles(double pcePercentage) {
		for(VehicleType vt : vehicles.getVehicleTypes().values()) {
			vt.setPcuEquivalents(vt.getPcuEquivalents()*pcePercentage);
		}
	}

	/**
	 * modifiy population
	 */
	private void population() {
		Network activityLinkNetwork = NetworkTools.filterNetworkByLinkMode(network, Collections.singleton("car"));
		new NetworkCleaner().run(activityLinkNetwork);

		// only home and work activities
		log.info("adapting plans...");
		Counter personCounter = new Counter(" person # ");
		for(Person person : population.getPersons().values()) {
			personCounter.incCounter();
			List<? extends Plan> plans = person.getPlans();
			for(Plan plan : plans) {
				List<PlanElement> elements = plan.getPlanElements();
				for(PlanElement e : elements) {
					if(e instanceof Activity) {
						Activity activity = (Activity) e;
						switch (activity.getType()) {
							case "home" :
								break;
							case "work" :
								break;
							default :
								activity.setType(OTHER);
						}
						activity.setFacilityId(null);
						activity.setLinkId(NetworkTools.getNearestLink(activityLinkNetwork, activity.getCoord()).getId());
					}
				}
			}
		}
	}

	private void popSubset(int divisor) {
		Population newPop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		int i = 0;
		for(Person person : new HashSet<>(population.getPersons().values())) {
			if(i % divisor == 0) {
				newPop.addPerson(person);
			}
			i++;
		}
		newPop.setName(population.getName());
		population = newPop;
	}

	private void cutSchedule() {
		Coord effretikon = new Coord(2693780.0, 1253409.0);
		Coord zurichHB = new Coord(2682830.0, 1248125.0);
		double radius = 20000;

		ScheduleCleaner.cutSchedule(schedule, zurichHB, radius);
		Set<String> modesToKeep = new HashSet<>();
		modesToKeep.add("car");
		modesToKeep.add("rail");
		modesToKeep.add("light_rail");
		ScheduleCleaner.removeNotUsedTransitLinks(schedule, network, modesToKeep, true);
		ScheduleCleaner.cleanVehicles(schedule, vehicles);

	}

	private static Config createConfig(String folder) {
		int nThreads = 12;

		Config config = ConfigUtils.createConfig();

		config.controler().setLastIteration(200);
		config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("/cluster/scratch/polettif/scenario_output/"+folder);

		config.global().setNumberOfThreads(nThreads);
		config.global().setRandomSeed(6129);
		config.global().setCoordinateSystem(TransformationFactory.CH1903_LV03_Plus);

		config.network().setInputFile("network.xml.gz");

		config.parallelEventHandling().setNumberOfThreads(nThreads);

		PlanCalcScoreConfigGroup.ActivityParams workParams = new PlanCalcScoreConfigGroup.ActivityParams();
		workParams.setActivityType(WORK);
		workParams.setTypicalDuration(Time.parseTime("08:00:00"));
		workParams.setEarliestEndTime(Time.parseTime("06:00:00"));
		workParams.setLatestStartTime(Time.parseTime("20:00:00"));
		workParams.setMinimalDuration(Time.parseTime("04:00:00"));
		PlanCalcScoreConfigGroup.ActivityParams homeParams = new PlanCalcScoreConfigGroup.ActivityParams();
		homeParams.setActivityType(HOME);
		homeParams.setTypicalDuration(Time.parseTime("08:00:00"));
		workParams.setEarliestEndTime(Time.parseTime("04:00:00"));
		workParams.setLatestStartTime(Time.parseTime("24:00:00"));
		workParams.setMinimalDuration(Time.parseTime("04:00:00"));
		PlanCalcScoreConfigGroup.ActivityParams otherParams = new PlanCalcScoreConfigGroup.ActivityParams();
		otherParams.setActivityType(OTHER);
		homeParams.setTypicalDuration(Time.parseTime("01:00:00"));
		workParams.setEarliestEndTime(Time.parseTime("00:00:00"));
		workParams.setLatestStartTime(Time.parseTime("24:00:00"));
		workParams.setMinimalDuration(Time.parseTime("00:30:00"));
		config.planCalcScore().addActivityParams(homeParams);
		config.planCalcScore().addActivityParams(workParams);
		config.planCalcScore().addActivityParams(otherParams);

		config.plans().setInputFile("plans.xml.gz");

		config.qsim().setEndTime(Time.parseTime("30:00:00"));
		config.qsim().setNumberOfThreads(nThreads);

		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile("transitSchedule.xml.gz");
		config.transit().setVehiclesFile("transitVehicles.xml.gz");

		// strategy
		StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings();
		changeExpBeta.setStrategyName("ChangeExpBeta");
		changeExpBeta.setWeight(0.5);
		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.2);
		StrategyConfigGroup.StrategySettings changeTripMode = new StrategyConfigGroup.StrategySettings();
		changeTripMode.setStrategyName("ChangeTripMode");
		changeTripMode.setWeight(0.2);
		config.strategy().addStrategySettings(changeExpBeta);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().addStrategySettings(changeTripMode);

		return config;
	}

	private void removeInvalidLines() {
		ScheduleCleaner.removeInvalidTransitRoutes(TransitScheduleValidator.validateAll(schedule, network), schedule);
	}


}