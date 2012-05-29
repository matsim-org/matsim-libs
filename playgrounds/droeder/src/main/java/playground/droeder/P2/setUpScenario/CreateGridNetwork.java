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
package playground.droeder.P2.setUpScenario;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author droeder
 *
 */
public class CreateGridNetwork {

	private static final String OUTDIR = "D:/VSP/net/ils/roeder/7x4Grid/";
	
	public static void main(String[] args){
		
		CreateGridNetwork.createTestGridNetwork(4, 7, 1000., OUTDIR + "network.xml.gz");
	}
	
	public static void createTestGridNetwork(int rows, int columns, double distanceFromNodeToNode, String outfile ) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Network network = scenario.getNetwork();
		NetworkFactory nf = network.getFactory();

		Node n;
		// creates nodes
		for(int row = 1; row < (rows + 1); row++){
			for(int column = 1; column < (columns + 1); column++){
				n = nf.createNode(scenario.createId(String.valueOf(column)+String.valueOf(row)), 
						scenario.createCoord(column * distanceFromNodeToNode, row * distanceFromNodeToNode));
				network.addNode(n);
			}
		}
		
		// create links
		Node from = null, to;
		Link l;
		//create rows
		for(int row = 1; row <(rows + 1); row++){
			from = null;
			for(int col = 1; col < (columns + 1); col++){
				if(from == null){
					from = network.getNodes().get(scenario.createId(String.valueOf(col)+String.valueOf(row)));
				}else{
					to = network.getNodes().get(scenario.createId(String.valueOf(col)+String.valueOf(row)));
					l = nf.createLink(scenario.createId(from.getId().toString()+to.getId().toString()), from, to);
					network.addLink(l);
					l = nf.createLink(scenario.createId(to.getId().toString()+from.getId().toString()), to, from);
					network.addLink(l);
					from = to;
				}
			}
		}
		
		//create columns
		for(int col = 1; col < (columns + 1); col++){
			from = null;
			for(int row = 1; row <(rows + 1); row++){
				if(from == null){
					from = network.getNodes().get(scenario.createId(String.valueOf(col)+String.valueOf(row)));
				}else{
					to = network.getNodes().get(scenario.createId(String.valueOf(col)+String.valueOf(row)));
					l = nf.createLink(scenario.createId(from.getId().toString()+to.getId().toString()), from, to);
					network.addLink(l);
					l = nf.createLink(scenario.createId(to.getId().toString()+from.getId().toString()), to, from);
					network.addLink(l);
					from = to;
				}
			}
		}
		
		
		Set<String> modes = new TreeSet<String>();
		modes.add(TransportMode.car);
		for (Link link : network.getLinks().values()) {
			link.setLength(distanceFromNodeToNode);
			link.setCapacity(2000.0);
			link.setFreespeed(13.8);
			link.setAllowedModes(modes);
			link.setNumberOfLanes(1.0);
		}
		
		new NetworkWriter(scenario.getNetwork()).write(outfile);
	}
	

}
