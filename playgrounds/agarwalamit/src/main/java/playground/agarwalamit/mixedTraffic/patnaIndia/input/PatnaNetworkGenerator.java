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
package playground.agarwalamit.mixedTraffic.patnaIndia.input;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaUtils;
import playground.andreas.utils.net.NetworkSimplifier;
/**
 * @author amit
 */

public class PatnaNetworkGenerator {       

	private static final Logger LOG = Logger.getLogger(PatnaNetworkGenerator.class);
	private Scenario scenario;

	public static void main(String[] args) throws IOException  {  
		PatnaNetworkGenerator png =  new PatnaNetworkGenerator();
		png.startProcessingFile();
		String outNetwork = "../../../../repos/runs-svn/patnaIndia/run108/input/network_diff_linkSpeed.xml.gz";
		new NetworkWriter(png.getPatnaNetwork()).write(outNetwork);
		LOG.info("The network file is written to - "+ outNetwork);
	}

	public void startProcessingFile() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());                                       
		final Network network = scenario.getNetwork();

		String inputFileNetwork = PatnaUtils.INPUT_FILES_DIR+"/networkInputTransCad.csv" ;

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
				double capacity = capacityOfLink(widthOfRoad);

				link1.setFreespeed(freeSpeedInMPS);
				link1.setCapacity(capacity);
				link1.setNumberOfLanes(numberoflanes);
				link1.setLength(linkLength);
				link1.setAllowedModes(new HashSet<>(PatnaUtils.ALL_MAIN_MODES));
				network.addLink(link1);

				link2.setFreespeed(freeSpeedInMPS);
				link2.setCapacity(capacity);
				link2.setNumberOfLanes(numberoflanes);
				link2.setLength(linkLength);
				link2.setAllowedModes(new HashSet<>(PatnaUtils.ALL_MAIN_MODES));
				network.addLink(link2);
			}
		};

		TabularFileParser tabularFileParser = new TabularFileParser();
		tabularFileParser.parse(tabularFileParserConfig, tabularFileHandler);  
		new NetworkCleaner().run(network);

		LOG.info("Number of links in the network are"+ network.getLinks().size()+" and number of nodes in the link are"+network.getNodes().size());

		NetworkSimplifier simplifier = new NetworkSimplifier();
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();

		nodeTypesToMerge.add(Integer.valueOf(4));
		nodeTypesToMerge.add(Integer.valueOf(5));

		simplifier.setNodesToMerge(nodeTypesToMerge);
		simplifier.run(network);

		// manual cleaning of the network.
		// remove links
		List<String> links2remove = Arrays.asList("1478","147810000",
				"1128-126410000-1262-126810000-126710000-127010000-126610000-1271-1258-128510000-71710000-1672-167310000-165510000-167710000-167610000-163910000-170410000-163810000-1735",
				"173510000-1638-1704-1639-1676-1677-1655-1673-167210000-717-1285-125810000-127110000-1266-1270-1267-1268-126210000-1264-112810000",
				"1841910000-18503","1850310000-18419",
				"18174","1817410000",
				"1861710000","18617",
				"145310000-1340-157710000-157110000-1569-156310000-68410000-1122-112310000-1124-109410000-110510000-1106-1101-1104-110210000-1103", //a useless links, dont know, y agents are diverted on it.
				"110310000-1102-110410000-110110000-110610000-1105-1094-112410000-1123-112210000-684-1563-156910000-1571-1577-134010000-1453",
				"145810000-1461-146210000","1462-146110000-1458","145510000","1455","1470-1471","147110000-147010000");
		for (String str : links2remove){
			network.removeLink(Id.createLinkId(str));
			LOG.warn("The link "+str+" is removed from the network.");
		}

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
	}    

	public Network getPatnaNetwork() {
		return scenario.getNetwork();
	}

	private double capacityOfLink (final String roadwidth) {
		double linkCapacity =0;
		double w = Double.parseDouble(roadwidth);
		double capacityCarrigway = -2184-22.6*Math.pow(w, 2)+857.4*w;  
		linkCapacity = 0.5*capacityCarrigway;
		if (linkCapacity < 300) linkCapacity = 300; 
		return linkCapacity;
	}
}