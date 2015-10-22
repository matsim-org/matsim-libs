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

package playground.benjamin.scenarios.santiago.gtfs;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import playground.mzilske.gtfs.GtfsConverter;

public class SantiagoGtfsConvert {
	private static final Logger log = Logger.getLogger(SantiagoGtfsConvert.class);

	public static void main( String[] args ) {
		CoordinateTransformation transform0  = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:32719");
//		CoordinateTransformation transform0  = new WGS84toCH1903LV03() ;
		// ---
		Config config = ConfigUtils.createConfig();
		Scenario scenario0 = ScenarioUtils.createScenario(config);
		// ---
		final String inputPath = "../../../shared-svn/studies/countries/cl/santiago_pt_demand_matrix/gtfs_201306";
		final String outputPath = "../../../shared-svn/studies/countries/cl/Kai_und_Daniel/inputForMATSim/transit";
		// ---
		GtfsConverter converter = new GtfsConverter(inputPath, scenario0, transform0 ) ;
		converter.convert() ;
		// ---
		
		File output = new File(outputPath);
		if(!output.exists()) createDir(new File(outputPath));
		new NetworkWriter(scenario0.getNetwork()).write( outputPath + "/transitnetwork.xml.gz");
		new TransitScheduleWriter( scenario0.getTransitSchedule() ).writeFile( outputPath + "/transitschedule.xml.gz");
		new VehicleWriterV1( scenario0.getTransitVehicles() ).writeFile( outputPath + "/transitvehicles.xml.gz");
	}

	private static void createDir(File file) {
		log.info("Directory " + file + " created: " + file.mkdirs());	
	}

}
