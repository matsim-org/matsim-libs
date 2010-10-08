/* *********************************************************************** *
 * project: org.matsim.*
 * AlbatrossFacilitiesCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.netherlands.facilities;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.christoph.netherlands.zones.GetZoneConnectors;

public class AlbatrossFacilitiesCreator {

	private static final Logger log = Logger.getLogger(AlbatrossFacilitiesCreator.class);

	private static String networkFile = "../../matsim/mysimulations/netherlands/network/network_with_connectors.xml.gz";
	private static String facilitiesFile = "../../matsim/mysimulations/netherlands/facilities/facilities.xml.gz";
	private static String f2lFile = "../../matsim/mysimulations/netherlands/facilities/f2l.txt";
		
	private Charset charset = Charset.forName("UTF-8");
	
	private Scenario scenario;
	private Map<Integer, List<Id>> connectorLinksMapping;
	private ActivityFacilitiesImpl activityFacilities;
	
	private double capacity = 1000000.0;
	
	public static void main(String[] args) throws Exception {
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new AlbatrossFacilitiesCreator(scenario);	
	}
	
	public AlbatrossFacilitiesCreator(Scenario scenario) throws Exception {
		this.scenario = scenario;
		
		log.info("Getting Connector Links...");
		connectorLinksMapping = new GetZoneConnectors(scenario.getNetwork()).getMapping(); 
		log.info("done.");
		
		log.info("Creating Facilities...");
		createFacilities();
		log.info("done.");

		log.info("Writing Facilities file...");
		writeFacilities();
		log.info("done.");
		
		log.info("Writing f2l file...");
		createAndWriteF2LMapping();
		log.info("done.");
	}
	
	/*
	 * Create Facilities
	 * 
	 * The coordinate of the Facility is 1m away from the center of
	 * the link. If we would use exactly the same coordinate as the link
	 * we could get two facilities with the same coordinate (from & to link). 
	 */
	/*package*/ void createFacilities() {
		activityFacilities = ((ScenarioImpl)scenario).getActivityFacilities();
	
		for (Entry<Integer, List<Id>> entry : connectorLinksMapping.entrySet()) {
//			int TAZ = entry.getKey();			
			List<Id> linkIds = entry.getValue();
			
			for (Id linkId : linkIds) {
				Link link = scenario.getNetwork().getLinks().get(linkId);
				
				double fromX = link.getFromNode().getCoord().getX();
				double fromY = link.getFromNode().getCoord().getY();
				double toX = link.getToNode().getCoord().getX();
				double toY = link.getToNode().getCoord().getY();
				
				double dX = toX - fromX;
				double dY = toY - fromY;
				
				double length = Math.sqrt(Math.pow(dX, 2) + Math.pow(dY, 2));
				
				double centerX = link.getCoord().getX();
				double centerY = link.getCoord().getY();
				
				/*
				 * Unit Vector that directs with an angle of 90° away from the link.
				 */
				double unitVectorX = dY/length;
				double unitVectorY = -dX/length;
				
				Coord coord = new CoordImpl(centerX + unitVectorX, centerY + unitVectorY);
				
				ActivityFacilityImpl facility = activityFacilities.createFacility(linkId, coord);
				facility.setLinkId(((LinkImpl)link).getId());
				
				createAndAddActivityOptions(facility);
			}
		}
	}
		
	/*
	 * Currently we do not convert Albatross Activity Types to MATSim
	 * Types. This might leed to problems when closing the loop
	 * Albatross -> MATSim -> Abatross -> ...
	 * 
	 * We assume that the assignment of the ActivityOptions is done meaningful
	 * by Alabatross. Therefore each Facility offers each possible Activity.
	 */
	private void createAndAddActivityOptions(ActivityFacilityImpl facility) {
			
		ActivityOptionImpl activityOption;
		
		String[] albatrossActivityTypes = new String[]{"work", "business", "brget", "shop1", "shopn", "service", "social", "leisure", "touring", "home"};
		
		for (String albatrossActivityType : albatrossActivityTypes) {
			activityOption = facility.createActivityOption(albatrossActivityType);
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
			activityOption.setCapacity(capacity);			
		}
	}
	
	/*
	 * Write facilities file
	 */
	/*package*/ void writeFacilities() {
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(activityFacilities);
		facilitiesWriter.write(facilitiesFile);
	}
	
	/*
	 * Create f2l Mapping
	 * 
	 * We have only one facility per link so we use a 1:1 mapping of the IDs.
	 * 
	 */
	/*package*/ void createAndWriteF2LMapping() throws Exception {
		FileOutputStream fos = new FileOutputStream(f2lFile);
		OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
		BufferedWriter bw = new BufferedWriter(osw);
		
		// write Header
		bw.write("fid" + "\t" + "lid" + "\n");
		
		/*
		 * To each Connector Link exactly one Facility is mapped which has the
		 * same Id.
		 */
		for (List<Id> linkIds : connectorLinksMapping.values()) {
			for (Id linkId : linkIds) {
				bw.write(linkId.toString() + "\t" + linkId.toString() + "\n");				
			}
		}
		
		bw.close();
		osw.close();
		fos.close();
	}
}