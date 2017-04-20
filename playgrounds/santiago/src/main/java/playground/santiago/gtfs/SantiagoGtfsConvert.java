/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.santiago.gtfs;

import com.conveyal.gtfs.GTFSFeed;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import playground.santiago.SantiagoScenarioConstants;

import java.io.File;
import java.time.LocalDate;

/**
 * @author kai, benjamin
 *
 */
public class SantiagoGtfsConvert {
	private static final Logger log = Logger.getLogger(SantiagoGtfsConvert.class);

	public static void main( String[] args ) {
//		final String inputPath = "../../../shared-svn/projects/santiago/gtfs/gtfs_201306.zip";
		final String inputPath = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/gtfs/gtfs_201306.zip";
		
		final String outputPath = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/transit";

		
		CoordinateTransformation transform  = TransformationFactory.getCoordinateTransformation("EPSG:4326", SantiagoScenarioConstants.toCRS);
//		CoordinateTransformation transform0  = new WGS84toCH1903LV03() ;

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		GtfsConverter converter = new GtfsConverter(GTFSFeed.fromFile(inputPath), scenario, transform) ;
		converter.setDate(LocalDate.of(2013, 6, 1)) ;
		converter.convert() ;
		
		File output = new File(outputPath);
		if(!output.exists()) createDir(new File(outputPath));
		
		TransitSchedule ts = scenario.getTransitSchedule();

		Network transitNet = NetworkUtils.createNetwork();
		CreatePseudoNetwork creator = new CreatePseudoNetwork(ts, transitNet, TransportMode.pt);
		creator.createNetwork();
		
		new NetworkWriter(transitNet).write(outputPath + "/transitnetwork.xml.gz");
//		new NetworkWriter(net).write(outputPath + "/transitnetwork.xml.gz");

		new TransitScheduleWriter(ts).writeFile(outputPath + "/transitschedule.xml.gz");
//		TransitScheduleSimplifier.mergeEqualRouteProfiles(ts, outputPath + "/transitschedule_simplified.xml.gz");
//		TransitScheduleSimplifier.mergeEqualProfilesOfAllRoutes(ts, outputPath + "/transitschedule_simplified.xml.gz");
//		TransitScheduleSimplifier.mergeTouchingRoutes(scenario, outputPath + "/transitschedule_simplified.xml.gz");
		TransitScheduleSimplifierAndreas.simplifyTransitSchedule(scenario, outputPath + "/transitschedule_simplified.xml.gz");

		Vehicles tv = scenario.getTransitVehicles();
		new VehicleWriterV1(tv).writeFile(outputPath + "/transitvehicles.xml.gz");
	}

	private static void createDir(File file) {
		log.info("Directory " + file + " created: " + file.mkdirs());	
	}
}
