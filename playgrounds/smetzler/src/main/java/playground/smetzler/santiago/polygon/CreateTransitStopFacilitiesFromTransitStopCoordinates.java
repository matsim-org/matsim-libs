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

package playground.smetzler.santiago.polygon;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * 
 * @author aneumann
 *
 */
public class CreateTransitStopFacilitiesFromTransitStopCoordinates {

	
	private static final Logger log = Logger.getLogger(CreateTransitStopFacilitiesFromTransitStopCoordinates.class);
	
	public static void createTransitStopFacilitiesFromTransitStopCoordinates(String networkFilename, String transitStopListFilename, String transitScheduleFilename){
		Map<String, List<Coord>> ptZoneId2TransitStopCoordinates = ReadStopTable.readGenericCSV(transitStopListFilename);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		
		TransitSchedule transitSchedule = createTransitStopFacilitiesFromTransitStopCoordinates(scenario, ptZoneId2TransitStopCoordinates);
		transitSchedule = filterTransitStopFacilities(scenario, transitSchedule);
		
		new TransitScheduleWriter(transitSchedule).writeFile(transitScheduleFilename);
	}		

	private static TransitSchedule createTransitStopFacilitiesFromTransitStopCoordinates(Scenario scenario, Map<String, List<Coord>> ptZoneId2TransitStopCoordinates) {

		TransitSchedule transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		int numberOfStopsCreated = 0;
		
		for (List<Coord> transitStopCoords : ptZoneId2TransitStopCoordinates.values()) {
			for (Coord transitStopCoord : transitStopCoords) {
				numberOfStopsCreated++;
				Link link = NetworkUtils.getNearestLink(scenario.getNetwork(), transitStopCoord);
				Id<TransitStopFacility> stopId = Id.create(numberOfStopsCreated, TransitStopFacility.class);
				TransitStopFacility transitStop = transitSchedule.getFactory().createTransitStopFacility(stopId, transitStopCoord, false);
				transitStop.setLinkId(link.getId());
				transitSchedule.addStopFacility(transitStop);
			}
		}
		
		log.info("Created " + numberOfStopsCreated + " transit stops");
		
		return transitSchedule;
	}
	
	/**
	 * reduce to one stop per link
	 * @return
	 */
	private static TransitSchedule filterTransitStopFacilities(Scenario scenario, TransitSchedule transitSchedule) {

		TransitSchedule newTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		int numberOfStopsCreated = 0;
		
		HashMap<Id<Link>, List<TransitStopFacility>> linkId2Stops = new HashMap<Id<Link>, List<TransitStopFacility>>();
		for (TransitStopFacility transitStop : transitSchedule.getFacilities().values()) {
			if (linkId2Stops.get(transitStop.getLinkId()) == null) {
				linkId2Stops.put(transitStop.getLinkId(), new LinkedList<TransitStopFacility>());
			}
			
			linkId2Stops.get(transitStop.getLinkId()).add(transitStop);
		}
		
		for (Id<Link> linkId : linkId2Stops.keySet()) {
			Link link = scenario.getNetwork().getLinks().get(linkId);
			
			double bestDistance = Double.POSITIVE_INFINITY;
			TransitStopFacility bestStop = null;
			
			for (TransitStopFacility transitStop : linkId2Stops.get(linkId)) {
				double distance = CoordUtils.calcDistance(transitStop.getCoord(), link.getToNode().getCoord());
				if (distance < bestDistance) {
					bestStop = transitStop;
				}
			}
			
			newTransitSchedule.addStopFacility(bestStop);
			numberOfStopsCreated++;
		}
		
		log.info(numberOfStopsCreated + " transit stops remain after filtering");
		
		return newTransitSchedule;
	}
	
	public static void main(String[] args) throws Exception {
		
		final String directory = "e:/_shared-svn/_data/santiago_pt_demand_matrix/";
		final String transitStopListFilename = directory + "/raw_data/Diccionario paradero-zona777.csv.gz";
		final String networkFilename = directory + "network/santiago_tiny_transformed.xml.gz";
		final String transitScheduleFilename = directory + "converted_data/transitSchedule_tiny.xml.gz";
		
		CreateTransitStopFacilitiesFromTransitStopCoordinates.createTransitStopFacilitiesFromTransitStopCoordinates(networkFilename, transitStopListFilename, transitScheduleFilename);
	}
}
