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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.network;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils.PatnaNetworkType;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
/**
 * @author amit
 */

public class PatnaNetworkFromTransCadDataGenerator {       

	private static final Logger LOG = Logger.getLogger(PatnaNetworkFromTransCadDataGenerator.class);
	private Scenario scenario;

	public static void main(String[] args) throws IOException  {  
		PatnaNetworkFromTransCadDataGenerator png =  new PatnaNetworkFromTransCadDataGenerator();
		png.startProcessingFile();
		String outNetwork = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/network/"+PatnaNetworkType.shpNetwork.toString()+"/network.xml.gz";
		new NetworkWriter(png.getPatnaNetwork()).write(outNetwork);
		LOG.info("The network file is written to - "+ outNetwork);
	}

	public void startProcessingFile() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());                                       
		final Network network = scenario.getNetwork();

		String inputFileNetwork = PatnaUtils.INPUT_FILES_DIR+"raw/network/networkInputTransCad.csv" ;

		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();       
		tabularFileParserConfig.setFileName(inputFileNetwork);
		tabularFileParserConfig.setDelimiterTags(new String[] {","});                               
		tabularFileParserConfig.setStartTag("linkId");											
		TabularFileHandler tabularFileHandler = new TabularFileHandler() {
			// ZZ_TODO : increase capacity of roundabout links.            
			@ Override
			public void startRow(String[] row) {

				String linkId = row[0];
				String widthOfRoad = row [3];
				String lengthInKm = row [6];
				String speedInKmph = row[7];
				String FromNodeId = row[8];							
				String ToNodeId = row[9];
				String fromNodeXCoord = row [10];				
				String fromNodeYCoord = row [12];
				String toNodeXCoord = row [11];
				String toNodeYCoord = row [13];

				Id<Node> fromNodeId = Id.create(FromNodeId,Node.class);
				Node fromNode;

				if (network.getNodes().containsKey(fromNodeId)) {     
					fromNode = network.getNodes().get(fromNodeId);
				}
				else {
					double xcord = (Double.parseDouble(fromNodeXCoord))/(1000000);				
					double ycord = (Double.parseDouble(fromNodeYCoord))/(1000000);
					Coord createCoord = new Coord(xcord, ycord);
					fromNode = network.getFactory().createNode(fromNodeId, PatnaUtils.COORDINATE_TRANSFORMATION.transform( createCoord));
					network.addNode(fromNode);
				}

				Id<Node> toNodeId = Id.create(ToNodeId,Node.class);
				Node toNode;
				if (network.getNodes().containsKey(toNodeId)) {     		   
					toNode = network.getNodes().get(toNodeId);
				}
				else {
					double xcord = (Double.parseDouble(toNodeXCoord))/(1000000);
					double ycord = (Double.parseDouble(toNodeYCoord))/(1000000);
					Coord createCoord = new Coord(xcord, ycord);
					toNode = network.getFactory().createNode(toNodeId, PatnaUtils.COORDINATE_TRANSFORMATION.transform(createCoord) );
					network.addNode(toNode);
				}

				Link link1 = network.getFactory().createLink(Id.create(linkId,Link.class), fromNode, toNode); 
				Link link2 = network.getFactory().createLink(Id.create(linkId + "10000",Link.class), toNode, fromNode);   

				double freeSpeed = 0;
				//				type of road is Arterial, sub arterial and collector so speeds are 50. 40 . 40kph respectively.
				//				in given data speed is stream speeds (25, 20 and 15) not free flow speed
				switch (speedInKmph){ 
				case "25" :	freeSpeed = 50; break;
				case "20" :	freeSpeed = 40; break;
				case "15" : 	freeSpeed = 40; break;
				case "50" :	freeSpeed = 60; break;
				default : throw new RuntimeException("The speed code "+speedInKmph+" is not recognized. Aborting...");
				}
				double freeSpeedInMPS = freeSpeed/3.6;	
				//				double freeSpeedInMPS = 60/3.6; 

				double roadWidth = 0.5*Double.parseDouble(widthOfRoad);			
				int numberoflanes = 0;

				if ( roadWidth < 4) numberoflanes = 1;								
				else if ( roadWidth <9 && roadWidth >= 4) numberoflanes =2;
				else if ( roadWidth <12 && roadWidth >= 9) numberoflanes =3;

				double linkLength = 1000 * Double.parseDouble(lengthInKm);
				double capacity = getCapacityOfLink(widthOfRoad);

				Set<String> allowedModes ; //AA_TODO : fix this
//				if (capacity <= 500.0) allowedModes = new HashSet<>(Arrays.asList("car","bike","motorbike","car_ext","motorbike_ext","bike_ext")); 
//				else
					allowedModes = new HashSet<>(PatnaUtils.ALL_MAIN_MODES);
				
				link1.setFreespeed(freeSpeedInMPS);
				link1.setCapacity(capacity);
				link1.setNumberOfLanes(numberoflanes);
				link1.setLength(linkLength);
				link1.setAllowedModes(new HashSet<>(allowedModes));
				network.addLink(link1);

				link2.setFreespeed(freeSpeedInMPS);
				link2.setCapacity(capacity);
				link2.setNumberOfLanes(numberoflanes);
				link2.setLength(linkLength);
				link2.setAllowedModes(new HashSet<>(allowedModes));
				network.addLink(link2);
			}
		};

		TabularFileParser tabularFileParser = new TabularFileParser();
		tabularFileParser.parse(tabularFileParserConfig, tabularFileHandler);  
		new NetworkCleaner().run(network);

		LOG.info("Number of links in the network are "+ network.getLinks().size()+" and number of nodes in the link are"+network.getNodes().size());

		NetworkSimplifier simplifier = new NetworkSimplifier();
		simplifier.run(network);

		// manual cleaning (filtering) of the network.
		removeLinksNodes();
		removeIsolatedNodes();
		modifyLinkCapacity();
		addNodesAndLinks();
		connectLinksDirectly(); 
	}    

	private void connectLinksDirectly() { 
		
		Network network = scenario.getNetwork();
		
		{// this connection with gandhi setu should not be present, thus making a link direct
			Node desiredNode = network.getNodes().get(Id.createNodeId("15057"));
			Link linkToBeRemoved = network.removeLink(Id.createLinkId("1878610000"));
			Link linkToBeRemoved_reverse = network.removeLink(Id.createLinkId("18786"));
			
			Link linkToModify = network.getLinks().get(Id.createLinkId("16787-16771-16778-16718"));
			Link linkToModify_reverse = network.getLinks().get(Id.createLinkId("1671810000-1677810000-1677110000-1678710000"));
			
			linkToModify.setFromNode(desiredNode);
			linkToModify_reverse.setToNode(desiredNode);
			
			linkToModify.setLength(linkToModify.getLength() + linkToBeRemoved.getLength());
			linkToModify_reverse.setLength(linkToModify_reverse.getLength() + linkToBeRemoved_reverse.getLength());	
		}
		{ //suddenly a small link with low capacity 
			network.removeLink(Id.createLinkId("18778"));
			network.removeLink(Id.createLinkId("1877810000"));
			
			Node n = network.getNodes().get(Id.createNodeId("10211"));
			network.getLinks().get(Id.createLinkId("1018010000-1017110000")).setToNode(n);
			network.getLinks().get(Id.createLinkId("10171-10180")).setFromNode(n);
			
			if(n.getInLinks().size()==0 && n.getOutLinks().size()==0 ) network.removeNode(n.getId()); // make sure no link is connected and then remove
		}
		{ // a junction, small link  (6m) become a bottleneck
			network.removeLink(Id.createLinkId("1674810000"));
			network.removeLink(Id.createLinkId("16748"));
			
			Node n = network.getNodes().get(Id.createNodeId("15009"));
			network.getLinks().get(Id.createLinkId("16787-16771-16778-16718")).setToNode(n);
			network.getLinks().get(Id.createLinkId("1671810000-1677810000-1677110000-1678710000")).setFromNode(n);
			
			network.getLinks().get(Id.createLinkId("1673110000-16768-1675710000-1676110000-1675510000-16754-16736-16751-1674910000-16750-16738")).setToNode(n);
			network.getLinks().get(Id.createLinkId("1673810000-1675010000-16749-1675110000-1673610000-1675410000-16755-16761-16757-1676810000-16731")).setFromNode(n);
			
			n = network.getNodes().get(Id.createNodeId("14974"));
			if(n.getInLinks().size()==0 && n.getOutLinks().size()==0 ) network.removeNode(n.getId()); // make sure no link is connected and then remove
		}
		{ // a small link  (3m) become a bottleneck
			network.removeLink(Id.createLinkId("1878710000"));
			network.removeLink(Id.createLinkId("18787"));
			
			Node n = network.getNodes().get(Id.createNodeId("16326"));
			network.getLinks().get(Id.createLinkId("16790")).setToNode(n);
			network.getLinks().get(Id.createLinkId("1679010000")).setFromNode(n);
			
			n = network.getNodes().get(Id.createNodeId("16318"));
			if(n.getInLinks().size()==0 && n.getOutLinks().size()==0 ) network.removeNode(n.getId()); // make sure no link is connected and then remove
		}
		{ // a small link  (6m) become a bottleneck
			network.removeLink(Id.createLinkId("17461"));
			network.removeLink(Id.createLinkId("1746110000"));
			
			Node n = network.getNodes().get(Id.createNodeId("15498"));
			network.getLinks().get(Id.createLinkId("1750110000-1742410000")).setToNode(n);
			network.getLinks().get(Id.createLinkId("17424-17501")).setFromNode(n);
			
			n = network.getNodes().get(Id.createNodeId("15468"));
			if(n.getInLinks().size()==0 && n.getOutLinks().size()==0 ) network.removeNode(n.getId()); // make sure no link is connected and then remove
		}
	}

	public Network getPatnaNetwork() {
		return scenario.getNetwork();
	}

	private double getCapacityOfLink (final String roadwidth) {
		double linkCapacity =0;
		double w = Double.parseDouble(roadwidth);
		double capacityCarrigway = -2184-22.6*Math.pow(w, 2)+857.4*w;  
		linkCapacity = 0.5*capacityCarrigway;
		if (linkCapacity < 300) linkCapacity = 300; 
		return Math.ceil( linkCapacity );
	}

	private void addNodesAndLinks(){
		Network network = scenario.getNetwork();
		// Adding links near to the counting station links (necessary for external demand)
		//OC4 -- using existing node (14653)
		Node oc4NearestNode = network.getNodes().get(Id.createNodeId("14653"));
		Coord oc4NodeCoord = new Coord(oc4NearestNode.getCoord().getX() + 500, oc4NearestNode.getCoord().getY() + 500);
		Node oc4Node = network.getFactory().createNode(Id.createNodeId("OC4_node"), oc4NodeCoord); network.addNode(oc4Node);
		final Node fromNode = oc4Node;
		final Node toNode = oc4NearestNode;
		NetworkUtils.createAndAddLink(((Network)network),Id.createLinkId("OC4_in"), fromNode, toNode, 500., 60./3.6, 1500., (double) 2 );
		final Node fromNode1 = oc4NearestNode;
		final Node toNode1 = oc4Node;
		NetworkUtils.createAndAddLink(((Network)network),Id.createLinkId("OC4_out"), fromNode1, toNode1, 500., 60./3.6, 1500., (double) 2 );
		network.getLinks().get(Id.createLinkId("OC4_in")).setAllowedModes(new HashSet<>(PatnaUtils.ALL_MAIN_MODES));
		network.getLinks().get(Id.createLinkId("OC4_out")).setAllowedModes(new HashSet<>(PatnaUtils.ALL_MAIN_MODES));
		
		
		//OC2 -- using existing node 16224
		Node oc2NearestNode = network.getNodes().get(Id.createNodeId("16224"));
		Coord oc2NodeCoord = new Coord(oc2NearestNode.getCoord().getX() + 500, oc2NearestNode.getCoord().getY() - 500);
		Node oc2Node = network.getFactory().createNode(Id.createNodeId("OC2_node"), oc2NodeCoord); network.addNode(oc2Node);
		final Node fromNode2 = oc2Node;
		final Node toNode2 = oc2NearestNode;
		NetworkUtils.createAndAddLink(((Network)network),Id.createLinkId("OC2_in"), fromNode2, toNode2, 500., 60./3.6, 1500., (double) 2 );
		final Node fromNode3 = oc2NearestNode;
		final Node toNode3 = oc2Node;
		NetworkUtils.createAndAddLink(((Network)network),Id.createLinkId("OC2_out"), fromNode3, toNode3, 500., 60./3.6, 1500., (double) 2 );
		network.getLinks().get(Id.createLinkId("OC2_in")).setAllowedModes(new HashSet<>(PatnaUtils.ALL_MAIN_MODES));
		network.getLinks().get(Id.createLinkId("OC2_out")).setAllowedModes(new HashSet<>(PatnaUtils.ALL_MAIN_MODES));
		
		//OC5
		Node oc5NearestNode = network.getNodes().get(Id.createNodeId("2426"));
		Coord oc5NodeCoord = new Coord(oc5NearestNode.getCoord().getX() - 500, oc5NearestNode.getCoord().getY() + 100);
		Node oc5Node = network.getFactory().createNode(Id.createNodeId("OC5_node"), oc5NodeCoord); network.addNode(oc5Node);
		final Node fromNode4 = oc5Node;
		final Node toNode4 = oc5NearestNode;
		NetworkUtils.createAndAddLink(((Network)network),Id.createLinkId("OC5_in"), fromNode4, toNode4, 500., 60./3.6, 1500., (double) 2 );
		final Node fromNode5 = oc5NearestNode;
		final Node toNode5 = oc5Node;
		NetworkUtils.createAndAddLink(((Network)network),Id.createLinkId("OC5_out"), fromNode5, toNode5, 500., 60./3.6, 1500., (double) 2 );
		network.getLinks().get(Id.createLinkId("OC5_in")).setAllowedModes(new HashSet<>(PatnaUtils.ALL_MAIN_MODES));
		network.getLinks().get(Id.createLinkId("OC5_out")).setAllowedModes(new HashSet<>(PatnaUtils.ALL_MAIN_MODES));
	}
	
	private void modifyLinkCapacity(){
		Network network = scenario.getNetwork();
		// increase capacity
		{//it looks a dead end link, but part of a highway, capacity can be something like -- 1800 at least in both directions.
			network.getLinks().get(Id.createLinkId("13800-13851-13857-13860")).setCapacity(1800.);
			network.getLinks().get(Id.createLinkId("1386010000-1385710000-1385110000-1380010000")).setCapacity(1800.); 
		}
		{//a major link, increase capacity from 300 to at least 1000.0
			network.getLinks().get(Id.createLinkId("858810000-8593-8592-8596-8534-8581-779610000-8111-8099-"
					+ "8104-8105-8101-8103-8084-8097-8091-8094-7959-8015-7986-800810000-7999-493-3204-3195")).setCapacity(1800.);
			network.getLinks().get(Id.createLinkId("319510000-320410000-49310000-799910000-8008-798610000-"
					+ "801510000-795910000-809410000-809110000-809710000-808410000-810310000-810110000-810510000-810410000-809910000-811110000-7796-858110000-853410000-859610000-859210000-859310000-8588")).setCapacity(1800.); 
		}
		{//a major link, increase capacity from 300 to at least 1000.0
			network.getLinks().get(Id.createLinkId("191610000-314110000")).setCapacity(1000.);
			network.getLinks().get(Id.createLinkId("3141-1916")).setCapacity(1000.);
		}
		{//minimum of the links on either side 
			network.getLinks().get(Id.createLinkId("1840910000")).setCapacity(1851.0);
			network.getLinks().get(Id.createLinkId("18409")).setCapacity(1851.0);
			
			network.getLinks().get(Id.createLinkId("1820310000")).setCapacity(1851.0);
			network.getLinks().get(Id.createLinkId("18203")).setCapacity(1851.0);
			
			network.getLinks().get(Id.createLinkId("18203")).setNumberOfLanes(2);
			network.getLinks().get(Id.createLinkId("18203")).setNumberOfLanes(2);
		}
		{// dont know y a link suddenly has so high capacity; does not make sense
			network.getLinks().get(Id.createLinkId("17346")).setCapacity(1074.0);
			network.getLinks().get(Id.createLinkId("1734410000")).setCapacity(1074.0);
			network.getLinks().get(Id.createLinkId("17346")).setNumberOfLanes(1.0);
			network.getLinks().get(Id.createLinkId("1734410000")).setNumberOfLanes(1.0);
		}
		{// suddenly so low capacity
			network.getLinks().get(Id.createLinkId("10610-10625-1062110000-1062610000-10397-1090010000-10907-10905-1091110000-1089610000")).setCapacity(2790.0);
			network.getLinks().get(Id.createLinkId("10896-10911-1090510000-1090710000-10900-1039710000-10626-10621-1062510000-1061010000")).setCapacity(2790.0);
			network.getLinks().get(Id.createLinkId("10610-10625-1062110000-1062610000-10397-1090010000-10907-10905-1091110000-1089610000")).setNumberOfLanes(2.0);
			network.getLinks().get(Id.createLinkId("10896-10911-1090510000-1090710000-10900-1039710000-10626-10621-1062510000-1061010000")).setNumberOfLanes(2.0);
			
			network.getLinks().get(Id.createLinkId("18783")).setCapacity(2790.0);
			network.getLinks().get(Id.createLinkId("1878310000")).setCapacity(2790.0);
			network.getLinks().get(Id.createLinkId("18783")).setNumberOfLanes(2.0);
			network.getLinks().get(Id.createLinkId("1878310000")).setNumberOfLanes(2.0);
		}
		{ // suddenly so low capacity
			Node n=network.getNodes().get(Id.createNodeId("10836"));
			for(Link l : n.getInLinks().values()) {
				l.setCapacity(2796.0); l.setNumberOfLanes(2.0);
			}
			for(Link l : n.getOutLinks().values()) {
				l.setCapacity(2796.0); l.setNumberOfLanes(2.0);
			}
			
			network.getLinks().get(Id.createLinkId("18795")).setCapacity(1838.0);
			network.getLinks().get(Id.createLinkId("1879510000")).setCapacity(1838.0);
			network.getLinks().get(Id.createLinkId("18795")).setNumberOfLanes(2.0);
			network.getLinks().get(Id.createLinkId("1879510000")).setNumberOfLanes(2.0);
			
			network.getLinks().get(Id.createLinkId("10927")).setCapacity(1838.0);
			network.getLinks().get(Id.createLinkId("1092710000")).setCapacity(1838.0);
			network.getLinks().get(Id.createLinkId("10927")).setNumberOfLanes(2.0);
			network.getLinks().get(Id.createLinkId("1092710000")).setNumberOfLanes(2.0);
		}
		{// suddenly so low capacity
			network.getLinks().get(Id.createLinkId("220610000")).setCapacity(1838.0);
			network.getLinks().get(Id.createLinkId("2206")).setCapacity(1838.0);
			network.getLinks().get(Id.createLinkId("220610000")).setNumberOfLanes(2.0);
			network.getLinks().get(Id.createLinkId("2206")).setNumberOfLanes(2.0);
			
			network.getLinks().get(Id.createLinkId("220710000")).setCapacity(1838.0);
			network.getLinks().get(Id.createLinkId("2207")).setCapacity(1838.0);
			network.getLinks().get(Id.createLinkId("220710000")).setNumberOfLanes(2.0);
			network.getLinks().get(Id.createLinkId("2207")).setNumberOfLanes(2.0);
		}
	}

	private void removeLinksNodes(){
		// remove links
		List<String> links2remove = Arrays.asList("1478","147810000",
				"1128-126410000-1262-126810000-126710000-127010000-126610000-1271-1258-128510000-71710000-1672-167310000-165510000-167710000-167610000-163910000-170410000-163810000-1735",
				"173510000-1638-1704-1639-1676-1677-1655-1673-167210000-717-1285-125810000-127110000-1266-1270-1267-1268-126210000-1264-112810000",
				"1841910000-18503","1850310000-18419",
				"18174","1817410000",
				"1861710000","18617",
				"145310000-1340-157710000-157110000-1569-156310000-68410000-1122-112310000-1124-109410000-110510000-1106-1101-1104-110210000-1103", //a useless links, dont know, y agents are diverted on it.
				"110310000-1102-110410000-110110000-110610000-1105-1094-112410000-1123-112210000-684-1563-156910000-1571-1577-134010000-1453",
				"145810000-1461-146210000","1462-146110000-1458","145510000","1455","1470-1471","147110000-147010000",
				"13902","1390210000","1390110000-13898-1390410000-1387210000-13914-1391310000","13913-1391410000-13872-13904-1389810000-13901", // agents are entering on this link instead of passing through OC1
				"1388010000-1388210000","13882-13880" // should not be present
				);
		for (String str : links2remove){
			scenario.getNetwork().removeLink(Id.createLinkId(str));
			LOG.warn("The link "+str+" is removed from the network.");
		}
		scenario.getNetwork().removeNode(Id.createNodeId("16348")); // this will also remove the connected links, these are new links after 2011
	}

	/**
	 * Removes nodes which do not have any in/out links.
	 */
	private void removeIsolatedNodes(){
		Collection<? extends Node> nodes = scenario.getNetwork().getNodes().values();
		List<Id<Node>> nodes2Remove = new ArrayList<>();
		for(Node n : nodes){
			if(n.getInLinks().size() == 0 && n.getOutLinks().size() == 0) {
				nodes2Remove.add(n.getId());
			}
		}
		for(Id<Node> nodeId : nodes2Remove){
			scenario.getNetwork().removeNode(nodeId);
			LOG.warn("The isolated node "+nodeId.toString()+" is removed from the network.");
		}
	}
}