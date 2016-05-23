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

import playground.polettif.publicTransitMapping.gtfs.GTFSConverter;
import playground.polettif.publicTransitMapping.hafas.PTScheduleCreator;
import playground.polettif.publicTransitMapping.osm.Osm2MultimodalNetworkConverter;
import playground.polettif.publicTransitMapping.plausibility.PlausibilityCheck;

/**
 * Class that shows the different access points of the
 * PublicTransitMapping package and how to use/run
 * them.
 *
 * @author polettif
 */
public class Run {

	public static void main(String[] args) {

	}

	public void gtfs2mts() {
		String base = "C:/Users/Flavio/Desktop/data/";

		String gtfsPath = base+"gtfs/zvv/";
		String mtsFile = base+"mts/fromGtfs/zvv_mostServices.xml";
		String vehiclesFile = base+"vehicles/fromGtfs/zvv_mostServices_vehicles.xml";
		String shapeFile = base+"gtfs/shp/zvv_mostServices.shp";

		GTFSConverter.run(gtfsPath, GTFSConverter.DAY_WITH_MOST_SERVICES, "CH1903_LV03_Plus", mtsFile);
		// or
		GTFSConverter.run(gtfsPath, GTFSConverter.DAY_WITH_MOST_SERVICES, "CH1903_LV03_Plus", mtsFile, vehiclesFile, shapeFile);
	}

	public void hafas2mts() {
		String hafasFolder = "";
		String outputFolder = "";
		String outputSystem = "CH1903_LV03_Plus";

		PTScheduleCreator.run(hafasFolder, outputFolder, outputSystem);
	}

	public void osm2network() {
		String configFile = "";
		Osm2MultimodalNetworkConverter.run(configFile);
	}

	public void plausibilityCheck() {
		String scheduleFile = "";
		String networkFile = "";
		String outputFolder = "";

		PlausibilityCheck.run(scheduleFile, networkFile, outputFolder);
	}

	public void editSchedule() {

	}

}
