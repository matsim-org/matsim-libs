/* *********************************************************************** *
 * project: org.matsim.*
 * ManteuffelRoadTypeMapper.java
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

package playground.benjamin.scenarios.manteuffel;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author benjamin
 *
 */
public class ManteuffelRoadTypeMapper {
	
	private static final String netFile = "../../runs-svn/manteuffelstrasse/bau/bvg.run190.25pct.dilution001.network20150727.v2.static.output_network.xml.gz";
	private static final String outputNetFile = "../../runs-svn/manteuffelstrasse/bau/bvg.run190.25pct.dilution001.network20150727.v2.static.emissionNetwork.xml.gz";
	
//	private static final String netFile = "../../runs-svn/manteuffelstrasse/p3/bvg.run190.25pct.dilution001.network.20150731.LP2.III.output_network.xml.gz";
//	private static final String outputNetFile = "../../runs-svn/manteuffelstrasse/p3/bvg.run190.25pct.dilution001.network.20150731.LP2.III.emissionNetwork.xml.gz";
	
//	private static final String netFile = "../../runs-svn/manteuffelstrasse/p4/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.output_network.xml.gz";
//	private static final String outputNetFile = "../../runs-svn/manteuffelstrasse/p4/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.emissionNetwork.xml.gz";
	
	private static final String roadTypeMappingFile = "../../runs-svn/manteuffelstrasse/hbefaForMatsim/roadTypeMapping.txt";
	
	private static Scenario scenario;
	private static Network outputNet;
	private static BufferedWriter writer;

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		scenario = ScenarioUtils.loadScenario(config);
		Network inputNet = scenario.getNetwork();
		addEmissionInformation(inputNet);
		new NetworkWriter(outputNet).write(outputNetFile);
		writeRoadTypeMappingFile();
	}

	private static void writeRoadTypeMappingFile() {
		writer = IOUtils.getBufferedWriter(roadTypeMappingFile);
		try {
			writer.write("VISUM_RT_NR" + ";" + "VISUM_RT_NAME" + ";"
					+ "HBEFA_RT_NAME" + "\n");
			writer.write("01" + ";" + "30kmh" + ";" + "URB/Access/30" + "\n");
			writer.write("02" + ";" + "40kmh" + ";" + "URB/Access/40"+ "\n");
			writer.write("031" + ";" + "50kmh-1l" + ";" + "URB/Local/50"+ "\n");
			writer.write("032" + ";" + "50kmh-2l" + ";" + "URB/Distr/50"+ "\n");
			writer.write("033" + ";" + "50kmh-3+l" + ";" + "URB/Trunk-City/50"+ "\n");
			writer.write("041" + ";" + "60kmh-1l" + ";" + "URB/Local/60"+ "\n");
			writer.write("042" + ";" + "60kmh-2l" + ";" + "URB/Trunk-City/60"+ "\n");
			writer.write("043" + ";" + "60kmh-3+l" + ";" + "URB/MW-City/60"+ "\n");
			writer.write("05" + ";" + "70kmh" + ";" + "URB/MW-City/70"+ "\n");
			writer.write("06" + ";" + "80kmh" + ";" + "URB/MW-Nat./80"+ "\n");
			writer.write("07" + ";" + ">80kmh" + ";" + "RUR/MW/>130"+ "\n"); //TODO: only applicable for this scenario!
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Could not write file.", e);
		}
	}

	private static void addEmissionInformation(Network inputNet) {
		outputNet = NetworkUtils.createNetwork();
		
		for(Link link : inputNet.getLinks().values()){
			Id<Node> fromId = link.getFromNode().getId();
			Id<Node> toId = link.getToNode().getId();
			Node from = null;
			Node to = null;
			Node nn;
			//check if from node already exists
			if (!outputNet.getNodes().containsKey(fromId)) {
				nn = inputNet.getNodes().get(fromId);
				from = addNode(outputNet, nn);
			}
			else {
				from = outputNet.getNodes().get(fromId);
			}
			//check if to node already exists
			if (!outputNet.getNodes().containsKey(toId)){
				nn = inputNet.getNodes().get(toId);
				to = addNode(outputNet, nn);
			}
			else {
				to = outputNet.getNodes().get(toId);
			}
			Link ll = mapLink2RoadType(link, from, to);
			outputNet.addLink(ll);
		}
	}
	
	private static Node addNode(Network net, Node n){
		Node newNode = net.getFactory().createNode(n.getId(), n.getCoord());
		net.addNode(newNode);
		return newNode;
	}
	
	private static Link mapLink2RoadType(Link link, Node from, Node to) {
		Link ll = outputNet.getFactory().createLink(link.getId(), from, to);
		ll.setAllowedModes(link.getAllowedModes());
		ll.setCapacity(link.getCapacity());
		ll.setFreespeed(link.getFreespeed());
		ll.setLength(link.getLength());
		ll.setNumberOfLanes(link.getNumberOfLanes());
		
		double fs = link.getFreespeed();
		if(fs <= 8.333333333){ //30kmh
			((LinkImpl) ll).setType("01");
		} else if(fs <= 11.111111111){ //40kmh
			((LinkImpl) ll).setType("02");
		} else if(fs <= 13.888888889){ //50kmh
			double lanes = ll.getNumberOfLanes();
			if(lanes <= 1.0){
				((LinkImpl) ll).setType("031");
			} else if(lanes <= 2.0){
				((LinkImpl) ll).setType("032");
			} else if(lanes > 2.0){
				((LinkImpl) ll).setType("033");
			} else{
				throw new RuntimeException("NoOfLanes not properly defined");
			}
		} else if(fs <= 16.666666667){ //60kmh
			double lanes = ll.getNumberOfLanes();
			if(lanes <= 1.0){
				((LinkImpl) ll).setType("041");
			} else if(lanes <= 2.0){
				((LinkImpl) ll).setType("042");
			} else if(lanes > 2.0){
				((LinkImpl) ll).setType("043");
			} else{
				throw new RuntimeException("NoOfLanes not properly defined");
			}
		} else if(fs <= 19.444444444){ //70kmh
			((LinkImpl) ll).setType("05");
		} else if(fs <= 22.222222222){ //80kmh
			((LinkImpl) ll).setType("06");
		} else if(fs > 22.222222222){ //faster
			((LinkImpl) ll).setType("07");
		} else{
			throw new RuntimeException("Link not considered...");
		}
		return ll;
	}
}
