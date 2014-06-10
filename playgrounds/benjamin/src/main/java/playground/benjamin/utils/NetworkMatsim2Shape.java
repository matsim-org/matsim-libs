/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkMatsim2Shape
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
package playground.benjamin.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;


/**
 * @author benjamin
 *
 */
public class NetworkMatsim2Shape {
	
	private static Logger log = Logger.getLogger(NetworkMatsim2Shape.class);
	
//	private static String filePath = "../../detailedEval/Net/";
//	private static String networkName = "network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes";
//	private static String networkName = "../policies/network-86-85-87-84_simplified---withLanes_zone30";
	
	private static String filePath = "../../runs-svn/run892/";
	private static String networkName = "892.output_network";
	
//	private static String inFileType = ".xml";
	private static String inFileType = ".xml.gz";
	private static String outFileType = ".shp";
	
	private static boolean filterLinks = true;
	private static String linksToFilter = filePath + "zh_forRun891_distanceMorningToll_0630-0900_cityWOhighways_35rp_per_km.xml";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFile = filePath + networkName + inFileType;
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Network net;
		if(!filterLinks){
			net = scenario.getNetwork();
			new MatsimNetworkReader(scenario).readFile(netFile);
		} else {
			Network network = scenario.getNetwork();
			new MatsimNetworkReader(scenario).readFile(netFile);
			net = filterNetwork(network);
		}
		new Links2ESRIShape(net, filePath + networkName + outFileType, "DHDN_GK4").write();
	}

	private static Network filterNetwork(Network network) {
		Network net = NetworkUtils.createNetwork();
		RoadPricingSchemeImpl rps = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpr = new RoadPricingReaderXMLv1(rps);
		rpr.parse(linksToFilter);
		Set<Id> linkList = rps.getTolledLinkIds();
		for(Link link : network.getLinks().values()){
			Id linkId = link.getId();
			if(linkList.contains(linkId)){
				Id fromId = link.getFromNode().getId();
				Id toId = link.getToNode().getId();
//				log.info("fromId " + fromId);
//				log.info("toId " + toId);
				if (!net.getNodes().containsKey(fromId)){
					addNode(net, link.getFromNode());
//					log.info("NodeIds " + net.getNodes().keySet().toString());
				}
				if (!net.getNodes().containsKey(toId)){
					addNode(net, link.getToNode());
				}
				net.addLink(link);
			}
		}
		return net;
	}
	
	private static void addNode(Network net, Node n){
		Node newNode = net.getFactory().createNode(n.getId(), n.getCoord());
		net.addNode(newNode);
	}
}
