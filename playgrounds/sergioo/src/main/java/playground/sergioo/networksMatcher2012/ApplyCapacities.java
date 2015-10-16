/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.sergioo.networksMatcher2012;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.sergioo.networksMatcher2012.gui.DoubleNetworkCapacitiesWindow;
import playground.sergioo.networksMatcher2012.kernel.CrossingMatchingStep;
import playground.sergioo.visualizer2D2012.LayersWindow;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class ApplyCapacities {

	//Constants

	public static final File MATCHINGS_FILE = new File("./data/matching/matchings.txt");
	
	//Attributes

	//Methods
	
	public static Network getNetworkFromShapeFileLength(String fileName) throws IOException {
		Map<Integer,Double> freeSpeedsMap = new HashMap<Integer, Double>(); 
		BufferedReader reader = new BufferedReader(new FileReader("./data/matching/freeSpeed/FreeSpeeds_X_Types.txt"));
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(":");
			freeSpeedsMap.put(Integer.parseInt(parts[0]), Double.parseDouble(parts[1]));
			line = reader.readLine();
		}
		reader.close();
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);
		Network network = NetworkUtils.createNetwork();
		NetworkFactory networkFactory = network.getFactory();
		for(SimpleFeature feature : features)
			if(feature.getFeatureType().getTypeName().equals("emme_links")) {
				Coordinate[] coords = ((Geometry) feature.getDefaultGeometry()).getCoordinates();
				Id<Node> idFromNode = Id.createNodeId((Long)feature.getAttribute("INODE"));
				Node fromNode = network.getNodes().get(idFromNode);
				if(fromNode==null) {
					fromNode = networkFactory.createNode(idFromNode, new Coord(coords[0].x, coords[0].y));
					network.addNode(fromNode);
				}
				Id<Node> idToNode = Id.createNodeId((Long)feature.getAttribute("JNODE"));
				Node toNode = network.getNodes().get(idToNode);
				if(toNode==null) {
					toNode = networkFactory.createNode(idToNode, new Coord(coords[coords.length - 1].x, coords[coords.length - 1].y));
					network.addNode(toNode);
				}
				Link link = network.getFactory().createLink(Id.createLinkId(idFromNode+"-->"+idToNode), fromNode, toNode);
				link.setCapacity((Double)feature.getAttribute("DATA2"));
				link.setFreespeed(freeSpeedsMap.get((Integer)feature.getAttribute("VDF"))/3.6);
				link.setNumberOfLanes((Double)feature.getAttribute("LANES"));
				link.setLength((Double)feature.getAttribute("LENGTH"));
				((LinkImpl)link).setOrigId(feature.getID());
				network.addLink(link);
			}
		return network;
	}
	
	public static Map<Link, Tuple<Link,Double>> loadCapacities(File file, Network networkA, Network networkB) {
		Map<Link, Tuple<Link,Double>> linksChanged = new HashMap<Link, Tuple<Link,Double>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(":::");
				linksChanged.put(networkB.getLinks().get(Id.createLinkId(parts[0])), new Tuple<Link,Double>(networkA.getLinks().get(Id.createLinkId(parts[1])), Double.parseDouble(parts[2])));
				line = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return linksChanged;
	}
	
	/**
	 * @param args 0 
	 *  0 - First network MATSim file ("./data/currentSimulation/singapore.xml")
	 *  1 - Second network shape file ("C:/Users/sergioo/Documents/2011/Work/FCL Old/Operations/Data/RoadCapacities/emme_links.shp")
	 *  2 - First network MATSim output file ("./data/currentSimulation/singapore2.xml")
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		Network networkLowResolution = getNetworkFromShapeFileLength(args[1]);
		Network networkHighResolution = scenario.getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84_UTM48N);
		for(Node node:networkLowResolution.getNodes().values())
			((NodeImpl)node).setCoord(coordinateTransformation.transform(node.getCoord()));
		CrossingMatchingStep.CAPACITIES_FILE = new File("./data/matching/capacities/linksChanged.txt");
		Map<Link, Tuple<Link,Double>> result = loadCapacities(CrossingMatchingStep.CAPACITIES_FILE, networkLowResolution, networkHighResolution);
		LayersWindow windowHR2 = new DoubleNetworkCapacitiesWindow("Networks capacities", networkLowResolution, networkHighResolution, result);
		windowHR2.setVisible(true);
		while(!windowHR2.isReadyToExit())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		for(Entry<Link, Tuple<Link,Double>> entry:result.entrySet()) {
			entry.getKey().setCapacity(entry.getValue().getFirst().getCapacity());
			entry.getKey().setFreespeed(entry.getValue().getFirst().getFreespeed());
		}
		new NetworkWriter(networkHighResolution).write(args[2]);
	}
	
}
