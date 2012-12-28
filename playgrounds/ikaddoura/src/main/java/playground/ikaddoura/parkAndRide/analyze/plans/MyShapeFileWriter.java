/* *********************.************************************************** *
 * project: org.matsim.*b
 * MyShapeFileWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.ikaddoura.parkAndRide.analyze.plans;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author Ihab
 *
 */
public class MyShapeFileWriter {
	
	private GeometryFactory geometryFactory = new GeometryFactory();
	ArrayList<SimpleFeature> FeatureList = new ArrayList<SimpleFeature>();
	
	public void writeShapeFileLines(Scenario scenario, String path, String outputFile) {
		File directory = new File(path);
		directory.mkdirs();
		
		String file = path + outputFile;
		
		PolylineFeatureFactory factory = initFeatureType1();
		Collection<SimpleFeature> features = createFeatures1(scenario, factory);
		ShapeFileWriter.writeGeometries(features, file);
		System.out.println("ShapeFile " + file + " written.");	
	}
	
	public void writeShapeFilePoints(Scenario scenario, SortedMap<Id,Coord> koordinaten, String outputFile) {
		if (koordinaten.isEmpty() == true){
			System.out.println("Map is empty, shapeFile " + outputFile + " not written!");
		}
		else {
			PointFeatureFactory factory = initFeatureType2();
			Collection<SimpleFeature> features = createFeatures2(scenario, koordinaten, factory);
			ShapeFileWriter.writeGeometries(features,  outputFile);
			System.out.println("ShapeFile " + outputFile + " written.");	
		}
	}
	
	public void writeShapeFileGeometry(Map<Integer, Geometry> nr2geometry, Map<Integer, Double> nr2PRUsersHomeShare, Map<Integer, Double> zoneNr2activityShare_work, Map<Integer, Integer> zoneNr2home_prUsers, Map<Integer, Integer> zoneNr2work_prUsers, Map<Integer, Integer> zoneNr2home_all, Map<Integer, Integer> zoneNr2work_all, String outputFile) {
		SimpleFeatureBuilder factory = initFeatureType4();
		Collection<SimpleFeature> features = createFeatures4(nr2geometry, nr2PRUsersHomeShare, zoneNr2activityShare_work, zoneNr2home_prUsers, zoneNr2work_prUsers, zoneNr2home_all, zoneNr2work_all, factory);
		ShapeFileWriter.writeGeometries(features, outputFile);
		System.out.println("ShapeFile " + outputFile + " written.");	
	}
	
	public void writeShapeFilePRUsage(Scenario scenario, Map<Id, ParkAndRideFacility> id2prFacilities, Map<Id, Integer> prLinkId2prActs, String outputFile) {
		if (prLinkId2prActs.isEmpty() == true){
			System.out.println("Map is empty, shapeFile " + outputFile + " not written!");
		}
		
		PolylineFeatureFactory factory = initFeatureType3();
		Collection<SimpleFeature> features = createFeatures3(scenario, id2prFacilities, prLinkId2prActs, factory);
		ShapeFileWriter.writeGeometries(features, outputFile);
		System.out.println("ShapeFile " + outputFile + " written.");	
	}

	private PolylineFeatureFactory initFeatureType1() {
		return new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
				setName("link").
				addAttribute("ID", String.class).create();
	}
	
	private PointFeatureFactory initFeatureType2() {
		return new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
				setName("point").
				addAttribute("PersonId", String.class).
				create();
	}
	
	private PolylineFeatureFactory initFeatureType3() {
		return new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
				setName("link").
				addAttribute("ID", String.class).
				addAttribute("Stop", String.class).
				addAttribute("Users", Integer.class).
				create();
	}
	
