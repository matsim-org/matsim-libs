/* *********************************************************************** *
 * project: org.matsim.*
 * Events2TTMatrix.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.toronto.ttimematrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;


public class Events2TTMatrix {
	
	/**
	 * This method, if I am seeing this right, is meant to parse a file that contains the mapping from links to zones
	 * (assuming that linkIds are in column 0, and zones in column 1).
	 * But it will presumably parse any two columns of a column-oriented file.
	 * @param infile
	 * @return the Map<Id,Id> containing the key (=linkId) value (=zoneId) pairs
	 */
	private static final Map<Id,Id> parseL2ZMapping(String infile) {
		Map<Id,Id> l2zMapping = new HashMap<Id,Id>();
		try {
			FileReader fr = new FileReader(infile);
			BufferedReader br = new BufferedReader(fr);

			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// lid  zid
				// 0    1
				Id lid = new IdImpl(entries[0]);
				Id zid = new IdImpl(entries[1]);
				l2zMapping.put(lid,zid);
			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		return l2zMapping;
	}

	public static void main(String[] args) {
		// input arguments
		if (args.length != 5) {
			System.out.println("Usage: Events2TTMatrix XMLnetwork TXTevents l2z-mapping hours outputfile");
			System.out.println("       XMLnetwork:  MATSim xml network");
			System.out.println("       TXTevents:   MATSim text events file");
			System.out.println("       l2z-mapping: link2zone mapping file (create it via playground.toronto.mapping.LinkZoneMapping)");
			System.out.println("       hours:       list of hours for the matrix, e.g. '0,7,8,12,19,23'. range[0..]");
			System.out.println("       outputfile:  where to store the output ttime matrix file");
			System.out.println();
			System.out.println("example: Events2TTMatrix input/network.xml.gz input/events.txt.gz input/l2z-mapping.txt 8,12,17 output/ttimes.txt");
			System.out.println();
			System.out.println("----------------");
			System.out.println("2008, matsim.org");
			System.out.println();
			System.exit(-1);
		}
		String networkfile = args[0];
		String eventsfile = args[1];
		String mapfile = args[2];
		String hrsStr = args[3];
		String outfile = args[4];
		
		String[] hrsArray = hrsStr.split(",");
		int[] hours = new int[hrsArray.length];
		for (int i=0; i<hrsArray.length; i++) { hours[i] = Integer.parseInt(hrsArray[i]); }

		System.out.println("Arguments:");
		System.out.println("  XMLnetwork:  "+networkfile);
		System.out.println("  TXTevents:   "+eventsfile);
		System.out.println("  l2z-mapping: "+mapfile);
		System.out.print("  hours:");
		for (int i=0; i<hours.length; i++) { System.out.print(" "+hours[i]); }
		System.out.print("\n");
		System.out.println("  outputfile: "+outfile);
		System.out.println();
		
		// reading the network
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkfile);

		TravelTimeCalculator ttc = new TravelTimeCalculator(network,3600,30*3600, scenario.getConfig().travelTimeCalculator());
		SpanningTree st = new SpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, scenario.getConfig().charyparNagelScoring()));
		TTimeMatrixCalculator ttmc = new TTimeMatrixCalculator(parseL2ZMapping(mapfile),hours,st,network);

		// creating events object and assign handlers
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(ttmc);
		events.addHandler(ttc);
		
		// reading events.  Will do all the processing as side effect.
		System.out.println("processing events file...");
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile(eventsfile);
		System.out.println("done.");
		
		// writing matrix
		System.out.println("writing matrix...");
		ttmc.writeMatrix(outfile);
		System.out.println("done.");
	}
}
