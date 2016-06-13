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

package playground.polettif.publicTransitMapping.workbench;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Transit;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.xml.sax.SAXException;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

import static org.matsim.contrib.accessibility.FacilityTypes.HOME;
import static org.matsim.contrib.accessibility.FacilityTypes.WORK;

/**
 * workbench class to do some scenario preparation
 *
 * @author polettif
 */
public class Prepare {

	protected static Logger log = Logger.getLogger(Prepare.class);

	private Config config;

	private Network network;
	private TransitSchedule schedule;
	private Vehicles vehicles;
	private Population population;

	public static void main(final String[] args) {

		String inputNetwork = "../output/2016-06-10/ch_ll_network.xml.gz";
		String inputSchedule = "../output/2016-06-10/ch_ll_schedule.xml.gz";
		String inputVehicles = "../data/vehicles/ch_hafas_vehicles.xml.gz";
		String inputPopulation;
		String scenName;

		// Zurich 1%
		// no delay
		scenName = "zurich_one";
		inputPopulation = "population/zh_1prct.xml.gz";

		final Config configZH1 = createConfig(scenName);
		configZH1.qsim().setFlowCapFactor(1.0);

		Prepare prepareZH1 = new Prepare(configZH1, inputNetwork, inputSchedule, inputVehicles, inputPopulation);
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

		Prepare prepareZH10 = new Prepare(configZH10, inputNetwork, inputSchedule, inputVehicles, inputPopulation);
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

		Prepare prepareZH10Tel = new Prepare(configZH10Tel, inputNetwork, inputSchedule, inputVehicles, inputPopulation);
		prepareZH10Tel.removeInvalidLines();
		prepareZH10Tel.cutSchedule();
		prepareZH10Tel.population();
		prepareZH10Tel.vehicles(0.1);
		prepareZH10Tel.writeFiles(scenName);


		// Switzerland 10%
		scenName = "ch_ten";
		inputPopulation = "population/ch_10prct.xml.gz";
		final Config configCH = createConfig(scenName);
		configCH.qsim().setFlowCapFactor(0.1);

		Prepare prepareCH = new Prepare(configCH, inputNetwork, inputSchedule, inputVehicles, inputPopulation);
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
	}

	public Prepare(Config config, String inputNetwork, String inputSchedule, String inputVehicles, String inputPopulation) {
		this.config = config;
		this.network = NetworkTools.readNetwork(inputNetwork);
		this.schedule = ScheduleTools.readTransitSchedule(inputSchedule);
		this.vehicles = ScheduleTools.readVehicles(inputVehicles);

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader reader = new PopulationReaderMatsimV5(sc);
		reader.readFile(inputPopulation);
		this.population = sc.getPopulation();
	}

	private void removeInvalidLines() {
		Set<String> set = new HashSet<>();

		// copy/paste
		set.add("AB-_line21");
		set.add("AB-_line21");
		set.add("RVB_line2");
		set.add("RVB_line2");
		set.add("RVB_line2");
		set.add("RVB_line2");
		set.add("RVB_line4");
		set.add("RVB_line4");
		set.add("RVB_line4");
		set.add("RVB_line4");
		set.add("SBG_line7312");
		set.add("SBG_line7312");
		set.add("VBZ_line303");
		set.add("VBZ_line303");
		set.add("VBZ_line303");
		set.add("VBZ_line303");
		set.add("VBZ_line303");
		set.add("VBZ_line303");
		set.add("FAR_line2");
		set.add("PAG_line581");


		for(String e : set) {
			TransitLine tl = schedule.getTransitLines().get(Id.create(e, TransitLine.class));
			if(tl != null) schedule.removeTransitLine(tl);
		}

		ScheduleCleaner.removeRoute(schedule, Id.create("VBZ_line31", TransitLine.class), Id.create("05060_155", TransitRoute.class));
		ScheduleCleaner.removeRoute(schedule, Id.create("AAG_line401", TransitLine.class), Id.create("00001_001", TransitRoute.class));
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
		Network carNetwork = NetworkTools.filterNetworkByLinkMode(network, Collections.singleton("car"));

		// only home and work activities
		log.info("adapting plans...");
		Counter personCounter = new Counter(" person # ");
		for(Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			personCounter.incCounter();
			List<PlanElement> elements = plan.getPlanElements();
			for(PlanElement e : elements) {
				if(e instanceof ActivityImpl) {
					Activity activity = (Activity) e;
					switch (activity.getType()) {
						case "home" :
							break;
						case "work" :
							break;
						default :
							activity.setType("work");
					}
					activity.setFacilityId(null);
					activity.setLinkId(NetworkTools.getNearestLink(carNetwork, activity.getCoord()).getId());
				}
			}
		}
	}

	private void cutSchedule() {
		Coord effretikon = new Coord(2693780.0, 1253409.0);
		double radius = 20000;

		ScheduleCleaner.cutSchedule(schedule, effretikon, radius);
		Set<String> modesToKeep = new HashSet<>();
		modesToKeep.add("car");
		modesToKeep.add("rail");
		modesToKeep.add("light_rail");
		ScheduleCleaner.removeNotUsedTransitLinks(schedule, network, modesToKeep, true);
		ScheduleCleaner.cleanVehicles(schedule, vehicles);

	}

	private static Config createConfig(String folder) {
		Config config = ConfigUtils.createConfig();

		config.controler().setLastIteration(400);
		config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("../output/"+folder);

		config.global().setNumberOfThreads(8);
		config.global().setRandomSeed(6129);
		config.global().setCoordinateSystem(TransformationFactory.CH1903_LV03_Plus);

		config.network().setInputFile("network.xml.gz");

		config.parallelEventHandling().setNumberOfThreads(8);

		PlanCalcScoreConfigGroup.ActivityParams workParams = new PlanCalcScoreConfigGroup.ActivityParams();
		workParams.setActivityType(WORK);
		workParams.setTypicalDuration(Time.parseTime("08:00:00"));

		PlanCalcScoreConfigGroup.ActivityParams homeParams = new PlanCalcScoreConfigGroup.ActivityParams();
		homeParams.setActivityType(HOME);
		homeParams.setTypicalDuration(Time.parseTime("8:00:00"));

		config.planCalcScore().addActivityParams(homeParams);
		config.planCalcScore().addActivityParams(workParams);

		config.plans().setInputFile("plans.xml.gz");

		config.qsim().setEndTime(Time.parseTime("30:00:00"));
		config.qsim().setNumberOfThreads(8);

		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile("transitSchedule.xml.gz");
		config.transit().setVehiclesFile("transitVehicles.xml.gz");

		return config;
	}


}