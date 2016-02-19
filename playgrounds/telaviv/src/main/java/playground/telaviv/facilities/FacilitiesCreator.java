/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesCreator.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.telaviv.config.XMLParameterParser;
import playground.telaviv.zones.Emme2Zone;
import playground.telaviv.zones.Emme2ZonesFileParser;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * <p>
 * This class creates facilities for each zone. Available activity options for each facilities
 * are defined based on information from a file containing zonal attributes.
 * </p>
 * <p>
 * So far, no information related to buildings in the zones is available. Therefore, random coordinates
 * within each zone are drawn. The number of buildings is defined by a config parameter. For each
 * of this coordinates, a facility is created and attached to the nearest link with a valid link type.
 * By default, e.g. highways are ignored for this assignment.
 * </p>
 * <p>
 * If a more detailed network is available, this class can be simply re-run to create a new mapping from
 * facilities to links.
 * </p>
 * <p>
 * Note that zonal shp file has to use WGS 84 coordinates!
 * </p>
 * @author cdobler
 */
public class FacilitiesCreator {

	private static final Logger log = Logger.getLogger(FacilitiesCreator.class);

	public static String ttaActivityType = "tta";
	public static String TAZObjectAttributesName = "taz";
	/*
	 * For each taz, we also have a unique index. In contrast to the taz values,
	 * the indices are enumerated from 0 onwards. The order of the indices equals
	 * the order of the taz values.
	 */
	public static String indexObjectAttributesName = "index";

	private static String ITM = "EPSG:2039";	// network coding String
	
	private static String basePath = "";
	private static String networkFile = "";
	private static String zonalAttributesFile = "";
	private static String zonalSHPFile = "";
	private static String facilitiesFile = "";
	private static String facilitiesAttributesFile = "";
	private static String f2lFile = "";
	
	private static int facilitiesPerZone = 100;
	private static double capacity = 1000000.0;
	
	private static Set<String> validLinkTypes = CollectionUtils.stringToSet("4,5,6");
	
	private static String separator = ",";
	
	private static Random random = MatsimRandom.getLocalInstance();
	private static GeometryFactory geometryFactory = new GeometryFactory();
	private static CoordinateTransformation fromWGS84CoordinateTransformation = new GeotoolsTransformation("WGS84", ITM);
	private static CoordinateTransformation toWGS84CoordinateTransformation = new GeotoolsTransformation(ITM, "WGS84");
	
