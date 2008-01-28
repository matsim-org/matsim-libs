/* *********************************************************************** *
 * project: org.matsim.*
 * RoutesForGianluca.java
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

public class RoutesForGianluca {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final NetworkLayer network;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public RoutesForGianluca() {
		System.out.println("  reading the network xml file...");
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		this.readRoutes("input/gewaehlte_routen.txt");
//		this.writeData("output/greetimes.xml");
		
		Scenario.writeNetwork(network);
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	public final void readRoutes(String inputfile) {
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine();
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    ID    Origin  Destination  linkID_1  linkID_2  ..  linkID_12
				// example: 2768  17816   15313        10392     7580      7562
				// index:   0         1   2            3         4         5
				Integer id = new Integer(entries[0].trim());
				Node o_node = this.network.getNode(entries[1].trim());
				Node d_node = this.network.getNode(entries[2].trim());

				LinkedList<Link> links = new LinkedList<Link>();
				int idx = 3;
				Node curr_node = o_node;
				while (!entries[idx].isEmpty()) {
					String orig_id = entries[idx].trim();
					boolean link_found = false;
					Iterator<? extends Link> outlink_it = curr_node.getOutLinks().values().iterator();
					while (outlink_it.hasNext()) {
						Link outlink = outlink_it.next();
						if (outlink.getOrigId().equals(orig_id)) {
							link_found = true;
							links.add(outlink);
						}
					}
					if (link_found) {
						curr_node = links.getLast().getToNode();
					}
					else {
//						System.out.println("id=" + id + ": no link found from node id=" + curr_node.getId() + " and link orig_id=" + orig_id);
						links.clear();
						break;
					}
					idx++;
				}
				if (links.isEmpty()) {
//					System.out.println("id=" + id + ": route not found in the network!");
				}
				else {
					if (links.getLast().getToNode() != d_node) {
						Gbl.errorMsg("id=" + id + ": Destination node not correct!");
					}
//					System.out.println(curr_line);
					System.out.print(id + "\t" + o_node.getId() + "\t" + d_node.getId());
					Iterator<Link> l_it = links.iterator();
					while (l_it.hasNext()) {
						System.out.print("\t" + l_it.next().getId());
					}
					System.out.print("\n");
//					System.out.print("\n");
				}
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////
	
	public final void writeData(String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("<greentimefractions desc=\"based on LSA data from City of Zurich\">\n");
			out.flush();
			out.write("</greentimefractions>\n");
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Scenario.setUpScenarioConfig();

		new RoutesForGianluca();

		Gbl.printElapsedTime();
	}
}
