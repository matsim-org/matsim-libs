/* *********************************************************************** *
 * project: org.matsim.*
 * UpdateNetworkSpeeds.java
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

package playground.christoph.agglobern;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/*
 * Update the allowed free speeds on a link depending on
 * the country to which the link belongs to.
 */
public class UpdateNetworkSpeeds {
	
	final private static Logger log = Logger.getLogger(UpdateNetworkSpeeds.class);
	
	private CoordinateTransformation transform;
	private GeometryFactory factory;
	private Feature france;
	private Feature germany;
	private Feature switzerland;

	private String shapeFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/Europa_Grenzen/DE_FR_CH_Grenze_GSC_WGS84.shp";
	private String networkFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/network.xml";
	private String outFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/network_updated.xml";
	private String outSHPNodesFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/network_nodes.shp";
	private String outSHPLinksFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/network_links.shp";
	private String outKMZFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/network.kmz";
	
	private String linksToCorrectFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/ways_with_default_values.txt";
	private String separator = ",";
	private Charset charset = Charset.forName("ISO-8859-1");
//	private Charset charset = Charset.forName("UTF-8");
	
	
	public static void main(String[] args) throws Exception {
		new UpdateNetworkSpeeds(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
	}
	
	public UpdateNetworkSpeeds(Scenario scenario) throws Exception {
		init();
		
//		log.info("Read Countries Shape File...");
//		readSHPFile();
//		log.info("done.");
		
		log.info("Read Network File...");
		readNetworkFile(scenario);
		log.info("done.");
		
		log.info("Cleaning Network...");
		cleanNetwork(scenario.getNetwork());
		log.info("done.");
		
//		log.info("Updating FreeSpeeds in Network depending on the Country the links belong to...");
//		updateFreeSpeeds(scenario.getNetwork());
//		log.info("done.");
		
		log.info("Correcting Free Speeds on non-Switzerland links...");
		correctFreeSpeeds(scenario.getNetwork());
		log.info("done.");
		
		log.info("Write Network File...");
		writeNetworkFile(scenario.getNetwork());
		log.info("done.");
				
		log.info("Write SHP Network Files...");
		writeSHPNetwork(scenario.getNetwork());
		log.info("done.");
		
		log.info("Write KMZ Network File...");
		writeKMZNetwork(scenario.getNetwork());
		log.info("done.");
	}
	
	private void init() {	
		transform = TransformationFactory.getCoordinateTransformation(TransformationFactory.CH1903_LV03_GT, TransformationFactory.WGS84);
		factory = new GeometryFactory();
	}
	
	private void readSHPFile() throws IOException {
		FeatureSource featureSource = ShapeFileReader.readDataFile(shapeFile);
		for (Object o : featureSource.getFeatures()) {
			Feature country = (Feature) o;
			if (((String)country.getAttribute("NAME")).equals("France")) france = country;
			else if (((String)country.getAttribute("NAME")).equals("Germany")) germany = country;
			else if (((String)country.getAttribute("NAME")).equals("Switzerland")) switzerland = country;
			else log.error("Unknown Country: " + country.getAttribute("NAME"));
		}
	}
	
	private void readNetworkFile(Scenario scenario) {
		new MatsimNetworkReader(scenario).readFile(networkFile);
	}
	
	private void cleanNetwork(Network network) {
		new NetworkCleaner().run(network);
	}
	
	private void writeNetworkFile(Network network) {
		new NetworkWriter(network).write(outFile);
	}
	
	private void correctFreeSpeeds(Network network) throws IOException {		
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
  		
		fis = new FileInputStream(linksToCorrectFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		
		// skip first Line
		br.readLine();
		
		Map<String, LinkToCorrect> linksToCorrect = new HashMap<String, LinkToCorrect>();
		String line;
		while((line = br.readLine()) != null) {
			LinkToCorrect linkToCorrect = new LinkToCorrect();
			
			String[] cols = line.split(separator);
			
			linkToCorrect.origid = cols[0];
			linkToCorrect.type = cols[1];
			linkToCorrect.country = cols[2];
			
			linksToCorrect.put(linkToCorrect.origid, linkToCorrect);
		}
				
		br.close();
		isr.close();
		fis.close();
		
		int count = 0;
		for (Link link : network.getLinks().values()) {
			LinkToCorrect linkToCorrect = linksToCorrect.get(((LinkImpl)link).getOrigId());
			if (linkToCorrect != null) {
				if (linkToCorrect.country.equals("germany")) {
					if (linkToCorrect.type.equals("motorway")) link.setFreespeed(130.0/3.6);
					else if (linkToCorrect.type.equals("trunk")) link.setFreespeed(100.0/3.6);
					else if (linkToCorrect.type.equals("primary")) link.setFreespeed(100.0/3.6);
					else if (linkToCorrect.type.equals("secondary")) link.setFreespeed(70.0/3.6);
					else log.error ("Uknown link type: " + linkToCorrect.type);				
				} else if (linkToCorrect.country.equals("france")) {
					if (linkToCorrect.type.equals("motorway")) link.setFreespeed(130.0/3.6);
					else if (linkToCorrect.type.equals("trunk")) link.setFreespeed(90.0/3.6);
					else if (linkToCorrect.type.equals("primary")) link.setFreespeed(90.0/3.6);
					else if (linkToCorrect.type.equals("secondary")) link.setFreespeed(70.0/3.6);
					else log.error ("Uknown link type: " + linkToCorrect.type);
				} else log.error("Unkown country: " + linkToCorrect.country);
				count++;
			}
		}
		log.info("Updated Links: " + count);
	}
	
	private void writeSHPNetwork(Network network) throws Exception {		
		GeometryFactory geoFac = new GeometryFactory();
		Collection<Feature> features = new ArrayList<Feature>();
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
		
		// Links
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("LineString", Geometry.class, true, null, null, crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", String.class);
		AttributeType fromNode = AttributeTypeFactory.newAttributeType("fromID", String.class);
		AttributeType toNode = AttributeTypeFactory.newAttributeType("toID", String.class);
		AttributeType length = AttributeTypeFactory.newAttributeType("length", Double.class);
		AttributeType type = AttributeTypeFactory.newAttributeType("type", String.class);
		FeatureType ftRoad = FeatureTypeBuilder.newFeatureType(new AttributeType[] {geom, id, fromNode, toNode, length, type}, "link");
		
		for (Link link : network.getLinks().values()) {		
			LineString ls = new LineString(new CoordinateArraySequence(new Coordinate [] {coord2Coordinate(link.getFromNode().getCoord()), coord2Coordinate(link.getCoord()), coord2Coordinate(link.getToNode().getCoord())}), geoFac);
			Feature ft = ftRoad.create(new Object [] {ls , link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(),link.getLength(), ((LinkImpl)link).getType()}, "links");
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, outSHPLinksFile);
		
		// Nodes
		features.clear();
		geom = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, crs);
		id = AttributeTypeFactory.newAttributeType("ID", String.class);
		FeatureType ftNode = FeatureTypeBuilder.newFeatureType(new AttributeType[] {geom, id}, "node");
		
		for (Node node : network.getNodes().values())
		{
			Point point = geoFac.createPoint(coord2Coordinate(node.getCoord()));

			Feature ft = ftNode.create(new Object[] {point, node.getId().toString()}, "nodes");
			features.add(ft);
		}

		ShapeFileWriter.writeGeometries(features, outSHPNodesFile);
	}
		
	/**
	 * Converts a MATSim {@link org.matsim.api.core.v01.Coord} into a Geotools <code>Coordinate</code>
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	private Coordinate coord2Coordinate(final Coord coord) {
		return new Coordinate(coord.getX(), coord.getY());
	}
	
	private void writeKMZNetwork(Network network) throws IOException{
		ObjectFactory kmlObjectFactory = new ObjectFactory();
		KMZWriter kmzWriter = new KMZWriter(outKMZFile);
	
		KmlType mainKml = kmlObjectFactory.createKmlType();
		DocumentType mainDoc = kmlObjectFactory.createDocumentType();
		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		
		KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(network, new GeotoolsTransformation(TransformationFactory.CH1903_LV03_GT, TransformationFactory.WGS84), kmzWriter, mainDoc);
	
		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(kmlNetworkWriter.getNetworkFolder()));
		kmzWriter.writeMainKml(mainKml);
		kmzWriter.close();
	}
	
	private void updateFreeSpeeds(Network network) {
		int franceCount = 0;
		int germanyCount = 0;
		int switzerlandCount = 0;
		
		for (Link link : network.getLinks().values()) {
			
			Coord wgs84LinkCenter = transform.transform(link.getCoord());
			Point linkCenter = factory.createPoint(new Coordinate(wgs84LinkCenter.getX(), wgs84LinkCenter.getY()));
			
			if (france.getDefaultGeometry().contains(linkCenter)) {
				franceCount++;
			} else if (germany.getDefaultGeometry().contains(linkCenter)) {
				germanyCount++;
			} else if (switzerland.getDefaultGeometry().contains(linkCenter)) {
				switzerlandCount++;
			} else log.error ("Center of Link is not part of one of the Countries!");
		}
		
		log.info("Links in France:      " + franceCount);
		log.info("Links in Germany:     " + germanyCount);
		log.info("Links in Switzerland: " + switzerlandCount);
	}
	
	private static class LinkToCorrect {
		String origid;
		String type;
		String country;
	}
}
