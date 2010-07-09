/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkEmme2MATSim.java
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.telaviv.zones.Emme2Zone;
import playground.telaviv.zones.ZoneMapping;

public class Emme2FacilitiesCreator {

	private static final Logger log = Logger.getLogger(Emme2FacilitiesCreator.class);

	private static String networkFile = "../../matsim/mysimulations/telaviv/network/network.xml";
	private String facilitiesFile = "../../matsim/mysimulations/telaviv/facilities/facilities.xml";
	private String f2lFile = "../../matsim/mysimulations/telaviv/facilities/f2l.txt";
		
	private Charset charset = Charset.forName("UTF-8");
	
	private Scenario scenario;
	private ZoneMapping zoneMapping;
	private ActivityFacilitiesImpl activityFacilities;
	
	private double capacity = 1000000.0;
	
	private int[] validLinkTypes = new int[] { 2, 3, 4, 5, 6, 9 };
	
	public static void main(String[] args)
	{
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		Emme2FacilitiesCreator facilitiesCreator = new Emme2FacilitiesCreator(scenario);
		
		facilitiesCreator.createInternalFacilities();
		facilitiesCreator.createExternalFacilities();
		facilitiesCreator.createAndWriteF2LMapping();
		facilitiesCreator.writeFacilities();		
	}
	
	public Emme2FacilitiesCreator(Scenario scenario)
	{
		this.scenario = scenario;
		
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
	}
	
