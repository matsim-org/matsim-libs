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

package cottbusLinks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

/**
 * @author jbischoff
 *
 */

public class MyLinkGetter {
	private static Logger log = Logger.getLogger(MyLinkGetter.class);
	private LeastCostPathCalculator lcp;
	private Network network;
	private List<Id> stopLinkList;
	private List<Id> routeLinkList;
	
	public	MyLinkGetter() {

	
	
	DijkstraFactory df = new DijkstraFactory();
	ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	String netfile = "E:\\Cottbus\\cottbus\\cottbus_feb_fix\\Cottbus-pt\\network_pt.xml";
	log.info("loading network from " + netfile);
	network = scenario.getNetwork();
	new MatsimNetworkReader(scenario).readFile(netfile);
	FreespeedTravelTimeCost fs =new FreespeedTravelTimeCost(-6, 7,-100);
	lcp =  df.createPathCalculator(network, fs,fs);
	
	
	
	
	}
	
	public static void main(String[] args) {
		MyLinkGetter mgl = new MyLinkGetter();
		String line = "1417";
		String dir = "ow";
		String in = "E:\\Cottbus\\Cottbus_pt\\Linien\\"+line+"\\"+line+dir+".csv";
		String out = "E:\\Cottbus\\Cottbus_pt\\Linien\\"+line+"\\"+line+dir+"_links.csv";
		
		mgl.run(in,out);

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
			for (Id linkid : this.routeLinkList){
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

	private List<Id> calculateRoutes() {
		List<Id> linkids = new ArrayList<Id>();
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

	private List<Id> readStopLinks(String filename) {
		
		log.info("Reading Links from "+filename);
		List<Id> linkids = new ArrayList<Id>();
		
		FileReader fr;
		try {
			fr = new FileReader(new File (filename));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null) {
			    line = line.replaceAll("\"", "");
				String[] result = line.split(";");
			    Id current = new IdImpl(result[6]);
			    linkids.add(current);
			}
			
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		return linkids;
	}

	private List<Link> getLinks(Id fromlink, Id tolink) {
		
		List<Link> route = null;
		log.info("from: "+fromlink+" to: "+tolink);
		try{
		Node fromNode = network.getLinks().get(fromlink).getToNode();
		Node toNode = network.getLinks().get(tolink).getToNode();
//		log.info("calculating from "+fromNode+" to "+toNode);
		
		
		route = lcp.calcLeastCostPath(fromNode, toNode, 6.0).links;
		}
		catch (NullPointerException e){
			log.error("could not get toNode for "+fromlink + " or fromNode for "+tolink );
			e.printStackTrace();
			
			
		}
		return route;
		
	}

	
}

