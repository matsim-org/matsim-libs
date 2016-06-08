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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import playground.polettif.boescpa.lib.tools.coordUtils.CoordFilter;
import playground.polettif.boescpa.lib.tools.spatialCutting.ScheduleCutter;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

		String inputNetwork = "../../output/2016-06-08/ch_busOnly_network.xml.gz";
		String inputSchedule = "../../output/2016-06-08/ch_busOnly_schedule.xml.gz";
		String inputVehicles = "../../data/vehicles/ch_hafas_vehicles.xml.gz";
		String inputPopulation = "population_Orig.xml.gz";

		Prepare prepare = new Prepare("config.xml", inputNetwork, inputSchedule, inputVehicles, inputPopulation);
		prepare.networkAndSchedule();
		prepare.removeInvalidLines();
		prepare.population();
		prepare.vehicles(0.005);
		prepare.writeFiles();
	}

	private void removeInvalidLines() {
		List<String> list = new ArrayList<>();

		// copy/paste
//		list.add("asd");

		for(String e : list) {
			TransitLine tl = schedule.getTransitLines().get(Id.create(e, TransitLine.class));
			schedule.removeTransitLine(tl);
		}
	}

	private void removeInvalidRoutes() {
		List<String[]> list = new ArrayList<>();

		// copy/paste
//		list.add(new String[]{	"asd", "fad"	});

		for(String[] e : list) {
			ScheduleCleaner.removeRoute(schedule, Id.create(e[0], TransitLine.class), Id.create(e[2], TransitRoute.class));
		}
	}

	public Prepare(String scenarioConfig, String inputNetwork, String inputSchedule, String inputVehicles, String inputPopulation) {
		config = ConfigUtils.loadConfig(scenarioConfig);

		network = NetworkTools.readNetwork(inputNetwork);
		schedule = ScheduleTools.readTransitSchedule(inputSchedule);
		vehicles = ScheduleTools.readVehicles(inputVehicles);

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader reader = new PopulationReaderMatsimV5(sc);
		reader.readFile(inputPopulation);
		population = sc.getPopulation();
	}

	private void writeFiles() {
		ScheduleTools.writeVehicles(vehicles, config.transit().getVehiclesFile());
		ScheduleTools.writeTransitSchedule(schedule, config.transit().getTransitScheduleFile());
		NetworkTools.writeNetwork(network, config.network().getInputFile());
		new PopulationWriter(population, network).write(config.plans().getInputFile());
	}

	private void vehicles(double percentage) {
		for(VehicleType vt : vehicles.getVehicleTypes().values()) {
			vt.setPcuEquivalents(vt.getPcuEquivalents()*percentage);
		}
	}

	/**
	 * modifiy population
	 */
	private void population() {
		log.info("creating car only network");
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

	private void networkAndSchedule() {
//		NetworkTools.replaceNonCarModesWithPT(network);
//		ScheduleCleaner.replaceScheduleModes(schedule, TransportMode.pt);

		for(Link link : network.getLinks().values()) {
			if(link.getAllowedModes().contains("rail") || link.getAllowedModes().contains("light_rail")) {
				link.setCapacity(9999);
			}
		}

		Coord effretikon = new Coord(2693780.0, 1253409.0);
		double radius = 20000;

		new ScheduleCutter(schedule, vehicles, new CoordFilter.CoordFilterCircle(effretikon, radius)).cutSchedule();

		ScheduleCleaner.cleanVehicles(schedule, vehicles);
	}

	private void editSchedule() {
		/*
		Network network = null;
		TransitSchedule schedule = null;
		ScheduleEditor editor = new BasicScheduleEditor(schedule, network);
		Link l = network.getLinks().get(Id.createLinkId("350213"));
		editor.addLink(Id.createLinkId("350213-2"), l.getToNode().getId(), l.getFromNode().getId(), l.getId());
		editor.refreshSchedule();

		TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll(schedule, network);
		TransitScheduleValidator.printResult(result);
		*/
	}

	private static void createConfig() {
		Config config = ConfigUtils.createConfig();
		new ConfigWriter(config).write("config.xml");
	}

	
}