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
import java.util.HashSet;
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

import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaConstants;
import playground.andreas.utils.net.NetworkSimplifier;
/**
 * @author amit
 */

public class PatnaNetworkGenerator {       

	private static final Logger logger = Logger.getLogger(PatnaNetworkGenerator.class);
	private Scenario scenario;

	public static void main(String[] args) throws IOException  {  
		PatnaNetworkGenerator png =  new PatnaNetworkGenerator();
		png.processDataAndWriteNetwork();
		new NetworkWriter(png.getPatnaNetwork()).write("../../../../repos/runs-svn/patnaIndia/run108/input/network_diff_linkSpeed.xml.gz");
	}

	public void processDataAndWriteNetwork() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());                                       
		final Network network = scenario.getNetwork();

		String inputFileNetwork    =  PatnaConstants.inputFilesDir+"/networkInputTransCad.csv" ;

		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();       
		tabularFileParserConfig.setFileName(inputFileNetwork);
		tabularFileParserConfig.setDelimiterTags(new String[] {","});                               
		tabularFileParserConfig.setStartTag("linkId");											
		TabularFileHandler tabularFileHandler = new TabularFileHandler()

		{            
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
					double xcord = ((Double.parseDouble(fromNodeXCoord))/(1000000));				
					double ycord = ((Double.parseDouble(fromNodeYCoord))/(1000000));
					Coord createCoord = new Coord(xcord, ycord);
					fromNode = network.getFactory().createNode(fromNodeId, PatnaConstants.COORDINATE_TRANSFORMATION.transform( createCoord));
					network.addNode(fromNode);
				}

				Id<Node> toNodeId = Id.create(ToNodeId,Node.class);
				Node toNode;
				if (network.getNodes().containsKey(toNodeId)) {     		   
					toNode = network.getNodes().get(toNodeId);
				}
				else {
					double xcord = ((Double.parseDouble(toNodeXCoord))/(1000000));
					double ycord = ((Double.parseDouble(toNodeYCoord))/(1000000));
					Coord createCoord = new Coord(xcord, ycord);
					toNode = network.getFactory().createNode(toNodeId, PatnaConstants.COORDINATE_TRANSFORMATION.transform(createCoord) );
					network.addNode(toNode);
				}

				Link link1 = network.getFactory().createLink(Id.create(linkId,Link.class), fromNode, toNode); 
				Link link2 = network.getFactory().createLink(Id.create(linkId + "10000",Link.class), toNode, fromNode);   

				int streamSpeed = Integer.parseInt(speedInKmph);
				double freeSpeed = 0;
//				type of road is Arterial, sub arterial and collector so speeds are 50. 40 . 40kph respectively.
//				in given data speed is stream speeds (25, 20 and 15) not free flow speed
				switch (streamSpeed)						
				{ 
				case 25 :	freeSpeed = 50; break;
				case 20 :	freeSpeed = 40; break;
				case 15 : 	freeSpeed = 40; break;
				case 50 :	freeSpeed = 60; break;
				}
				double freeSpeedInMPS = freeSpeed/3.6;	
//				double freeSpeedInMPS = 60/3.6; 

				double roadWidth = (0.5*Double.parseDouble(widthOfRoad));			
				int numberoflanes = 0;

				if ( roadWidth < 4) numberoflanes = 1;								
				else if ( roadWidth <9 && roadWidth >= 4) numberoflanes =2;
				else if ( roadWidth <12 && roadWidth >= 9) numberoflanes =3;

				double linkLength = 1000 * Double.parseDouble(lengthInKm);

				link1.setFreespeed(freeSpeedInMPS);
				link1.setCapacity(capacityOfLink(widthOfRoad));
				link1.setNumberOfLanes(numberoflanes);
				link1.setLength(linkLength);
				link1.setAllowedModes(new HashSet<>(PatnaConstants.allModes));
				network.addLink(link1);

				link2.setFreespeed(freeSpeedInMPS);
				link2.setCapacity(capacityOfLink(widthOfRoad));
				link2.setNumberOfLanes(numberoflanes);
				link2.setLength(linkLength);
				link2.setAllowedModes(new HashSet<>(PatnaConstants.allModes));
				network.addLink(link2);
			}
		};

		TabularFileParser tabularFileParser = new TabularFileParser();
		tabularFileParser.parse(tabularFileParserConfig, tabularFileHandler);  
		new NetworkCleaner().run(network);

		logger.info("Number of links in the network are"+ network.getLinks().size()+" and number of nodes in the link are"+network.getNodes().size());

		NetworkSimplifier simplifier = new NetworkSimplifier();
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();

		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));

		simplifier.setNodesToMerge(nodeTypesToMerge);
		simplifier.run(network);
	}    
	
	public Network getPatnaNetwork() {
		return scenario.getNetwork();
	}

	private double capacityOfLink (String roadwidth) {
		double linkCapacity =0;
		double w = Double.parseDouble(roadwidth);
		double capacityCarrigway = -2184-22.6*Math.pow(w, 2)+857.4*w;  
		linkCapacity = 0.5*capacityCarrigway;
		if (linkCapacity < 300) linkCapacity = 300; 
		return linkCapacity;
	}
}