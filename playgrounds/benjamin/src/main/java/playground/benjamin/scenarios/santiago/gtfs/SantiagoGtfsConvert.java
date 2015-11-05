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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.temporal.object.Utils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleWriterV1;

import playground.mzilske.gtfs.GtfsConverter;

public class SantiagoGtfsConvert {
	private static final Logger log = Logger.getLogger(SantiagoGtfsConvert.class);

	public static void main( String[] args ) {
		CoordinateTransformation transform  = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:32719");
//		CoordinateTransformation transform0  = new WGS84toCH1903LV03() ;
		// ---
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		// ---
		final String inputPath = "../../../shared-svn/studies/countries/cl/santiago_pt_demand_matrix/gtfs_201306";
		final String outputPath = "../../../shared-svn/studies/countries/cl/Kai_und_Daniel/inputForMATSim/transit";
		// ---
		GtfsConverter converter = new GtfsConverter(inputPath, scenario, transform) ;
		converter.convert() ;
		// ---
		
		File output = new File(outputPath);
		if(!output.exists()) createDir(new File(outputPath));
		new NetworkWriter(scenario.getNetwork()).write(outputPath + "/transitnetwork.xml.gz");
		
		// ***BEGIN*** Ugly hack to map transit schedule to the network to be merged later on...
		TransitSchedule ts = scenario.getTransitSchedule();
		String prefix = TransportMode.pt;
		for(TransitStopFacility tsf : ts.getFacilities().values()){
			tsf.setLinkId(Id.createLinkId(prefix.concat(tsf.getLinkId().toString())));
		}
		for(TransitLine tl : ts.getTransitLines().values()){
			for(TransitRoute tr : tl.getRoutes().values()){
				NetworkRoute nr = tr.getRoute();
				List<Id<Link>> newRouteIds = new ArrayList<>();
				for(Id<Link> lid : nr.getLinkIds()){
					Id<Link> newLink = Id.createLinkId(prefix.concat(lid.toString()));
					newRouteIds.add(newLink);
				}
				NetworkRoute newRoute = RouteUtils.createNetworkRoute(newRouteIds, scenario.getNetwork());
				tr.setRoute(newRoute);
			}
		}
		// ***END*** Ugly hack to map transit schedule to the network to be merged later on...
		
		new TransitScheduleWriter(ts).writeFile(outputPath + "/transitschedule.xml.gz");
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(outputPath + "/transitvehicles.xml.gz");
//		TransitScheduleSimplifier.mergeEqualRouteProfiles(ts, outputPath + "/transitschedule_simplified.xml.gz");
		TransitScheduleSimplifier.mergeEqualProfilesOfAllRoutes(ts, outputPath + "/transitschedule_simplified.xml.gz");
//		TransitScheduleSimplifier.mergeTouchingRoutes(scenario, outputPath + "/transitschedule_simplified.xml.gz");
	}

	private static void createDir(File file) {
		log.info("Directory " + file + " created: " + file.mkdirs());	
	}

}
