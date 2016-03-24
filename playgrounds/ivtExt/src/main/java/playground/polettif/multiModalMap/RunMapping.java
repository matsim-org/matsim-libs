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
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.TransitScheduleValidator;
import playground.polettif.boescpa.converters.osm.Osm2Network;
import playground.polettif.multiModalMap.gtfs.GTFSReader;
import playground.polettif.multiModalMap.mapping.PTMapperV1;
import playground.polettif.multiModalMap.mapping.PTMapperV2;
import playground.polettif.multiModalMap.mapping.PTMapperV3;

public class RunMapping {

	protected static Logger log = Logger.getLogger(RunMapping.class);


	public static void main(final String[] args) {

		boolean reloadGTFS = false;
		boolean reloadNetwork = false;

		// input
		final String gtfsPath = "C:/Users/polettif/Desktop/data/gtfs/zvv/";
		final String mtsFile = "C:/Users/polettif/Desktop/data/mts/zvv_unmappedSchedule_WGS84.xml";

		final String osmFile = "C:/Users/polettif/Desktop/data/osm/zurich-plus.osm";
		final String networkFile = "C:/Users/polettif/Desktop/data/network/zurich-plus.xml.gz";

		final String outbase = "C:/Users/polettif/Desktop/output/mtsMapping/";

		final String outCoordinateSystem = "CH1903_LV03_Plus";

		// fields
		TransitSchedule schedule;
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, outCoordinateSystem);

		final String path_MixedSchedule = outbase + "MixedSchedule.xml";
		final String path_MixedNetwork = outbase + "MixedNetwork.xml";

		// Load Schedule
		if (reloadGTFS) {
			GTFSReader gtfsReader = new GTFSReader(gtfsPath);
			gtfsReader.writeTransitSchedule(mtsFile);
			//schedule = gtfsReader.getTransitSchedule();
		}
		new TransitScheduleReader(coordinateTransformation, scenario).readFile(mtsFile);
		schedule = scenario.getTransitSchedule();


		// Load Network
		if (reloadNetwork) {
			// TODO coordinate transformation in Osm2Network
			Osm2Network.main(new String[]{osmFile, networkFile});
		}

		(new MatsimNetworkReader(scenario.getNetwork())).readFile(networkFile);
		Network network = scenario.getNetwork();


		// MAPPING
		// vgl. OSM2MixedIVT createMixed
	//	new PTMapperV1(schedule).routePTLines(network);
//		new PTMapperV2(schedule).routePTLines(network);
		new PTMapperV3(schedule).routePTLines(network);

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