	public static void main(String[] args) {	
		try {
			if (args.length > 0) {
				String file = args[0];
				Map<String, String> parameterMap = new XMLParameterParser().parseFile(file);
				String value;
				
				value = parameterMap.remove("basePath");
				if (value != null) basePath = value;

				value = parameterMap.remove("networkFile");
				if (value != null) networkFile = value;

				value = parameterMap.remove("zonalAttributesFile");
				if (value != null) zonalAttributesFile = value;
				
				value = parameterMap.remove("separator");
				if (value != null) separator = value;
				
				value = parameterMap.remove("zonalSHPFile");
				if (value != null) zonalSHPFile = value;
				
				value = parameterMap.remove("facilitiesFile");
				if (value != null) facilitiesFile = value;
				
				value = parameterMap.remove("facilitiesAttributesFile");
				if (value != null) facilitiesAttributesFile = value;
				
				value = parameterMap.remove("f2lFile");
				if (value != null) f2lFile = value;

				value = parameterMap.remove("facilitiesPerZone");
				if (value != null) facilitiesPerZone = Integer.parseInt(value);
				
				value = parameterMap.remove("validLinkTypes");
				if (value != null) validLinkTypes = CollectionUtils.stringToSet(value);

				for (String key : parameterMap.keySet()) log.warn("Found parameter " + key + " which is not handled!");
			} else {
				log.error("No input config file was given. Therefore cannot proceed. Aborting!");
				return;
			}
			
			log.info("loading network ...");
			Config config = ConfigUtils.createConfig();
			config.network().setInputFile(basePath + networkFile);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			log.info("done.\n");
			
			log.info("loading zonal attributes ...");
			boolean skipHeader = true;
			Map<Integer, Emme2Zone> zonalAttributes = new Emme2ZonesFileParser(basePath + zonalAttributesFile, 
					separator).readFile(skipHeader);
			log.info("done.\n");
			
			log.info("loading zonal shp file ...");
			// use a TreeMap to be deterministic
			Map<Integer, SimpleFeature> zonalShapes = new TreeMap<Integer, SimpleFeature>();
			for (SimpleFeature feature : ShapeFileReader.getAllFeatures(basePath + zonalSHPFile)) {
				zonalShapes.put((Integer) feature.getAttribute(3), feature);
			}
			log.info("done.\n");
			
			log.info("identify nodes outside the model area ...");
			Set<Id<Node>> externalNodes = getExternalNodes(scenario, zonalShapes);
			log.info("\tfound " + externalNodes.size() + " nodes outside the mapped area");
			log.info("done.\n");

			/*
			 * We have to create tta activities BEFORE filtering the network. They might also start
			 * and end at highways. We do not know their real start and end positions. The coordinate
			 * we know might only be the place where the agents enter the modeled area, which will
			 * probably be by using a highway.
			 */
			log.info("creating external facilities for tta activities ...");
			createExternalFacilities(scenario, externalNodes);
			log.info("done.\n");
			
			/*
			 * Before creating the internal facilities, we can perform the links filtering.
			 */
			log.info("removing links from network where no facilities should be attached to ...");
			List<Id<Link>> linksToRemove = new ArrayList<Id<Link>>();
			for (Link link : scenario.getNetwork().getLinks().values()) {
				String type = ((LinkImpl) link).getType();
				if (!validLinkTypes.contains(type)) linksToRemove.add(link.getId());
			}
			log.info("\tfound " + linksToRemove.size() + " links which do not match the criteria");
			for (Id<Link> linkId : linksToRemove) ((NetworkImpl) scenario.getNetwork()).removeLink(linkId);
			log.info("\tprocessed network contains " + scenario.getNetwork().getLinks().size() + 
					" valid links");
			log.info("done.\n");
			
			log.info("creating internal facilities ...");
			createInternalFacilities(scenario, zonalAttributes, zonalShapes);
			log.info("done.\n");
			
			log.info("writing facilities to links mapping to a file ...");
			createAndWriteF2LMapping(scenario);
			log.info("done.\n");
			
			log.info("writing " + scenario.getActivityFacilities().getFacilities().size() + " facilities to a file ...");
			FacilitiesWriter facilitiesWriter = new FacilitiesWriter(scenario.getActivityFacilities());
			facilitiesWriter.write(basePath + facilitiesFile);
			new ObjectAttributesXmlWriter(scenario.getActivityFacilities().getFacilityAttributes()).
				writeFile(basePath + facilitiesAttributesFile);
			log.info("done.\n");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	// Create Facilities inside the simulated area.
	private static void createInternalFacilities(Scenario scenario, Map<Integer, Emme2Zone> zonalAttributes,
			Map<Integer, SimpleFeature> zonalShapes) {
		
		// create indices for the zones
		Map<Integer, Integer> indices = new HashMap<Integer, Integer>();
		int index = 0;
		for (Integer taz : zonalShapes.keySet()) {
			indices.put(taz, index);
			index++;
		}
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		ActivityFacilities activityFacilities = scenario.getActivityFacilities();
		ObjectAttributes facilitiesAttributes = activityFacilities.getFacilityAttributes();
		ActivityFacilitiesFactory factory = activityFacilities.getFactory();

		for (Entry<Integer, SimpleFeature> entry : zonalShapes.entrySet()) {
			int taz = entry.getKey();
			SimpleFeature feature = entry.getValue();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			List<Coord> coordinates = getRandomCoordinatesInZone(facilitiesPerZone, geometry, random);
			
			int i = 0;
			for (Coord coord : coordinates) {
				Id<ActivityFacility> id = Id.create(taz + "_" + i, ActivityFacility.class);
				Link link = network.getNearestLinkExactly(coord);
				ActivityFacility facility = factory.createActivityFacility(id, coord, link.getId());
				createAndAddActivityOptions(scenario, facility, zonalAttributes.get(taz));
				activityFacilities.addActivityFacility(facility);
				i++;
				
				// Also add a tta activity to all facilities. 
				ActivityOption activityOption = factory.createActivityOption(ttaActivityType);			
				activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
				activityOption.setCapacity(capacity);
				facility.addActivityOption(activityOption);
				
				facilitiesAttributes.putAttribute(id.toString(), TAZObjectAttributesName, taz);
				facilitiesAttributes.putAttribute(id.toString(), indexObjectAttributesName, indices.get(taz));
			}
		}
	}
	
	// Create external Facilities that are used by transit traffic agents.
	private static void createExternalFacilities(Scenario scenario, Set<Id<Node>> externalNodes) {
		
		ActivityFacilities activityFacilities = scenario.getActivityFacilities();
		ActivityFacilitiesFactory factory = activityFacilities.getFactory();

		/*
		 * We check for all OutLinks of all external nodes if they already host a facility. If not, 
		 * a new facility with a tta ActivityOption will be created and added. 
		 */
		for (Id<Node> id : externalNodes) {
			Node externalNode = scenario.getNetwork().getNodes().get(id);
			
			for (Link externalLink : externalNode.getOutLinks().values()) {
				ActivityFacility facility = activityFacilities.getFacilities().get(externalLink.getId());
				
				// if already a facility exists we have nothing left to do
				if (facility != null) continue;

				/*
				 * No Facility exists at that link therefore we create and add a new one.
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
				 * Unit vector that directs with an angle of 90° away from the link.
				 */
				double unitVectorX = dY/length;
				double unitVectorY = -dX/length;

				Coord coord = new Coord(centerX + unitVectorX, centerY + unitVectorY);
				
				facility = activityFacilities.getFactory().createActivityFacility(
						Id.create(externalLink.getId().toString(), ActivityFacility.class), coord, externalLink.getId());
				activityFacilities.addActivityFacility(facility);
				
				ActivityOption activityOption = factory.createActivityOption(ttaActivityType); 
				activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
				activityOption.setCapacity(capacity);
				facility.addActivityOption(activityOption);
			}		
		}
	}
	
	/*
	 * Creates and adds the possible activities to the facility. The capacities
	 * have to defined elsewhere...
	 * 
	 * home	/	no (Activity)	/	0 .. 24
	 * work	/	work	/	8 .. 18
	 * education	/	study	/	8 .. 18
	 * shopping	/	shopping	/	9 .. 19
	 * leisure	/	other	6 .. 22
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
	private static void createAndAddActivityOptions(Scenario scenario, ActivityFacility facility, Emme2Zone zone) {
	
		boolean hasHome = false;
		boolean hasWork = false;
		boolean hasEducationUniversity = false;
		boolean hasEducationHighSchool = false;
		boolean hasEducationElementarySchool = false;
		boolean hasShopping = false;
		boolean hasLeisure = false;

		hasHome = zone.hasHome();
		hasWork = zone.hasWork();
		hasEducationUniversity = zone.hasEducationUniversity();
		hasEducationHighSchool = zone.hasEducationHighSchool();
		hasEducationElementarySchool = zone.hasEducationElementarySchool();
		hasShopping = zone.hasShopping();
		hasLeisure = zone.hasLeisure();
//		if (zone.POPULATION > 0) { hasHome = true; }
//		if (zone.CULTURAL > 0) { hasLeisure = true; hasWork = true; }
//		if (zone.EDUCATION == 1) { hasEducationUniversity = true; hasWork = true; }
//		if (zone.EDUCATION == 2) { hasEducationHighSchool = true; hasWork = true; }
//		if (zone.EDUCATION == 3) { hasEducationElementarySchool = true; hasWork = true; }
//		if (zone.OFFICE > 0) { hasWork = true; }
//		if (zone.SHOPPING > 0) { hasShopping = true; hasWork = true; }
//		if (zone.HEALTH > 0) { hasLeisure = true; hasWork = true; }
//		if (zone.TRANSPORTA > 0) { hasLeisure = true; hasWork = true; }
//		if (zone.EMPL_TOT > 0) { hasWork = true; }

		// "Other" activities - should be possible in every zone.
//		hasLeisure = true;
		
		// "Shopping" activities - should be possible in every zone.
//		hasShopping = true;
		
		ActivityOption activityOption;
			
		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		
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
	
	// create f2l mapping file
	private static void createAndWriteF2LMapping(Scenario scenario) {
		log.info("creating f2l mapping and write it to a file ...");
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(basePath + f2lFile);
			
			// write Header
			bw.write("fid" + "\t" + "lid" + "\n");
			
			for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
				bw.write(facility.getId().toString() + "\t" + facility.getLinkId().toString() + "\n");
			}
			
			bw.flush();
			bw.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("done.");		
	}
	
	// iterate over all nodes to find all external nodes
	private static final Set<Id<Node>> getExternalNodes(Scenario scenario, Map<Integer, SimpleFeature> zonalShapes) {
		
		Set<Id<Node>> externalNodes = new TreeSet<Id<Node>>();
		for (Node node : scenario.getNetwork().getNodes().values()) {
			Coord pointCoord = toWGS84CoordinateTransformation.transform(node.getCoord());
			Point point = geometryFactory.createPoint(new Coordinate(pointCoord.getX(), pointCoord.getY()));
			
			SimpleFeature pointZone = null;
			for (SimpleFeature zone : zonalShapes.values()) {
				Geometry polygon = (Geometry) zone.getDefaultGeometry();
				if (polygon.contains(point)) {
					pointZone = zone;
					break;
				}
			}
			
			// if the point is not contained in any Zone it is an external node.
			if (pointZone == null) externalNodes.add(node.getId());
		}
		
		
		return externalNodes;
	}
	
	private static final List<Coord> getRandomCoordinatesInZone(int numCoordinates, Geometry zoneGeometry, Random random) {
		
		/*
		 * Get the bounding box of the geometry. The returned geometry is a polygon with the following points:
		 * (minx, miny), (maxx, miny), (maxx, maxy), (minx, maxy), (minx, miny).
		 */
		Geometry envelope = zoneGeometry.getEnvelope();
		Coordinate[] coords = envelope.getCoordinates();
		
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;
		
		for (Coordinate coord : coords) {
			if (coord.x < minX) minX = coord.x;
			if (coord.x > maxX) maxX = coord.x;
			if (coord.y < minY) minY = coord.y;
			if (coord.y > maxY) maxY = coord.y;
		}
		
		List<Coord> list = new ArrayList<Coord>();

		// loop until a valid point was found and is returned
		while (list.size() < numCoordinates) {
			double x = minX + random.nextDouble() * (maxX - minX);
			double y = minY + random.nextDouble() * (maxY - minY);

			Point point = geometryFactory.createPoint(new Coordinate(x, y));
			if (zoneGeometry.contains(point)) list.add(fromWGS84CoordinateTransformation.transform(new Coord(x, y)));
		}
		return list;
	}
}