	private SimpleFeatureBuilder initFeatureType4() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		b.setName("geometry");
		b.add("location", Geometry.class);
		b.add("NR", String.class);
		b.add("HomeAll", Integer.class);
		b.add("WorkAll", Integer.class);
		b.add("HomePRUsers", Integer.class);
		b.add("WorkPRUsers", Integer.class);
		b.add("HomePRshare", Double.class);
		b.add("WorkPRshare", Double.class);
		return new SimpleFeatureBuilder(b.buildFeatureType());
	}
	
	private Collection<SimpleFeature> createFeatures1(Scenario scenario, PolylineFeatureFactory factory) {
		ArrayList<SimpleFeature> liste = new ArrayList<SimpleFeature>();
		for (Link link : scenario.getNetwork().getLinks().values()){
			liste.add(getFeature1(link, factory));
		}
		return liste;
	}
	
	private Collection<SimpleFeature> createFeatures2(Scenario scenario, SortedMap<Id,Coord> Koordinaten, PointFeatureFactory factory) {
		ArrayList<SimpleFeature> liste = new ArrayList<SimpleFeature>();
		for (Entry<Id,Coord> entry : Koordinaten.entrySet()){
			liste.add(getFeature2((Coord)entry.getValue(), (Id)entry.getKey(), factory));
		}
		return liste;
	}
	
	private Collection<SimpleFeature> createFeatures3(Scenario scenario, Map<Id, ParkAndRideFacility> id2prFacilities, Map<Id, Integer> prLinkId2prActs, PolylineFeatureFactory factory) {
		ArrayList<SimpleFeature> liste = new ArrayList<SimpleFeature>();
		for (Id linkId : prLinkId2prActs.keySet()){
			
			String name = "";
			for (ParkAndRideFacility pr : id2prFacilities.values()){
				if (pr.getPrLink3in().equals(linkId)){
					name = pr.getStopFacilityName();
				}
			}
			
			liste.add(getFeature3(scenario.getNetwork().getLinks().get(linkId), name, prLinkId2prActs.get(linkId), factory));
		}
		return liste;
	}
	
	private Collection<SimpleFeature> createFeatures4(Map<Integer, Geometry> nr2geometry, Map<Integer, Double> nr2PRUsersHomeShare, Map<Integer, Double> zoneNr2activityShare_work, Map<Integer, Integer> zoneNr2home_prUsers, Map<Integer, Integer> zoneNr2work_prUsers, Map<Integer, Integer> zoneNr2home_all, Map<Integer, Integer> zoneNr2work_all, SimpleFeatureBuilder factory) {
		ArrayList<SimpleFeature> liste = new ArrayList<SimpleFeature>();
		for (Integer nr : nr2geometry.keySet()){
			liste.add(getFeature4(nr, nr2geometry.get(nr), nr2PRUsersHomeShare, zoneNr2activityShare_work, zoneNr2home_prUsers, zoneNr2work_prUsers, zoneNr2home_all, zoneNr2work_all, factory));
		}
		return liste;
	}

	private SimpleFeature getFeature1(Link link, PolylineFeatureFactory factory) {
		return factory.createPolyline(
				new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())}, 
				new Object[] {link.getId().toString()}, 
				null);
	}
	
	private SimpleFeature getFeature4(Integer nr, Geometry geometry, Map<Integer, Double> zoneNr2activityShare_home, Map<Integer, Double> zoneNr2activityShare_work, Map<Integer, Integer> zoneNr2home_prUsers, Map<Integer, Integer> zoneNr2work_prUsers, Map<Integer, Integer> zoneNr2home_all, Map<Integer, Integer> zoneNr2work_all, SimpleFeatureBuilder factory) {
		Geometry g = this.geometryFactory.createGeometry(geometry);
		Object [] attribs = new Object[8];
		attribs[0] = g;
		attribs[1] = String.valueOf(nr);
		if (zoneNr2home_all.containsKey(nr)){
			attribs[2] = zoneNr2home_all.get(nr);
		} else {
			attribs[2] = 0;
		}
		if (zoneNr2work_all.containsKey(nr)){
			attribs[3] = zoneNr2work_all.get(nr);
		} else {
			attribs[3] = 0;
		}
		if (zoneNr2home_prUsers.containsKey(nr)){
			attribs[4] = zoneNr2home_prUsers.get(nr);
		} else {
			attribs[4] = 0;
		}
		if (zoneNr2work_prUsers.containsKey(nr)){
			attribs[5] = zoneNr2work_prUsers.get(nr);
		} else {
			attribs[5] = 0;
		}
		if (zoneNr2activityShare_home.containsKey(nr)){
			attribs[6] = zoneNr2activityShare_home.get(nr);
		} else {
			attribs[6] = 0;
		}
		if (zoneNr2activityShare_work.containsKey(nr)){
			attribs[7] = zoneNr2activityShare_work.get(nr);
		} else {
			attribs[7] = 0;
		}

		return factory.buildFeature(null, attribs);
	}
	
	private SimpleFeature getFeature2(Coord coord, Id id, PointFeatureFactory factory) {
		return factory.createPoint(coord, new Object[] {id.toString()}, null);
	}
	
	private SimpleFeature getFeature3(Link link, String transitStopName, Integer prUsers, PolylineFeatureFactory factory) {
		return factory.createPolyline(
				new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())}, 
				new Object[] {link.getId().toString(), transitStopName, prUsers},
				null);
	}
}
