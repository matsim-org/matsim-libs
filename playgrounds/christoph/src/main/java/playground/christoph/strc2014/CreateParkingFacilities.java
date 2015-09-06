/* *********************************************************************** *
 * project: org.matsim.*
 * CreateParkingFacilities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.christoph.strc2014;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;
import playground.christoph.parking.ParkingTypes;
import playground.christoph.parking.core.mobsim.ParkingFacility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

public class CreateParkingFacilities {
	
	private final String privateOutsideParkingFile = "../../matsim/mysimulations/parking/zurich/originalData/shp/Grundstueckparkplaetze.shp";
	private final String privateInsideParkingFile = "../../matsim/mysimulations/parking/zurich/originalData/shp/Gebaeudeparkplaetze.shp";
	private final String garageParkingFile = "../../matsim/mysimulations/parking/zurich/originalData/shp/Parkhaeuser.shp";
	private final String streetParkingFile = "../../matsim/mysimulations/parking/zurich/originalData/shp/Strassenparkplaetze.shp";
	
	private final String cantonZurichFile = "../../matsim/mysimulations/parking/zurich/originalData/shp/Zurich_Canton.shp";
	
	private double minX = Double.MAX_VALUE;
	private double minY = Double.MAX_VALUE;
	private double maxX = Double.MIN_VALUE;
	private double maxY = Double.MIN_VALUE;
		
	public static void main (String[] args) {
		
		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile("../../matsim/mysimulations/parking/zurich/input/network.xml.gz");
//		config.setParam(WorldConnectLocations.CONFIG_F2L, WorldConnectLocations.CONFIG_F2L_OUTPUTF2LFile, "../../matsim/mysimulations/parking/zurich/input/f2l.txt");
		config.network().setInputFile("../../matsim/mysimulations/parking/zurich/input/network_ivtch.xml.gz");
		config.setParam(WorldConnectLocations.CONFIG_F2L, WorldConnectLocations.CONFIG_F2L_OUTPUTF2LFile, "../../matsim/mysimulations/parking/zurich/input/f2l_ivtch.txt");
		config.facilities().setInputFile("../../matsim/mysimulations/parking/zurich/input/facilities.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new CreateParkingFacilities(scenario);
				
//		new FacilitiesWriter(scenario.getActivityFacilities()).write("../../matsim/mysimulations/parking/zurich/input/facilities_with_parking.xml.gz");
		new FacilitiesWriter(scenario.getActivityFacilities()).write("../../matsim/mysimulations/parking/zurich/input/facilities_with_parking_ivtch.xml.gz");
//		new ObjectAttributesXmlWriter(scenario.getActivityFacilities().getFacilityAttributes()).writeFile("../../matsim/mysimulations/parking/zurich/input/facilityAttributes.xml.gz");
		new ObjectAttributesXmlWriter(scenario.getActivityFacilities().getFacilityAttributes()).writeFile("../../matsim/mysimulations/parking/zurich/input/facilityAttributes_ivtch.xml.gz");

		// write f2l mapping
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(config.getParam(WorldConnectLocations.CONFIG_F2L, WorldConnectLocations.CONFIG_F2L_OUTPUTF2LFile));
			
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
	}
	
	public CreateParkingFacilities(Scenario scenario) {
		
		// assign existing facilities to network
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			if (facility.getLinkId() == null) {
				Link link = NetworkUtils.getNearestRightEntryLink(network, facility.getCoord());
				((ActivityFacilityImpl) facility).setLinkId(link.getId());
			}
		}
		
		// identify network boundaries
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			Coord coord = facility.getCoord();
			double x = coord.getX();
			double y = coord.getY();
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
		}
		
		/*
		 * Has to be called before the garage and street parkings are created. Otherwise
		 * we would have to ensure that they are not assigned to them.
		 */
		createPrivateInsideParking(scenario);
		createPrivateOutsideParking(scenario);
		
		createGarageParking(scenario);
		createStreetParking(scenario);
		
		createOutsideCantonParking(scenario);
		
		// store f2l information in object attributes
		ObjectAttributes objectAttributes = scenario.getActivityFacilities().getFacilityAttributes();
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			objectAttributes.putAttribute(facility.getId().toString(), "linkId", facility.getLinkId().toString());
		}
	}
	
	private void createPrivateInsideParking(Scenario scenario) {
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(5);
		nf.setGroupingUsed(false);
		Counter counter = new Counter("# privateInsideParking: ");
		
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(privateInsideParkingFile);
		for (SimpleFeature feature : features) {
			Point point = (Point) feature.getAttribute(0);
			Coord coord = new Coord(point.getCoordinate().x, point.getCoordinate().y);
			String name = ((Double) feature.getAttribute(1)).toString();
			double capacity = (Double) feature.getAttribute(4);
			
			// check whether coordinate is inside the area covered by the network
			if (coord.getX() < minX || coord.getX() > maxX || coord.getY() < minY || coord.getY() > maxY) continue;
			
			if (capacity <= 0) continue;
			
			Link link = NetworkUtils.getNearestRightEntryLink(network, coord);
			Id<ActivityFacility> facilityId = Id.create(ParkingTypes.PRIVATEINSIDEPARKING + "_" + nf.format(counter.getCounter()), ActivityFacility.class);
			counter.incCounter();
			ActivityFacility parkingFacility = createAndAddParkingFacility(scenario, coord, facilityId, link.getId());
			((ActivityFacilityImpl) parkingFacility).setDesc(name);
			ActivityOption activityOption = scenario.getActivityFacilities().getFactory().createActivityOption(ParkingTypes.PRIVATEINSIDEPARKING);
			activityOption.setCapacity(capacity);
			activityOption.setFacility(parkingFacility);
			parkingFacility.addActivityOption(activityOption);
		}
		counter.printCounter();
	}
	
	// Privatgrund
	private void createPrivateOutsideParking(Scenario scenario) {
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(5);
		nf.setGroupingUsed(false);
		Counter counter = new Counter("# privateOutsideParking: ");
		
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(privateOutsideParkingFile);
		for (SimpleFeature feature : features) {
			Point point = (Point) feature.getAttribute(0);
			Coord coord = new Coord(point.getCoordinate().x, point.getCoordinate().y);
			String name = (String) feature.getAttribute(1);
			double capacity = (Double) feature.getAttribute(4);
			
			// check whether coordinate is inside the area covered by the network
			if (coord.getX() < minX || coord.getX() > maxX || coord.getY() < minY || coord.getY() > maxY) continue;
			
			if (capacity <= 0) continue;
			
			Link link = NetworkUtils.getNearestRightEntryLink(network, coord);
			Id<ActivityFacility> facilityId = Id.create(ParkingTypes.PRIVATEOUTSIDEPARKING + "_" + nf.format(counter.getCounter()), ActivityFacility.class);
			counter.incCounter();
			ActivityFacility parkingFacility = createAndAddParkingFacility(scenario, coord, facilityId, link.getId());
			((ActivityFacilityImpl) parkingFacility).setDesc(name);
			ActivityOption activityOption = scenario.getActivityFacilities().getFactory().createActivityOption(ParkingTypes.PRIVATEOUTSIDEPARKING);
			activityOption.setCapacity(capacity);
			activityOption.setFacility(parkingFacility);
			parkingFacility.addActivityOption(activityOption);
			
			if (capacity > 0) {
				// so far: just assume that 10% of the parking capacity is available as waiting capacity
				int waitingCapacity = (int) ((capacity) / 10);
				
				activityOption = scenario.getActivityFacilities().getFactory().createActivityOption(ParkingFacility.WAITING);
				activityOption.setCapacity(waitingCapacity);
			}
		}
		counter.printCounter();
	}
	
	// Parkhaeuser
	private void createGarageParking(Scenario scenario) {
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(5);
		nf.setGroupingUsed(false);
		Counter counter = new Counter("# garageParking: ");
		
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(garageParkingFile);
		for (SimpleFeature feature : features) {
			Point point = (Point) feature.getAttribute(0);
			Coord coord = new Coord(point.getCoordinate().x, point.getCoordinate().y);
			String name = (String) feature.getAttribute(4);
			double capacity = (Double) feature.getAttribute(6);
			
			// check whether coordinate is inside the area covered by the network
			if (coord.getX() < minX || coord.getX() > maxX || coord.getY() < minY || coord.getY() > maxY) continue;
			
			if (capacity <= 0) continue;
						
			Link link = NetworkUtils.getNearestRightEntryLink(network, coord);
			Id<ActivityFacility> facilityId = Id.create(ParkingTypes.GARAGEPARKING + "_" + nf.format(counter.getCounter()), ActivityFacility.class);
			counter.incCounter();
			ActivityFacility parkingFacility = createAndAddParkingFacility(scenario, coord, facilityId, link.getId());
			((ActivityFacilityImpl) parkingFacility).setDesc(name);
			ActivityOption activityOption = scenario.getActivityFacilities().getFactory().createActivityOption(ParkingTypes.GARAGEPARKING);
			activityOption.setCapacity(capacity);
			activityOption.setFacility(parkingFacility);
			parkingFacility.addActivityOption(activityOption);
			
			if (capacity > 0) {
				// so far: just assume that 10% of the parking capacity is available as waiting capacity
				int waitingCapacity = (int) ((capacity) / 10);
				
				activityOption = scenario.getActivityFacilities().getFactory().createActivityOption(ParkingFacility.WAITING);
				activityOption.setCapacity(waitingCapacity);
			}
		}
		counter.printCounter();
	}
	
	// Strassenparkplaetze - each feature equals a single parking space
	private void createStreetParking(Scenario scenario) {
		
		// <LinkId, ParkingFacility on Link>
		Map<Id<Link>, ActivityFacility> linkParkingFacilities = new HashMap<>();
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumIntegerDigits(5);
		nf.setGroupingUsed(false);
		Counter counter = new Counter("# streetParking: ");
		
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(streetParkingFile);
		for (SimpleFeature feature : features) {
			Point point = (Point) feature.getAttribute(0);
			Coord coord = new Coord(point.getCoordinate().x, point.getCoordinate().y);
		
			// check whether coordinate is inside the area covered by the network
			if (coord.getX() < minX || coord.getX() > maxX || coord.getY() < minY || coord.getY() > maxY) continue;
						
			Link link = NetworkUtils.getNearestRightEntryLink(network, coord);
			
			ActivityFacility parkingFacility = linkParkingFacilities.get(link.getId());
			if (parkingFacility == null) {
				Id<ActivityFacility> facilityId = Id.create(ParkingTypes.STREETPARKING + "_" + nf.format(counter.getCounter()), ActivityFacility.class);
				counter.incCounter();
				parkingFacility = createAndAddParkingFacility(scenario, coord, facilityId, link.getId());
				((ActivityFacilityImpl) parkingFacility).setDesc(ParkingTypes.STREETPARKING);
				ActivityOption activityOption = scenario.getActivityFacilities().getFactory().createActivityOption(ParkingTypes.STREETPARKING);
				activityOption.setCapacity(1.0);
				activityOption.setFacility(parkingFacility);
				parkingFacility.addActivityOption(activityOption);
				linkParkingFacilities.put(link.getId(), parkingFacility);
			} else {
				ActivityOption activityOption = parkingFacility.getActivityOptions().get(ParkingTypes.STREETPARKING);
				activityOption.setCapacity(activityOption.getCapacity() + 1.0);
			}
		}
		counter.printCounter();
	}
	
	private void createOutsideCantonParking(Scenario scenario) {
		
		Counter counter = new Counter("# outside canton zurich links: ");
		
		GeometryFactory factory = new GeometryFactory();
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(cantonZurichFile);
		Set<SimpleFeature> set = new LinkedHashSet<SimpleFeature>(features);
		Geometry geometry = new SHPFileUtil().mergeGeometries(set);

		Set<String> parkingTypes = new LinkedHashSet<String>();
		parkingTypes.add(ParkingTypes.STREETPARKING);
		parkingTypes.add(ParkingTypes.GARAGEPARKING);
		parkingTypes.add(ParkingTypes.PRIVATEINSIDEPARKING);
		parkingTypes.add(ParkingTypes.PRIVATEOUTSIDEPARKING);
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			
			Coord coord = link.getCoord();
			Point point = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
			
			// skip links located inside canton Zurich
			if (geometry.contains(point)) continue;
			
			for (String parkingType : parkingTypes) {
				Id<ActivityFacility> facilityId = Id.create(parkingType + "_outside_" + link.getId().toString(), ActivityFacility.class);
				ActivityFacility parkingFacility = createAndAddParkingFacility(scenario, coord, facilityId, link.getId());
				((ActivityFacilityImpl) parkingFacility).setDesc(parkingType + "_for_link_" + link.getId().toString());
				ActivityOption activityOption = scenario.getActivityFacilities().getFactory().createActivityOption(parkingType);
				activityOption.setCapacity(1000000);
				activityOption.setFacility(parkingFacility);
				parkingFacility.addActivityOption(activityOption);				
			}
			counter.incCounter();
		}
		counter.printCounter();
	}
	
	private ActivityFacility createAndAddParkingFacility(Scenario scenario, Coord coord, Id facilityId, Id linkId) {
		ActivityFacility facility = scenario.getActivityFacilities().getFactory().createActivityFacility(facilityId, coord);
		((ActivityFacilityImpl) facility).setLinkId(linkId);
		scenario.getActivityFacilities().addActivityFacility(facility);
		return facility;
	}
}