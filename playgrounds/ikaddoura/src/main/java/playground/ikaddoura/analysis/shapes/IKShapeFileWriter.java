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

package playground.ikaddoura.analysis.shapes;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.log4j.Logger;
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

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author Ihab
 *
 */
public class IKShapeFileWriter {
	private static final Logger log = Logger.getLogger(IKShapeFileWriter.class);
	
	ArrayList<SimpleFeature> FeatureList = new ArrayList<SimpleFeature>();
	
	public void writeShapeFileLines(Scenario scenario, String path, String outputFile) {
		File directory = new File(path);
		directory.mkdirs();
		
		String file = path + outputFile;
		
		PolylineFeatureFactory factory = initFeatureType1();
		Collection<SimpleFeature> features = createFeatures1(scenario, factory);
		ShapeFileWriter.writeGeometries(features, file);
		log.info("ShapeFile " + file + " written.");	
	}
	
	public void writeShapeFilePoints(Scenario scenario, SortedMap<Id,Coord> koordinaten, String outputFile) {
		if (koordinaten.isEmpty() == true){
			log.info("Map is empty, shapeFile " + outputFile + " not written!");
		}
		else {
			PointFeatureFactory factory = initFeatureType2();
			Collection<SimpleFeature> features = createFeatures2(scenario, koordinaten, factory);
			ShapeFileWriter.writeGeometries(features,  outputFile);
			log.info("ShapeFile " + outputFile + " written.");	
		}
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

	private SimpleFeature getFeature1(Link link, PolylineFeatureFactory factory) {
		return factory.createPolyline(
				new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())}, 
				new Object[] {link.getId().toString()}, 
				null);
	}
	
	private SimpleFeature getFeature2(Coord coord, Id id, PointFeatureFactory factory) {
		return factory.createPoint(coord, new Object[] {id.toString()}, null);
	}
	
}
