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

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
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
	
//	private static String filePath = "../../runs-svn/run892/";
//	private static String networkName = "892.output_network";
	
	private static String filePath = "../../runs-svn/krasnojarsk/bau/";
	private static String networkName = "network";
	
	private static String inFileType = ".xml";
//	private static String inFileType = ".xml.gz";
	private static String outFileType = ".shp";
	
	private static boolean filterLinks = false;
	private static String linksToFilter = filePath + "zh_forRun891_distanceMorningToll_0630-0900_cityWOhighways_35rp_per_km.xml";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFile = filePath + networkName + inFileType;
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Network net;
		if(!filterLinks){
			net = scenario.getNetwork();
			new MatsimNetworkReader(scenario.getNetwork()).readFile(netFile);
		} else {
			Network network = scenario.getNetwork();
			new MatsimNetworkReader(scenario.getNetwork()).readFile(netFile);
			net = filterNetwork(network);
		}
//		new Links2ESRIShape(net, filePath + networkName + outFileType, TransformationFactory.WGS84).write();
//		new Links2ESRIShape(net, filePath + networkName + outFileType, TransformationFactory.CH1903_LV03_GT).write();
//		new Links2ESRIShape(net, filePath + networkName + outFileType, TransformationFactory.DHDN_GK4).write();
		new Links2ESRIShape(net, filePath + networkName + outFileType, "EPSG:32646").write();
//		new Links2ESRIShape(net, filePath + networkName + outFileType, "WGS84_UTM32T").write();
	}

	private static Network filterNetwork(Network network) {
		CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(TransformationFactory.CH1903_LV03_GT, TransformationFactory.DHDN_GK4);
//		CoordinateTransformation transform = TransformationFactory.getCoordinateTransformation(TransformationFactory.CH1903_LV03_GT, "WGS84_UTM32T");
		
		
		Network net = NetworkUtils.createNetwork();
		RoadPricingSchemeImpl rps = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpr = new RoadPricingReaderXMLv1(rps);
		rpr.readFile(linksToFilter);
		Set<Id<Link>> linkList = rps.getTolledLinkIds();
		for(Link link : network.getLinks().values()){
			Id linkId = link.getId();
			if(linkList.contains(linkId)){
				Id fromId = link.getFromNode().getId();
				Id toId = link.getToNode().getId();
				Coord fromNodeCoord = link.getFromNode().getCoord();
				Coord toNodeCoord = link.getToNode().getCoord();
				Coord fromNodeTransformed = transform.transform(fromNodeCoord);
				Coord toNodeTransformed = transform.transform(toNodeCoord);
//				Node newFromNode = net.getFactory().createNode(fromId, fromNodeCoord);
//				Node newToNode = net.getFactory().createNode(toId, toNodeCoord);
				Node newFromNode = net.getFactory().createNode(fromId, fromNodeTransformed);
				Node newToNode = net.getFactory().createNode(toId, toNodeTransformed);
				if (!net.getNodes().containsKey(fromId)){
					net.addNode(newFromNode);
				}
				if (!net.getNodes().containsKey(toId)){
					net.addNode(newToNode);
				}
				Link ll = net.getFactory().createLink(link.getId(), newFromNode, newToNode);
				net.addLink(ll);
			}
		}
		return net;
	}
}
