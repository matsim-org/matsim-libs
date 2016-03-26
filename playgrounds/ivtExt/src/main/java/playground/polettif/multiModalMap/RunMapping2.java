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


package playground.polettif.multiModalMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.TransitScheduleValidator;
import playground.polettif.multiModalMap.mapping.PTMapperLinkScoring;

public class RunMapping2 {

	protected static Logger log = Logger.getLogger(RunMapping2.class);


	public static void main(final String[] args) {

		boolean reloadGTFS = false;
		boolean reloadNetwork = false;

		String base = "C:/Users/Flavio/Desktop/data/ptMappingTest/";

		// input
		final String mtsFile = base + "testSchedule_full.xml";

		final String networkFile = base + "network_berlin.xml.gz";

		final String outbase = base + "output/";

		final String outCoordinateSystem = "GK4"; //"CH1903_LV03_Plus";

		// fields
		TransitSchedule schedule;
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	//	CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, outCoordinateSystem);

		final String path_MixedSchedule = outbase + "schedule.xml";
		final String path_MixedNetwork = outbase + "network.xml";

		// Load Schedule
		new TransitScheduleReader(scenario).readFile(mtsFile);
		schedule = scenario.getTransitSchedule();


		// Load Network
		(new MatsimNetworkReader(scenario.getNetwork())).readFile(networkFile);
		Network network = scenario.getNetwork();


		// MAPPING
		// vgl. OSM2MixedIVT createMixed
	//	new PTMapperV1(schedule).routePTLines(network);
//		new PTMapperV2(schedule).routePTLines(network);
		new PTMapperLinkScoring(schedule).routePTLines(network);

		log.info("Writing schedule and network to file...");
		new TransitScheduleWriter(schedule).writeFile(path_MixedSchedule);
		new NetworkWriter(network).write(path_MixedNetwork);
		log.info("Writing schedule and network to file... done");

		log.info("Validating transit schedule...");
		try {
			TransitScheduleValidator.main(new String[]{path_MixedSchedule, path_MixedNetwork});
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("Validating transit schedule... done");

		log.info("Schedule mapped!");
	}

}