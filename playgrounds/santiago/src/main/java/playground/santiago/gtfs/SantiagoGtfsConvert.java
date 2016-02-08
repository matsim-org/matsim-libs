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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.mzilske.gtfs.GtfsConverter;
import playground.santiago.SantiagoScenarioConstants;

/**
 * @author kai, benjamin
 *
 */
public class SantiagoGtfsConvert {
	private static final Logger log = Logger.getLogger(SantiagoGtfsConvert.class);

	public static void main( String[] args ) {
		final String inputPath = "../../../shared-svn/projects/santiago/santiago_pt_demand_matrix/gtfs_201306";
		final String outputPath = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/transit";

		CoordinateTransformation transform  = TransformationFactory.getCoordinateTransformation("EPSG:4326", SantiagoScenarioConstants.toCRS);
//		CoordinateTransformation transform0  = new WGS84toCH1903LV03() ;

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		/* TODO: Michaels converter somehow doesnt add lines from frequencies.txt if a line 
 		with the same stops already exists from stops.txt; Here a problem mainly for metro in off-peak.
		Simply comment out line 448 in GtfsConverter. */
		GtfsConverter converter = new GtfsConverter(inputPath, scenario, transform) ;
		converter.convert() ;
		
		File output = new File(outputPath);
		if(!output.exists()) createDir(new File(outputPath));
		
		TransitSchedule ts = scenario.getTransitSchedule();
		// Ugly hack to map transit schedule to the network to be merged later on...
//		Network net = scenario.getNetwork();
//		addPrefixToLinkIds(ts, net);
		
		// Routes seem to have problems; thus, deleting them...
		removeNetworkRoutes(ts);
		
		// There are (most likely) wrongly coded departures at midnight; thus, deleting them...
		// TODO: cannot delete departures only, other adjustments in schedule need to be done.
//		removeMidnightDepartures(ts);
		
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

	private static void removeMidnightDepartures(TransitSchedule ts) {
		for(TransitLine tl : ts.getTransitLines().values()){
			for(TransitRoute tr : tl.getRoutes().values()){
				for(Departure dp : new HashSet<Departure>(tr.getDepartures().values())){
					Double dpt = dp.getDepartureTime();
					if(dpt.equals(0.0)){
						tr.removeDeparture(dp);
					}
				} 
//				for(Departure dp : tr.getDepartures().values()){
//					Double dpt = dp.getDepartureTime();
//					if(dpt.equals(0.0)){
//						tr.removeDeparture(dp);
//					}
//				}
			}
		}
	}

	private static void removeNetworkRoutes(TransitSchedule ts) {
		for(TransitLine tl : ts.getTransitLines().values()){
			for(TransitRoute tr : tl.getRoutes().values()){
//				tl.removeRoute(tl.getRoutes().get(trId));
//				NetworkRoute nr = tr.getRoute();
				tr.setRoute(null);
			}
		}
	}

	private static void addPrefixToLinkIds(TransitSchedule ts, Network net) {
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
				NetworkRoute newRoute = RouteUtils.createNetworkRoute(newRouteIds, net);
				tr.setRoute(newRoute);
			}
		}
	}

	private static void createDir(File file) {
		log.info("Directory " + file + " created: " + file.mkdirs());	
	}
}
