/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTimeAnalyzer.java
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

package matrix;

import gnu.trove.TObjectLongHashMap;
import gnu.trove.TObjectLongIterator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.run.NetworkCleaner;

/**
 * @author sfuerbas
 * 
 */
public class NetworkScaler {

	/**
	 * @param args
	 * @throws IOException 
	 */
	
	static double BETW_MIN = 1500000.;	//minimum betweenness required to remain in network
	final static double CAPACITY_MIN = Double.MAX_VALUE;	//minimim capacity required to remain in network
	
	public static void main(String[] args) throws IOException {
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile("/Users/jillenberger/Work/work/socialnets/data/schweiz/network/switzerland_matsim_cl_simple.xml");	//provide MATSim network
		Network network = scenario.getNetwork();
		HashMap<String, Double> linkBetw = new HashMap<String, Double>();
		HashMap<Id, Link> linkDeletion = new HashMap<Id, Link>();	//HashMap containing all links up for deletion from network
//		List<Link> linkDeletion = new ArrayList<Link>();
		Double betw = 0.;
		String matsimLinkId = null;
		
//		BETW_MIN = network.getNodes().size();
		BETW_MIN = 200000;
//		BufferedReader br = new BufferedReader(new FileReader(args[1]));	//as args[1] provide file with results from MatrixCentrality in format: Matsim Id "TAB" Betweenness
//		
//		while (br.ready()) {
//			String aLine = br.readLine();
//			String splitLine[] = aLine.split("\t");
//			matsimLinkId = splitLine[0].trim();
//			betw = Double.parseDouble(splitLine[1]);
//			linkBetw.put(matsimLinkId, betw);
//		}
		
		TObjectLongHashMap<Link> values = BetweennessLoader.loadBetweenness("/Users/jillenberger/Work/work/socialnets/data/schweiz/network/BetweennessSchweiz.txt", network);
		TObjectLongIterator<Link> it = values.iterator();
		
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			if(it.value() <= BETW_MIN && it.key().getCapacity() <= CAPACITY_MIN) {
				linkDeletion.put(it.key().getId(), it.key());
				/*
				 * get back link
				 */
				Link link = it.key();
				Link backLink = null;
				Node from = link.getFromNode();
				Node to = link.getToNode();
				for(Link outLink : to.getOutLinks().values()) {
					if(outLink.getToNode() == from) {
						backLink = outLink;
						break;
					}
				}
				if(backLink != null)
					linkDeletion.put(backLink.getId(), backLink);
			}
		}
		
//		for (Link link : network.getLinks().values()) {
//			betw = linkBetw.get(link.getId().toString());
//			if (betw==null) continue;	//bricht teilweise ab, weil betw "null" wird. ursache habe ich noch nicht gefunden!
//			System.out.println(betw);
//			if (betw<=BETW_MIN && link.getCapacity()<=CAPACITY_MIN) {
//				linkDeletion.put(link.getId(), link);
////				System.out.println(linkDeletion.toString());
//			}
//		}
		
		int cnt = 0;
        for (Entry<Id, Link> entry : linkDeletion.entrySet()) {        
//            System.out.println(linkDeletion.get(entry.getKey()));
            if(network.removeLink(entry.getKey()) == null) {
            	//tut leider nicht das, was ich will, nämlich den link löschen. gibt es dafür eine methode?
            	System.err.println("Link not found!");
            } else {
            	cnt++;
            }
        }
        
        System.out.println("Removed " + cnt + " links");
		
        NetworkWriter writer = new NetworkWriter(network);
        writer.write("/Users/jillenberger/Work/work/socialnets/data/schweiz/network/network-out1.xml");

        new NetworkCleaner().run(new String[] {"/Users/jillenberger/Work/work/socialnets/data/schweiz/network/network-out1.xml", "/Users/jillenberger/Work/work/socialnets/data/schweiz/network/network-out2.xml"});

		

	}

}
