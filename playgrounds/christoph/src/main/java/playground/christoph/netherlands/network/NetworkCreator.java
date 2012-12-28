/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCreator.java
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

package playground.christoph.netherlands.network;

import java.util.ArrayList;
import java.util.Collection;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.counts.Counts;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

public class NetworkCreator {

	private static final Logger log = Logger.getLogger(NetworkCreator.class);
	
	private static String osmInFile = "../../matsim/mysimulations/netherlands/network/netherlands-bigroads.osm";
	private static String matsimOutFile = "../../matsim/mysimulations/netherlands/network/network.xml.gz";
	private static String matsimWGS84OutFile = "../../matsim/mysimulations/netherlands/network/network_WGS84.xml.gz";
	private static String kmzOutFile = "../../matsim/mysimulations/netherlands/network/network.kmz";
	private static String shpLinksOutFile = "../../matsim/mysimulations/netherlands/network/links.shp";
	private static String shpNodesOutFile = "../../matsim/mysimulations/netherlands/network/nodes.shp";
	
	private static boolean useWGS84 = true;
	
	public static void main(String[] args) throws Exception {

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();

		CoordinateTransformation ct;
		if (useWGS84) ct = new IdentityTransformation();
		else ct = new GeotoolsTransformation("WGS84", "EPSG:28992");
		
		OsmNetworkReader onr = new OsmNetworkReader(network, ct);
		onr.parse(osmInFile); 
		
		for (int i = 0; i < 5; i++) {
			log.info("cleaning network ...");
			NetworkCleaner nwCleaner = new NetworkCleaner();
			nwCleaner.run(network);
			log.info(" ... done.");
			
			log.info("thinning network ...");
			new NetworkThinner().run((NetworkImpl) network, new Counts());
			log.info(" ... done.");
		}
		
		log.info("cleaning network ...");
		NetworkCleaner nwCleaner = new NetworkCleaner();
		nwCleaner.run(network);
		log.info(" ... done.");
		
		log.info("writing network ...");
		if (useWGS84) new NetworkWriter(network).write(matsimWGS84OutFile);
		else new NetworkWriter(network).write(matsimOutFile);
		log.info(" ... done.");
		
		log.info("writing kmz network ...");
		ObjectFactory kmlObjectFactory = new ObjectFactory();
		KMZWriter kmzWriter = new KMZWriter(kmzOutFile);
		
		KmlType mainKml = kmlObjectFactory.createKmlType();
		DocumentType mainDoc = kmlObjectFactory.createDocumentType();
		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		
		KmlNetworkWriter kmlNetworkWriter;
		if (useWGS84) kmlNetworkWriter = new KmlNetworkWriter(network, new IdentityTransformation(), kmzWriter, mainDoc);
		else kmlNetworkWriter = new KmlNetworkWriter(network, new GeotoolsTransformation("EPSG:28992", "WGS84"), kmzWriter, mainDoc);
		
		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(kmlNetworkWriter.getNetworkFolder()));
		kmzWriter.writeMainKml(mainKml);
		kmzWriter.close();
		log.info("... done.");
		
		log.info("writing shape files...");
		Collection<SimpleFeature> ft;
		ft = generateNodesFromNet(network);
		ShapeFileWriter.writeGeometries(ft, shpNodesOutFile);
			
		ft = generateLinksFromNet(network);
		ShapeFileWriter.writeGeometries(ft, shpLinksOutFile);
		log.info("... done.");
	}

	public static Collection<SimpleFeature> generateLinksFromNet(Network network) {

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder().
				setCrs(crs).
				setName("link").
				addAttribute("ID", String.class).
				addAttribute("fromID", String.class).
				addAttribute("toID", String.class).
				addAttribute("length", Double.class).
				addAttribute("type", String.class).
				create();
		
		for (Link link : network.getLinks().values()) {		
			SimpleFeature ft = factory.createPolyline(new Coordinate[] {coord2Coordinate(link.getFromNode().getCoord()), coord2Coordinate(link.getCoord()), coord2Coordinate(link.getToNode().getCoord())}, new Object[] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(),link.getLength(), ((LinkImpl)link).getType()}, link.getId().toString());
			features.add(ft);
		}
				
		return features;
	}
	
	public static Collection<SimpleFeature> generateNodesFromNet(Network network) {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;

		PointFeatureFactory factory = new PointFeatureFactory.Builder().
				setCrs(crs).
				setName("nodes").
				addAttribute("ID", String.class).
				create();
		
		for (Node node : network.getNodes().values()) {
			SimpleFeature ft = factory.createPoint(node.getCoord(), new Object[] {node.getId().toString()}, node.getId().toString());
			features.add(ft);
		}
		
		return features;
	}
	
	/**
	 * Converts a MATSim {@link org.matsim.api.core.v01.Coord} into a Geotools <code>Coordinate</code>
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	private static Coordinate coord2Coordinate(final Coord coord) {
		return new Coordinate(coord.getX(), coord.getY());
	}
}
