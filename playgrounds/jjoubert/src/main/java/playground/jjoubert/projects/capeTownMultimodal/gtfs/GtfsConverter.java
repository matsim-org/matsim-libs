/* *********************************************************************** *
 * project: org.matsim.*
 * GtfsConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.projects.capeTownMultimodal.gtfs;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import GTFS2PTSchedule.GTFS2MATSimTransitSchedule;
import playground.southafrica.utilities.Header;

/**
 * Class to convert the City of Cape Town GTFS feed.
 * 
 * @author jwjoubert
 */
public class GtfsConverter {
	final private static Logger LOG = Logger.getLogger(GtfsConverter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GtfsConverter.class.toString(), args);
		
		String folderName = args[0];
		folderName += folderName.endsWith("/") ? "" : "/";
		String network = args[1];

		File folder = new File(folderName);
		File[] folders = {folder};
		String[] modes = {"car"};
		String[] serviceIds = {"daily"};
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).parse(network);
		LOG.info("Total number of links in (original) network: " + sc.getNetwork().getLinks().size());
		
		GTFS2MATSimTransitSchedule g2m = new GTFS2MATSimTransitSchedule(folders, modes, sc.getNetwork(), serviceIds, "WGS84_SA_Albers");
		TransitSchedule ts = g2m.getTransitSchedule();
		
		String[] parts = {};
		int[] indices = {};
		int publicSystemNumber = 0;
		g2m.processTrip(parts, indices, publicSystemNumber);
		
		
		
		
		new TransitScheduleWriter(ts).writeFile(folderName + "transitSchedule.xml.gz");
		
		LOG.info("Total number of links in (post-transit) network: " + sc.getNetwork().getLinks().size());
		cleanupStops(ts);
		Header.printFooter();
	}
	
	public static void cleanupStops(TransitSchedule schedule){
		Map<Id<Link>, List<Id<TransitStopFacility>>> map = new HashMap<Id<Link>, List<Id<TransitStopFacility>>>();
		
		LOG.info("Number of TransitStopFacility's: " + schedule.getFacilities().size());
		for(TransitStopFacility tsf : schedule.getFacilities().values()){
		}
		
		int routes = 0;
		LOG.info("Number of TransitLines: " + schedule.getTransitLines().size());
		for(TransitLine tl : schedule.getTransitLines().values()){
			routes += tl.getRoutes().size();
		}
		LOG.info("Number of TransitRoute's: " + routes);
	}

}
