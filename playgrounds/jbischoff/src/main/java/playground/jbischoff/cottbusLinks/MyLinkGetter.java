/* *********************************************************************** *
 * project: org.matsim.*
 * MyLinkGetter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.jbischoff.cottbusLinks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *
 */

public class MyLinkGetter {
	private static Logger log = Logger.getLogger(MyLinkGetter.class);
	private LeastCostPathCalculator lcp;
	private Network network;
	private List<Id<Link>> stopLinkList;
	private List<Id<Link>> routeLinkList;
	
	public	MyLinkGetter() {

	
	
	DijkstraFactory df = new DijkstraFactory();
	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	String netfile = "Z:\\WinHome\\Docs\\cottbus\\cottbus_feb_fix\\Cottbus-pt\\network_pt.xml";
	log.info("loading network from " + netfile);
	network = scenario.getNetwork();
	new MatsimNetworkReader(scenario).readFile(netfile);
	FreespeedTravelTimeAndDisutility fs =new FreespeedTravelTimeAndDisutility(-6, 7,-100);
	lcp =  df.createPathCalculator(network, fs,fs);
	
	
	
	
	}
	
	public static void main(String[] args) {
		MyLinkGetter mgl = new MyLinkGetter();
//		String line = "1417";
//		String dir = "ow";
//		String in = "E:\\Cottbus\\Cottbus_pt\\Linien\\"+line+"\\"+line+dir+".csv";
//		String out = "E:\\Cottbus\\Cottbus_pt\\Linien\\"+line+"\\"+line+dir+"_links.csv";
//		
//		mgl.run(in,out);
		List<Link> ll = mgl.getLinks(Id.create(6546, Link.class), Id.create(6549, Link.class));
		for (Link l : ll){
			System.out.println(l.getId());
		}
	}

	public void run(String infile, String outfile) {

		this.stopLinkList = readStopLinks(infile);
		this.routeLinkList = calculateRoutes();
		this.writeRoutes(outfile);
		
		
		
		
	}

	
	
	private void writeRoutes(String outfile) {
		try {
			log.info("Writing Links to "+outfile);
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
			for (Id<Link> linkid : this.routeLinkList){
				bw.append(linkid.toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();
			log.info("Done Writing");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private List<Id<Link>> calculateRoutes() {
		List<Id<Link>> linkids = new ArrayList<>();
		int size = this.stopLinkList.size();
		linkids.add(this.stopLinkList.get(0));
//		for the very first link, as routing is nodebased on toNodes
		
		for (int i = 0; i<(size-1);i++){
			
			for (Link link : this.getLinks(this.stopLinkList.get(i), this.stopLinkList.get(i+1))){
				linkids.add(link.getId());
				
			}
		}

		
		
		
		return linkids;
	}

	private List<Id<Link>> readStopLinks(String filename) {
		
		log.info("Reading Links from "+filename);
		List<Id<Link>> linkids = new ArrayList<>();
		
		FileReader fr;
		try {
			fr = new FileReader(new File (filename));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null) {
			    line = line.replaceAll("\"", "");
				String[] result = line.split(";");
			    Id<Link> current = Id.create(result[6], Link.class);
			    linkids.add(current);
			}
			
			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		return linkids;
	}

	private List<Link> getLinks(Id<Link> fromlink, Id<Link> tolink) {
		
		List<Link> route = null;
		log.info("from: "+fromlink+" to: "+tolink);
		try{
		Node fromNode = network.getLinks().get(fromlink).getToNode();
		Node toNode = network.getLinks().get(tolink).getToNode();
//		log.info("calculating from "+fromNode+" to "+toNode);
		
		
		route = lcp.calcLeastCostPath(fromNode, toNode, 6.0, null, null).links;
		}
		catch (NullPointerException e){
			log.error("could not get toNode for "+fromlink + " or fromNode for "+tolink );
			e.printStackTrace();
			
			
		}
		return route;
		
	}

	
}

