/* *********************************************************************** *
 * project: org.matsim.*
 * KShortestPath.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;

import playground.balmermi.algos.RouteSetGenerator;

public class CalcRouteSets {

	private static RouteSetGenerator gen;

	private static FileWriter fw;;
	private static BufferedWriter out;

	//////////////////////////////////////////////////////////////////////
	//
	//////////////////////////////////////////////////////////////////////

	public static void runRouteSetAlgo(Integer id,Integer orig,Integer dest) {

		System.out.println("RUN: runRouteSetAlgo...");

		NetworkLayer network = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);

		Node o = network.getNode("" + orig);
		Node d = network.getNode("" + dest);
		int time = 0;
		int nof_routes = 20;
		int var_factor = 3;

		if (o.getId().equals(d.getId())) { return; }

		System.out.println("  create route set...");
		LinkedList<Route> routes = gen.calcRouteSet(o,d,nof_routes,time,var_factor);
		System.out.println("  done.");

		System.out.println("  write route set file...");
		writeRouteSetFile(id,o,d,routes);
		printRoutes(o,d,routes);
		System.out.println("  done.");

		System.out.println("RUN: runRouteSetAlgo finished.");
		System.out.println();
	}

	public static void parseODPairs(final String inputfile) {

		ArrayList<Integer> ids = new ArrayList<Integer>();
		ArrayList<Integer> origs = new ArrayList<Integer>();
		ArrayList<Integer> dests = new ArrayList<Integer>();

		int line_cnt = 0;
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine(); line_cnt++;
			System.out.println(curr_line);
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// IDSEGMENT  StartNode  EndNode
				// 0          1          2
				Integer id = Integer.parseInt(entries[0].trim());
				Integer orig = Integer.parseInt(entries[1].trim());
				Integer dest = Integer.parseInt(entries[2].trim());

				ids.add(id);
				origs.add(orig);
				dests.add(dest);

				// progress report
				if (line_cnt % 100000 == 0) {
					System.out.println("    Line " + line_cnt);
				}
				line_cnt++;
			}
			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("    # lines = " + line_cnt);
		System.out.println("    # ids = " + ids.size());
		System.out.println("    # origs = " + origs.size());
		System.out.println("    # dests = " + dests.size());

		for (int i=0; i< ids.size(); i++) {
			runRouteSetAlgo(ids.get(i),origs.get(i),dests.get(i));
		}
	}

	public static void printRoutes(Node o, Node d, LinkedList<Route> routes) {
		Iterator<Route> r_it = routes.iterator();
		while (r_it.hasNext()) {
			Route r = r_it.next();
			Link[] links = r.getLinkRoute();
			for (int i=0; i<links.length; i++) {
				Link l = links[i];
				System.out.print(l.getId() + "\t");
			}
			System.out.print("\n");
		}
	}

	public static void writeRouteSetFile(Integer id, Node o, Node d, LinkedList<Route> routes) {
		try {
			out.flush();
			boolean is_first = true;
			Iterator<Route> r_it = routes.iterator();
			while (r_it.hasNext()) {
				Route r = r_it.next();
				out.write(id.toString());
				out.write("\t" + o.getId());
				out.write("\t" + d.getId());
				Link[] links = r.getLinkRoute();
				for (int i=0; i<links.length; i++) { out.write("\t" + links[i].getId()); }
				out.write("\t" + "-1");
				if (is_first) { out.write("\t" + "1"); is_first = false; }
				else { out.write("\t" + "0"); }
				out.write("\t" + "-1");
				out.write("\n");
				out.flush();
			}
			out.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws Exception {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();

		Scenario.setUpScenarioConfig();

		System.out.println("reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("done.");

		fw = new FileWriter("output/routesets.txt");
		out = new BufferedWriter(fw);
		gen = new RouteSetGenerator(network);

		out.write("# Routesets\n");
		out.write("# SEG_ID\tFROM_NODE\tTO_NODE\tROUTE(linklist)...\t-1\tLEASTCOSTROUTE(0,1)\t-1\n");
		out.flush();

		parseODPairs("input/routen_start_ende.txt");

		out.close();
		fw.close();

		Gbl.printElapsedTime();
	}
}
