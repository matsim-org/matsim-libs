/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.juliakern.toi;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.benjamin.utils.NetworkSimplifier;

public class ShapeConverterNetwork {

	static String shapeFile = "input/oslo/Matsim_files_1/trondheim_med_omland_4.shp";
	private static String networkfile = "input/oslo/trondheim_network_with_lanes_simple_V2.xml";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Logger logger = Logger.getLogger(ShapeConverterNetwork.class);
		
		Config config = new Config();
		config.addCoreModules();
		Controler controler = new Controler(config);
		Scenario scenario = controler.getScenario();

		ShapeFileReader sfr = new ShapeFileReader();
		Collection<SimpleFeature> features = sfr.readFileAndInitialize(shapeFile);
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		//Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(0.0, 10000.0));
		
		for(SimpleFeature sf: features){
			if(sf.getFeatureType() instanceof SimpleFeatureTypeImpl){
				//SimpleFeatureTypeImpl http://www.opengis.net/gml:trondheim_med_omland_4 identified extends lineFeature(the_geom:MultiLineString,
				//FNODE_:FNODE_,TNODE_:TNODE_,LPOLY_:LPOLY_,RPOLY_:RPOLY_,LENGTH:LENGTH,
				//VEGNETT_:VEGNETT_,VEGNETT_ID:VEGNETT_ID,OPPR:OPPR,KOORDH:KOORDH,
				//LTEMA:LTEMA,ADRS1:ADRS1,ADRS1F:ADRS1F,ADRS1T:ADRS1T,ADRS2:ADRS2,ADRS2F:ADRS2F,ADRS2T:ADRS2T,
				//MEDIUM:MEDIUM,KOMM:KOMM,TRANSID:TRANSID,GATE:GATE,MAALEMETOD:MAALEMETOD,NOEYAKTIGH:NOEYAKTIGH,
				//GATENAVN:GATENAVN,VEGTYPE:VEGTYPE,VEGSTATUS:VEGSTATUS,VEGNUMMER:VEGNUMMER,
				//HOVEDPARSE:HOVEDPARSE,METER_FRA:METER_FRA,METER_TIL:METER_TIL,VKJORFLT:VKJORFLT,VFRADATO:VFRADATO,
				//DATO:DATO,AKSEL_SO:AKSEL_SO,AKSEL_VI:AKSEL_VI,AKSEL_TEL:AKSEL_TEL,LENGDE:LENGDE,TOTVEKT:TOTVEKT,
				//FARTSGRENS:FARTSGRENS,HOYDE:HOYDE,ONEWAY:ONEWAY,SPERRING:SPERRING,DRIVETIME:DRIVETIME,
				//KOMMTRANSI:KOMMTRANSI,FYLKE:FYLKE,KOMMUNE:KOMMUNE,X_fra:X_fra,Y_fra:Y_fra,X_til:X_til,Y_til:Y_til)
//				System.out.println(sf.getAttribute("FNODE_"));
//				System.out.println(sf.getAttribute("KOORDH"));
				
				// create from node (if it doesnt exist yet)
				Double fromNodeX = (Double) sf.getAttribute("X_fra");
				Double fromNodeY = (Double) sf.getAttribute("Y_fra");
				Coord fromCoord = new Coord(fromNodeX, fromNodeY);
				Long fromNodeLong = (Long) sf.getAttribute("FNODE_");
				String fromNode = Long.toString(fromNodeLong);
				Node node1;
				
				if(!network.getNodes().containsKey(Id.create(fromNode, Node.class))){
					node1 = network.createAndAddNode(Id.create(fromNode, Node.class), fromCoord);
				}else{
					node1=network.getNodes().get(Id.create(fromNode, Node.class));
					
				}
				
				// create to node (if it does not exist yet)
				Double toNodeX = (Double) sf.getAttribute("X_til");
				Double toNodeY = (Double) sf.getAttribute("Y_til");
				Coord toCoord = new Coord(toNodeX, toNodeY);
				Long toNodeLong = (Long) sf.getAttribute("TNODE_");
				String toNode = Long.toString(toNodeLong);
				Node node2;
				
				if(!network.getNodes().containsKey(Id.create(toNode, Node.class))){
					node2 = network.createAndAddNode(Id.create(toNode, Node.class), toCoord);
				}else{
					node2 = network.getNodes().get(Id.create(toNode, Node.class));
					
				}
				
				// create link
				//network.createAndAddLink(scenario.createId("12"), node1, node2, 1000, 30.00, 3600, 1, null, "22");
				//network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, numLanes);
				//network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, numLanes, origId, type);
				Id<Link> linkId1 = Id.create(fromCoord+"_"+toCoord, Link.class);
				Id<Link> linkId2 = Id.create(toCoord+"_"+fromCoord, Link.class);
				if(node1.equals(node2)){
					logger.warn("nodes equal");
				}
				
				Double linkLength = (Double) sf.getAttribute("LENGTH");
				Integer freeSpeedkmh = (Integer) sf.getAttribute("FARTSGRENS"); // tempo limit //TODO change to m/sec?
				Double freeSpeed = freeSpeedkmh.doubleValue(); 
				String lanetype = (String) sf.getAttribute("VKJORFLT");
				
				LaneType laneType = new LaneType(lanetype);
				
					Double numLanesForwards = laneType.getNumberOfForwardLanes();
					Double numLanesBackwards = laneType.getNumberOfBackLanes();
					Double capacityf = numLanesForwards * freeSpeed *29; // assume 2000 in 1 hour for freespeed 50 km/h
					Double capacityb = numLanesBackwards * freeSpeed * 29;
					if (!network.getLinks().containsKey(linkId1)) {
						if(numLanesForwards>0.0){
							network.createAndAddLink(linkId1, node1, node2,	linkLength, freeSpeed, capacityf,numLanesForwards);
						}
					}
					if (!network.getLinks().containsKey(linkId2)) {
						if(numLanesBackwards>0.0){
							network.createAndAddLink(linkId2, node2, node1,	linkLength, freeSpeed, capacityb,numLanesBackwards);
						}
					}
					
					
					
					if(!(numLanesBackwards+numLanesForwards>=1.0)){
						logger.warn("no lanes for link " + lanetype);
					}
				}
		
		
		}
		
		
		// network cleaner - find biggest cluster, throw everything else away. removes unreachable nodes, links etc.
		 new NetworkCleaner().run(network);
		 // merges links with same parameters if they meet at a node (with no other node attached)
		 NetworkSimplifier ns = new NetworkSimplifier();
		 Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		 nodeTypesToMerge.add(4); // nodes of type "path 1 way"  -->node-->
		 nodeTypesToMerge.add(5); // nodes of type "path 2 ways" <-->node<-->
		 ns.setNodesToMerge(nodeTypesToMerge);
		 ns.run(network);
		 
		 logger.info("The cleaned and simplified network has " + network.getNodes().size() + " nodes and " + network.getLinks().size() + " links.");
		 NetworkWriter nw = new NetworkWriter(network); // write network into xml file
		 nw.write(networkfile );
		
		
	
	}
}

