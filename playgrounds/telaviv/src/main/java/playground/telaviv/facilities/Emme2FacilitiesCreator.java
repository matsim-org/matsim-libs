/* *********************************************************************** *
 * project: org.matsim.*
 * Emme2FacilitiesCreator.java
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

package playground.telaviv.facilities;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacilitiesFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.telaviv.config.TelAvivConfig;
import playground.telaviv.zones.Emme2Zone;
import playground.telaviv.zones.ZoneMapping;

public class Emme2FacilitiesCreator {

	private static final Logger log = Logger.getLogger(Emme2FacilitiesCreator.class);

	private static String networkFile = TelAvivConfig.basePath + "/network/network.xml";
	private String facilitiesFile = TelAvivConfig.basePath + "/facilities/facilities.xml";
	private String f2lFile = TelAvivConfig.basePath + "/facilities/f2l.txt";
	
	private Scenario scenario;
	private ZoneMapping zoneMapping;
	private ActivityFacilities activityFacilities;
	
	private double capacity = 1000000.0;
	
	private int[] validLinkTypes = new int[] { 2, 3, 4, 5, 6, 9 };
	
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		Emme2FacilitiesCreator facilitiesCreator = new Emme2FacilitiesCreator(scenario);
		
		facilitiesCreator.createInternalFacilities();
		facilitiesCreator.createExternalFacilities();
		facilitiesCreator.createAndWriteF2LMapping();
		facilitiesCreator.writeFacilities();		
	}
	
	public Emme2FacilitiesCreator(Scenario scenario) {
		this.scenario = scenario;
		
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
//		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("EPSG:28193", "EPSG:2039");
//		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:28193", "WGS84"));
//		zoneMapping = new ZoneMapping(scenario, new IdentityTransformation());
	}
	
	public Emme2FacilitiesCreator(Scenario scenario, ZoneMapping zoneMapping) {
		this.scenario = scenario;			
		this.zoneMapping = zoneMapping;			
	}
	
	/*
	 * Create Facilities inside the simulated area
	 * 
	 * The coordinate of the Facility is 1m away from the center of
	 * the link. If we would use exactly the same coordinate as the link
	 * we could get two facilities with the same coordinate (from & to link). 
	 */
	public void createInternalFacilities() {
		activityFacilities = scenario.getActivityFacilities();
		
		List<Integer> validTypes = new ArrayList<Integer>();
		for (int type : validLinkTypes) validTypes.add(type);
	
		for (Entry<Id, SimpleFeature> entry : zoneMapping.getLinkMapping().entrySet()) {
			Id id = entry.getKey();
						
			Link link = zoneMapping.getNetwork().getLinks().get(id);
			
			/*
			 * Check whether the link type allows Facilities or not.
			 */
			LinkImpl linkImpl = (LinkImpl) link;
			int type = Integer.valueOf(linkImpl.getType());
			if (!validTypes.contains(type)) continue;
			
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
			
			ActivityFacility facility = activityFacilities.getFactory().createActivityFacility(id, coord);
			activityFacilities.addActivityFacility(facility);
			((ActivityFacilityImpl) facility).setLinkId(((LinkImpl)link).getId());
			
			createAndAddActivityOptions(facility);
			
//		 * home	/	no (Activity)	/	0 .. 24
//		 * work	/	work	/	8 .. 18
//		 * education	/	study	/	8 .. 18
//		 * shopping	/	shopping	/	9 .. 19
//		 * leisure	/	other	6 .. 22
		}
	}
	
	/*
	 * Create external Facilities that are used by Transit Traffic Agents.
	 */
	public void createExternalFacilities() {	
		/*
		 * we add a tta Activity to all already existing facilities 
		 */
		for (ActivityFacility facility : activityFacilities.getFacilities().values()) {
			ActivityOptionImpl activityOption = ((ActivityFacilityImpl)facility).createActivityOption("tta");			
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
			activityOption.setCapacity(capacity);
		}
		
		/*
		 * We check for all OutLinks of all external Nodes if they
		 * already host a Facility. If not, a new Facility with a tta
		 * ActivityOption will be created and added. 
		 */
		for (Id id : zoneMapping.getExternalNodes()) {
			Node externalNode = zoneMapping.getNetwork().getNodes().get(id);
			
			for (Link externalLink : externalNode.getOutLinks().values()) {
				ActivityFacility facility = activityFacilities.getFacilities().get(externalLink.getId());
				
				// if already a facility exists we have nothing left to do
				if (facility != null) continue;

				/*
				 * No Facility exists at that Link therefore we create and add a new one.
				 */				
				double fromX = externalLink.getFromNode().getCoord().getX();
				double fromY = externalLink.getFromNode().getCoord().getY();
				double toX = externalLink.getToNode().getCoord().getX();
				double toY = externalLink.getToNode().getCoord().getY();
				
				double dX = toX - fromX;
				double dY = toY - fromY;
				
				double length = Math.sqrt(Math.pow(dX, 2) + Math.pow(dY, 2));
				
				double centerX = externalLink.getCoord().getX();
				double centerY = externalLink.getCoord().getY();
				
				/*
				 * Unit Vector that directs with an angle of 90° away from the link.
				 */
				double unitVectorX = dY/length;
				double unitVectorY = -dX/length;
				
				Coord coord = new CoordImpl(centerX + unitVectorX, centerY + unitVectorY);
				
				facility = activityFacilities.getFactory().createActivityFacility(externalLink.getId(), coord);
				activityFacilities.addActivityFacility(facility);
				((ActivityFacilityImpl) facility).setLinkId(((LinkImpl)externalLink).getId());
				
				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("tta");
				activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
				activityOption.setCapacity(capacity);
			}
			
		}
	}
	
	/*
	 * Creates and adds the possible activities to the facility. The capacities
	 * have to defined elsewhere...
	 * 
	 * Mapping from the zones file:
	 * 
	 * Cultural Areas -> leisure, work
	 * Education -> education_university, education_highschool, education_elementaryschool, work
	 * Office -> work
	 * Shopping -> leisure, work
	 * Health Institutions -> work, leisure
	 * Urban Cores -> ignore
	 * Religions Character -> ignore
	 * Transportation -> work, leisure (airport, big train stations, etc.)
	 */
	private void createAndAddActivityOptions(ActivityFacility facility) {
		boolean hasHome = false;
		boolean hasWork = false;
		boolean hasEducationUniversity = false;
		boolean hasEducationHighSchool = false;
		boolean hasEducationElementarySchool = false;
		boolean hasShopping = false;
		boolean hasLeisure = false;
		
		// Get the zone where the facility's link is mapped to.
		SimpleFeature zone = zoneMapping.getLinkMapping().get(facility.getLinkId());
		int TAZ = (Integer) zone.getAttribute(3);
		
		Emme2Zone parsedZone = zoneMapping.getParsedZone(TAZ);
		hasHome = parsedZone.hasHome();
		hasWork = parsedZone.hasWork();
		hasEducationUniversity = parsedZone.hasEducationUniversity();
		hasEducationHighSchool = parsedZone.hasEducationHighSchool();
		hasEducationElementarySchool = parsedZone.hasEducationElementarySchool();
		hasShopping = parsedZone.hasShopping();
		hasLeisure = parsedZone.hasLeisure();
//		if (parsedZone.POPULATION > 0) { hasHome = true; }
//		if (parsedZone.CULTURAL > 0) { hasLeisure = true; hasWork = true; }
//		if (parsedZone.EDUCATION == 1) { hasEducationUniversity = true; hasWork = true; }
//		if (parsedZone.EDUCATION == 2) { hasEducationHighSchool = true; hasWork = true; }
//		if (parsedZone.EDUCATION == 3) { hasEducationElementarySchool = true; hasWork = true; }
//		if (parsedZone.OFFICE > 0) { hasWork = true; }
//		if (parsedZone.SHOPPING > 0) { hasShopping = true; hasWork = true; }
//		if (parsedZone.HEALTH > 0) { hasLeisure = true; hasWork = true; }
//		if (parsedZone.TRANSPORTA > 0) { hasLeisure = true; hasWork = true; }
//		if (parsedZone.EMPL_TOT > 0) { hasWork = true; }

		// "Other" activities - should be possible in every zone.
//		hasLeisure = true;
		
		// "Shopping" activities - should be possible in every zone.
//		hasShopping = true;
		
		ActivityOption activityOption;
			
		ActivityFacilitiesFactory factory = this.activityFacilities.getFactory();
		
		if (hasHome) {
			activityOption = factory.createActivityOption("home");
			facility.addActivityOption(activityOption);
			activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
			activityOption.setCapacity(capacity);			
		}
		
		if (hasWork) {
			activityOption = factory.createActivityOption("work");
			facility.addActivityOption(activityOption);
			activityOption.addOpeningTime(new OpeningTimeImpl(8*3600, 18*3600));
			activityOption.setCapacity(capacity);			
		}
		
		if (hasEducationUniversity) {
			activityOption = factory.createActivityOption("education_university");
			facility.addActivityOption(activityOption);
			activityOption.addOpeningTime(new OpeningTimeImpl(9*3600, 18*3600));
			activityOption.setCapacity(capacity);			
		}
		
		if (hasEducationHighSchool) {
			activityOption = factory.createActivityOption("education_highschool");
			facility.addActivityOption(activityOption);
			activityOption.addOpeningTime(new OpeningTimeImpl(8*3600, 16*3600));
			activityOption.setCapacity(capacity);			
		}

		if (hasEducationElementarySchool) {
			activityOption = factory.createActivityOption("education_elementaryschool");
			facility.addActivityOption(activityOption);
			activityOption.addOpeningTime(new OpeningTimeImpl(8*3600, 14*3600));
			activityOption.setCapacity(capacity);			
		}
		
		if (hasShopping) {
			activityOption = factory.createActivityOption("shopping");
			facility.addActivityOption(activityOption);
			activityOption.addOpeningTime(new OpeningTimeImpl(9*3600, 19*3600));
			activityOption.setCapacity(capacity);
		}

		if (hasLeisure) {
			activityOption = factory.createActivityOption("leisure");
			facility.addActivityOption(activityOption);
			activityOption.addOpeningTime(new OpeningTimeImpl(6*3600, 22*3600));
			activityOption.setCapacity(capacity);			
		}
	}
	
	/*
	 * Write facilities file
	 */
	public void writeFacilities() {
		log.info("Writing " + activityFacilities.getFacilities().size() + " facilities to a file...");
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(activityFacilities);
		facilitiesWriter.write(facilitiesFile);
		log.info("done.");		
	}
	
	/*
	 * Create f2l Mapping
	 * 
	 * We have only one facility per link so we use a 1:1 mapping of the IDs.
	 * 
	 */
	public void createAndWriteF2LMapping() {
		log.info("Creating f2l mapping and write it to a file...");
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(f2lFile);
			
			// write Header
			bw.write("fid" + "\t" + "lid" + "\n");
			
			for (Id id : activityFacilities.getFacilities().keySet()) {
				bw.write(id.toString() + "\t" + id.toString() + "\n");
			}
			
//			for (Id id : zoneMapping.getLinkMapping().keySet()) {
//				bw.write(id.toString() + "\t" + id.toString() + "\n");
//			}
			
			bw.flush();
			bw.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");		
	}
	
	public List<Id> getLinkIdsInZoneForFacilites(int TAZ) {
		Emme2Zone zone = zoneMapping.getParsedZone(TAZ);
		if (zone == null) return null;
		
		List<Id> validLinks = new ArrayList<Id>(zone.linkIds);

		List<Integer> validTypes = new ArrayList<Integer>();
		for (int type : validLinkTypes) validTypes.add(type);

		Iterator<Id> iter = validLinks.iterator();
		while (iter.hasNext()) {
			LinkImpl link = (LinkImpl) zoneMapping.getNetwork().getLinks().get(iter.next());
			int type = Integer.valueOf(link.getType());
			if (!validTypes.contains(type)) iter.remove();
		}
		
		return validLinks;
	}
	
	public ActivityFacility getActivityFacility(Id id) {
		return this.activityFacilities.getFacilities().get(id);
	}
}