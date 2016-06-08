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
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.Collections;
import java.util.List;

/**
 * workbench class to do some scenario preparation
 *
 * @author polettif
 */
public class Prepare {

	protected static Logger log = Logger.getLogger(Prepare.class);

	private static Config config;

	public static void main(final String[] args) {
		config = ConfigUtils.loadConfig("config.xml");

		prepareNetworkAndSchedule("../../output/2016-06-08/ch_network.xml.gz", "../../output/2016-06-08/ch_schedule.xml.gz");

		preparePopulation("population_Orig.xml.gz");

		prepareVehicles("../../data/vehicles/ch_vehicles.xml.gz", 0.005);
	}

	private static void prepareVehicles(String inputVehicleFile, double percentage) {
		Vehicles transitVehicles = ScheduleTools.readVehicles(inputVehicleFile);
		for(VehicleType vt : transitVehicles.getVehicleTypes().values()) {
			vt.setPcuEquivalents(vt.getPcuEquivalents()*percentage);
		}
		ScheduleTools.writeVehicles(transitVehicles, config.vehicles().getVehiclesFile());
	}

	/**
	 * modifiy population
	 */
	private static void preparePopulation(String inputPopulationFile) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		PopulationReader reader = new PopulationReaderMatsimV5(sc);
		reader.readFile(inputPopulationFile);
		Population population = sc.getPopulation();

		Network network = NetworkTools.readNetwork(config.network().getInputFile());
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

		log.info("writing new plans file");
		new PopulationWriter(population, network).write(config.plans().getInputFile());
	}

	private static void createConfig() {
		Config config = ConfigUtils.createConfig();
		new ConfigWriter(config).write("config.xml");
	}

	private static void prepareNetworkAndSchedule(String n, String s) {
		Network network = NetworkTools.readNetwork(n);
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(s);

//		NetworkTools.replaceNonCarModesWithPT(network);
//		ScheduleCleaner.replaceScheduleModes(schedule, TransportMode.pt);

		ScheduleTools.writeTransitSchedule(schedule, config.transit().getTransitScheduleFile());
		NetworkTools.writeNetwork(network, config.network().getInputFile());
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


	
}