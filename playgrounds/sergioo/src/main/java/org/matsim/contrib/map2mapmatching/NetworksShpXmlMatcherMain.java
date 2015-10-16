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

package org.matsim.contrib.map2mapmatching;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.map2mapmatching.gui.DoubleNetworkMatchingWindow;
import org.matsim.contrib.map2mapmatching.gui.NetworkTwoNodesPainter;
import org.matsim.contrib.map2mapmatching.gui.core.LayersWindow;
import org.matsim.contrib.map2mapmatching.kernel.CrossingMatchingStep;
import org.matsim.contrib.map2mapmatching.kernel.InfiniteRegion;
import org.matsim.contrib.map2mapmatching.kernel.core.MatchingProcess;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class NetworksShpXmlMatcherMain {


	//Main

	/**
	 * @param args:
	 *  0 - First network MATSim file ("./data/currentSimulation/singapore.xml")
	 *  1 - Second network shape file ("C:/Users/sergioo/Documents/2011/Work/FCL Old/Operations/Data/RoadCapacities/emme_links.shp")
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		Network networkLowResolution = getNetworkFromShapeFileLength(args[1]);
		/*CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84_UTM48N);
		for(Node node:networkLowResolution.getNodes().values())
			((NodeImpl)node).setCoord(coordinateTransformation.transform(node.getCoord()));
		new NetworkWriter(networkLowResolution).write("./data/forPieter.xml");
		return;*/
		Network networkHighResolution = scenario.getNetwork();
		LayersWindow windowHR = new DoubleNetworkMatchingWindow("Networks matching", new NetworkTwoNodesPainter(networkHighResolution, Color.BLACK, new BasicStroke(), Color.CYAN), new NetworkTwoNodesPainter(networkLowResolution, Color.BLACK, new BasicStroke(), Color.CYAN));
		windowHR.setVisible(true);
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		MatchingProcess matchingProcess = new MatchingProcess(modes);
		matchingProcess.addMatchingStep(new CrossingMatchingStep(new InfiniteRegion(), 30, Math.PI/12));
		matchingProcess.execute(networkLowResolution, networkHighResolution);
		LayersWindow windowHR2 = new DoubleNetworkMatchingWindow("Networks reduced", matchingProcess);
		windowHR2.setVisible(true);
		while(!windowHR2.isReadyToExit())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		matchingProcess.applyProperties(false);
		System.out.println(networkHighResolution.getLinks().size()+" "+networkLowResolution.getLinks().size());
	}


	//Methods

	public static Network getNetworkFromShapeFilePolyline(String fileName) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);
		Network network = NetworkUtils.createNetwork();
		NetworkFactory networkFactory = network.getFactory();
		long nodeLongId=0, linkLongId=0;
		for(SimpleFeature feature:features)
			if(feature.getFeatureType().getTypeName().equals("emme_links")) {
				Coordinate[] coords = ((Geometry) feature.getDefaultGeometry()).getCoordinates();
				Node[] nodes = new Node[coords.length];
				for(int n=0; n<nodes.length; n++) {
					Coord coord = new Coord(coords[n].x, coords[n].y);
					for(Node node:network.getNodes().values())
						if(node.getCoord().equals(coord))
							nodes[n] = node;
					if(nodes[n]==null) {
						nodes[n] = networkFactory.createNode(Id.createNodeId(nodeLongId), coord);
						nodeLongId++;
						if(n==0)
							((NodeImpl)nodes[n]).setOrigId(feature.getAttribute("INODE").toString());
						else if(n==nodes.length-1)
							((NodeImpl)nodes[n]).setOrigId(feature.getAttribute("JNODE").toString());
					}
				}
				for(int n=0; n<nodes.length-1; n++) {
					if(network.getNodes().get(nodes[n].getId())==null)
						network.addNode(nodes[n]);
					Link link = network.getFactory().createLink(Id.createLinkId(linkLongId), nodes[n], nodes[n+1]);
					link.setCapacity((Double)feature.getAttribute("DATA2"));
					link.setNumberOfLanes((Double)feature.getAttribute("LANES"));
					((LinkImpl)link).setOrigId(feature.getID());
					network.addLink(link);
					linkLongId++;
				}
				if(network.getNodes().get(nodes[nodes.length-1].getId())==null)
					network.addNode(nodes[nodes.length-1]);
			}
		return network;
	}
	
	public static Network getNetworkFromShapeFileLength(String fileName) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(fileName);
		Network network = NetworkUtils.createNetwork();
		NetworkFactory networkFactory = network.getFactory();
		for(SimpleFeature feature:features)
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
				link.setNumberOfLanes((Double)feature.getAttribute("LANES"));
				link.setLength((Double)feature.getAttribute("LENGTH"));
				((LinkImpl)link).setOrigId(feature.getID());
				network.addLink(link);
			}
		return network;
	}

}