	public Emme2FacilitiesCreator(Scenario scenario, ZoneMapping zoneMapping)
	{
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
	public void createInternalFacilities()
	{
		activityFacilities = ((ScenarioImpl)scenario).getActivityFacilities();
		
		List<Integer> validTypes = new ArrayList<Integer>();
		for (int type : validLinkTypes) validTypes.add(type);
	
		for (Entry<Id, Feature> entry : zoneMapping.getLinkMapping().entrySet())
		{
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
			
			ActivityFacilityImpl facility = activityFacilities.createFacility(id, coord);
			facility.addDownMapping((LinkImpl)link);
			
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
	public void createExternalFacilities()
	{	
		/*
		 * we add a tta Activity to all already existing facilities 
		 */
		for (ActivityFacility facility : activityFacilities.getFacilities().values())
		{
			ActivityOptionImpl activityOption = ((ActivityFacilityImpl)facility).createActivityOption("tta");			
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
			activityOption.setCapacity(capacity);
		}
		
		/*
		 * We check for all OutLinks of all external Nodes if they
		 * already host a Facility. If not, a new Facility with a tta
		 * ActivityOption will be created and added. 
		 */
		for (Id id : zoneMapping.getExternalNodes())
		{
			Node externalNode = zoneMapping.getNetwork().getNodes().get(id);
			
			for (Link externalLink : externalNode.getOutLinks().values())
			{
				ActivityFacilityImpl facility = activityFacilities.getFacilities().get(externalLink.getId());
				
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
				
				facility = activityFacilities.createFacility(externalLink.getId(), coord);
				facility.addDownMapping((LinkImpl)externalLink);
				
				ActivityOptionImpl activityOption = facility.createActivityOption("tta");
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
	private void createAndAddActivityOptions(ActivityFacilityImpl facility)
	{
		boolean hasHome = false;
		boolean hasWork = false;
		boolean hasEducationUniversity = false;
		boolean hasEducationHighSchool = false;
		boolean hasEducationElementarySchool = false;
		boolean hasShopping = false;
		boolean hasLeisure = false;
		
		// Get the zone where the facility's link is mapped to.
		Feature zone = zoneMapping.getLinkMapping().get(facility.getLinkId());
		int TAZ = (Integer) zone.getAttribute(3);
		
		Emme2Zone parsedZone = zoneMapping.getParsedZone(TAZ);
		if (parsedZone.POPULATION > 0) { hasHome = true; }
		if (parsedZone.CULTURAL > 0) { hasLeisure = true; hasWork = true; }
		if (parsedZone.EDUCATION == 1) { hasEducationUniversity = true; hasWork = true; }
		if (parsedZone.EDUCATION == 2) { hasEducationHighSchool = true; hasWork = true; }
		if (parsedZone.EDUCATION == 3) { hasEducationElementarySchool = true; hasWork = true; }
		if (parsedZone.OFFICE > 0) { hasWork = true; }
		if (parsedZone.SHOPPING > 0) { hasShopping = true; hasWork = true; }
		if (parsedZone.HEALTH > 0) { hasLeisure = true; hasWork = true; }
		if (parsedZone.TRANSPORTA > 0) { hasLeisure = true; hasWork = true; }
		if (parsedZone.EMPL_TOT > 0) { hasWork = true; }

		// "Other" activities - should be possible in every zone.
		hasLeisure = true;
		
		// "Shopping" activities - should be possible in every zone.
		hasShopping = true;
		
		ActivityOptionImpl activityOption;
		
	
		
		if (hasHome)
		{
			activityOption = facility.createActivityOption("home");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
			activityOption.setCapacity(capacity);			
		}
		
		if (hasWork)
		{
			activityOption = facility.createActivityOption("work");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 8*3600, 18*3600));
			activityOption.setCapacity(capacity);			
		}
		
		if (hasEducationUniversity)
		{
			activityOption = facility.createActivityOption("education_university");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 9*3600, 18*3600));
			activityOption.setCapacity(capacity);			
		}
		
		if (hasEducationHighSchool)
		{
			activityOption = facility.createActivityOption("education_highschool");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 8*3600, 16*3600));
			activityOption.setCapacity(capacity);			
		}

		if (hasEducationElementarySchool)
		{
			activityOption = facility.createActivityOption("education_elementaryschool");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 8*3600, 14*3600));
			activityOption.setCapacity(capacity);			
		}
		
		if (hasShopping)
		{
			activityOption = facility.createActivityOption("shopping");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 9*3600, 19*3600));
			activityOption.setCapacity(capacity);
		}

		if (hasLeisure)
		{
			activityOption = facility.createActivityOption("leisure");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 6*3600, 22*3600));
			activityOption.setCapacity(capacity);			
		}
	}
	
	/*
	 * Write facilities file
	 */
	public void writeFacilities()
	{
		log.info("Writing facilities to a file...");
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
	public void createAndWriteF2LMapping()
	{
		log.info("Creating f2l mapping and write it to a file...");
		try 
		{
			FileOutputStream fos = new FileOutputStream(f2lFile);
			OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
			BufferedWriter bw = new BufferedWriter(osw);
			
			// write Header
			bw.write("fid" + "\t" + "lid" + "\n");
			
			for (Id id : zoneMapping.getLinkMapping().keySet())
			{
				bw.write(id.toString() + "\t" + id.toString() + "\n");
			}
			
			bw.close();
			osw.close();
			fos.close();
			
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		log.info("done.");		
	}
	
	public List<Id> getLinkIdsInZoneForFacilites(int TAZ)
	{
		Emme2Zone zone = zoneMapping.getParsedZone(TAZ);
		if (zone == null) return null;
		
		List<Id> validLinks = new ArrayList<Id>(zone.linkIds);

		List<Integer> validTypes = new ArrayList<Integer>();
		for (int type : validLinkTypes) validTypes.add(type);

		Iterator<Id> iter = validLinks.iterator();
		while (iter.hasNext())
		{
			LinkImpl link = (LinkImpl) zoneMapping.getNetwork().getLinks().get(iter.next());
			int type = Integer.valueOf(link.getType());
			if (!validTypes.contains(type)) iter.remove();
		}
		
		return validLinks;
	}
	
	public ActivityFacility getActivityFacility(Id id)
	{
		return this.activityFacilities.getFacilities().get(id);
	}
}
