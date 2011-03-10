/* *********************************************************************** *
 * project: org.matsim.*
 * CreateZoneConnectors.java
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

package playground.christoph.netherlands.zones;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vis.kml.KMZWriter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class CreateZoneConnectors {

	private static final Logger log = Logger.getLogger(CreateZoneConnectors.class);
	
	private static String networkInFile = "../../matsim/mysimulations/netherlands/network/network.xml.gz";
	private static String networkOutFile = "../../matsim/mysimulations/netherlands/network/network_with_connectors.xml.gz";
	private static String kmzOutFile = "../../matsim/mysimulations/netherlands/network/network_with_connectors.kmz";
	private static String shapeFile = "../../matsim/mysimulations/netherlands/zones/postcode4_org.shp";

	private double connectorLinkFreeSpeed = 40 / 3.6;	// 40 km/h
	private double connectorCapacity = 10000.0;
	
	private Scenario scenario;
	private Network network;
	
	private Set<Feature> zones;
	private Map<Integer, Feature> zonesMap;	// TAZ, Feature
	
	private GeotoolsTransformation ct = new GeotoolsTransformation("WGS84", "EPSG:28992");
	
	public static void main(String[] args) throws Exception {
		new CreateZoneConnectors();
	}
	
	public CreateZoneConnectors() throws Exception {
		
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkInFile);
		CoordinateTransformation coordinateTransformation = new IdentityTransformation();
						
		this.createMapping(coordinateTransformation);
	}
	
	public void createMapping(CoordinateTransformation coordinateTransformation) throws Exception {
		network = scenario.getNetwork();

		log.info("Loading Network ... done");
		log.info("Nodes: " + network.getNodes().size());
		log.info("Links: " + network.getLinks().size());
		
		/*
		 * read zones shape file
		 */
		zones = new HashSet<Feature>();

		FeatureSource featureSource = ShapeFileReader.readDataFile(shapeFile);
		for (Object o : featureSource.getFeatures()) {
			zones.add((Feature) o);
		}
	
		zonesMap = new TreeMap<Integer, Feature>();
		for (Feature zone : zones) {
//			int id = Integer.valueOf(zone.getID().replace("postcode4.", ""));	// Object Id
//			int id = ((Long)zone.getAttribute(1)).intValue();	// Zone Id
			int id = ((Long)zone.getAttribute(3)).intValue();	// PostCode
			zonesMap.put(id, zone);
		}
		
		/*
		 * print zones
		 */
		log.info("Using " + zones.size() + " zones.");

		log.info("Adding connector nodes and links...");
		addConnectorLinks();
		log.info("done.");
		
		log.info("writing network ...");
		new NetworkWriter(network).write(networkOutFile);
		log.info(" ... done.");
		
		log.info("writing kmz network ...");
		ObjectFactory kmlObjectFactory = new ObjectFactory();
		KMZWriter kmzWriter = new KMZWriter(kmzOutFile);
		
		KmlType mainKml = kmlObjectFactory.createKmlType();
		DocumentType mainDoc = kmlObjectFactory.createDocumentType();
		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		
		KmlNetworkWriter kmlNetworkWriter;
		kmlNetworkWriter = new KmlNetworkWriter(network, new GeotoolsTransformation("EPSG:28992", "WGS84"), kmzWriter, mainDoc);
		
		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(kmlNetworkWriter.getNetworkFolder()));
		kmzWriter.writeMainKml(mainKml);
		kmzWriter.close();
		log.info("... done.");
	}
	
	/*package*/ void addConnectorLinks() {
		
		QuadTree<Node> quadTree = getNodesQuadTree();		
		NetworkFactory factory = network.getFactory();
		
		/*
		 * Try to use the centroid of the polygon. If that does not lie within
		 * the polygon, use an interior point.
		 */
		for (Entry<Integer, Feature> entry : zonesMap.entrySet()) {
			int zoneId = entry.getKey();
			Feature zone = entry.getValue();
			
			if (SpecialZones.skipZone(zone)) continue;
			
			Geometry polygon = zone.getDefaultGeometry();
			Point point = polygon.getCentroid();
			if (!polygon.contains(point)) point = polygon.getInteriorPoint();
			
			/*
			 * Convert coordinate from WGS84 to EPSG:28992 (Netherlands Projection)
			 */
			Coord wgs84Coord = scenario.createCoord(point.getCoordinate().x, point.getCoordinate().y);
			Coord nodeCoord = ct.transform(wgs84Coord);
			
			Id nodeId = scenario.createId(String.valueOf(zoneId));
			network.addNode(factory.createNode(nodeId, nodeCoord));
			
			Node networkConnectorNode = quadTree.get(nodeCoord.getX(), nodeCoord.getY());
			Id networkConnectorNodeId = networkConnectorNode.getId();
			
			double length = CoordUtils.calcDistance(nodeCoord, networkConnectorNode.getCoord());
			
			Id linkFromId = scenario.createId(String.valueOf(zoneId) + "from");
			Link fromLink = factory.createLink(linkFromId, nodeId, networkConnectorNodeId);
			fromLink.setLength(length);
			fromLink.setCapacity(this.connectorCapacity);
			fromLink.setFreespeed(this.connectorLinkFreeSpeed);
			network.addLink(fromLink);
			
			Id linkToId = scenario.createId(String.valueOf(zoneId) + "to");
			Link toLink = factory.createLink(linkToId, networkConnectorNodeId, nodeId);
			toLink.setLength(length);
			toLink.setCapacity(this.connectorCapacity);
			toLink.setFreespeed(this.connectorLinkFreeSpeed);
			network.addLink(toLink);
		}
	}
	
	/*package*/ QuadTree<Node> getNodesQuadTree() {
		log.info("building nodes quad tree...");
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (Node node : network.getNodes().values()) {
			if (node.getCoord().getX() < minx) { minx = node.getCoord().getX(); }
			if (node.getCoord().getY() < miny) { miny = node.getCoord().getY(); }
			if (node.getCoord().getX() > maxx) { maxx = node.getCoord().getX(); }
			if (node.getCoord().getY() > maxy) { maxy = node.getCoord().getY(); }
		}
		
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		log.info("xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
				
		QuadTree<Node> quadTree = new QuadTree<Node>(minx, miny, maxx, maxy);
		
		log.info("filling nodes quad tree...");
		for (Node node : network.getNodes().values()) {
			quadTree.put(node.getCoord().getX(), node.getCoord().getY(), node);
		}
		
		log.info("done.");
		
		return quadTree;
	}
	
}