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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.editor.BasicScheduleEditor;
import playground.polettif.publicTransitMapping.editor.ScheduleEditor;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.Collections;
import java.util.List;

/**
 * workbench class to do some scenario preparation
 */
public class Prepare {

	protected static Logger log = Logger.getLogger(Prepare.class);
	
	public static void main(final String[] args) {
		Network network = NetworkTools.readNetwork("../output/2016-06-08-1018/ch_network.xml.gz");
		TransitSchedule schedule = ScheduleTools.readTransitSchedule("../output/2016-06-08-1018/ch_schedule.xml.gz");

		for(Link l : network.getLinks().values()) {
			if(l.getAllowedModes().contains(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE)) {
				l.setCapacity(9999);
			}
		}

//		NetworkTools.replaceNonCarModesWithPT(network);

		ScheduleCleaner.replaceScheduleModes(schedule, TransportMode.pt);

		ScheduleEditor editor = new BasicScheduleEditor(schedule, network);
		Link l = network.getLinks().get(Id.createLinkId("350213"));
		editor.addLink(Id.createLinkId("350213-2"), l.getToNode().getId(), l.getFromNode().getId(), l.getId());
		editor.refreshSchedule();

		TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll(schedule, network);
		TransitScheduleValidator.printResult(result);

		ScheduleTools.writeTransitSchedule(schedule, "prepared/ch_pt_schedule.xml.gz");
		NetworkTools.writeNetwork(network, "prepared/ch_pt_network.xml.gz");


		//		createConfig();
//		adaptPop("population_Orig.xml.gz", "zurich_hafas_network.xml", "plans.xml.gz");
//		adaptVehicles("../../data/vehicles/zurich_hafas_vehicles.xml.gz", "zurich_hafas_vehicles.xml.gz", 0.01);

	}

	private static void adaptVehicles(String inputVehicleFile, String outputVehicleFile, double percentage) {
		Vehicles transitVehicles = ScheduleTools.readVehicles(inputVehicleFile);
		for(VehicleType vt : transitVehicles.getVehicleTypes().values()) {
			vt.setPcuEquivalents(vt.getPcuEquivalents()*percentage);
		}
		ScheduleTools.writeVehicles(transitVehicles, outputVehicleFile);
	}

	private static void runScenario(String configFile) {
		final Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.run();
	}

	private static void createConfig() {
		Config config = ConfigUtils.createConfig();
		new ConfigWriter(config).write("config.xml");
	}

	/**
	 * modifiy population
	 */
	private static void adaptPop(String inputPopulationFile, String networkFile, String outputPopulationFile) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		PopulationReader reader = new PopulationReaderMatsimV5(sc);
		reader.readFile(inputPopulationFile);
		Population population = sc.getPopulation();

		Network network = NetworkTools.readNetwork(networkFile);
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
		new PopulationWriter(population, network).write(outputPopulationFile);
	}



	
}