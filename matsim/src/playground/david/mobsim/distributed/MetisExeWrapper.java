/* *********************************************************************** *
 * project: org.matsim.*
 * MetisExeWrapper.java
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

package playground.david.mobsim.distributed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class MetisExeWrapper {

	public static ArrayList<Integer> decomposeNetwork (QueueNetworkLayer network, int parts, String networkID) throws IOException {
		if (parts > 1) {
			writeNetworkToFile(network, networkID);
			callMetisExe(networkID, parts);
			return readPartitionInfoFromFile(network, networkID, parts);
		}
		return null;
	}

	public static ArrayList<Integer> readPartitionInfoFromFile(QueueNetworkLayer network, String myID, int parts) throws IOException {
		int count = 1;
		TreeMap<Integer, Id> IDToInt = new TreeMap<Integer, Id>();
		for (Node node : network.getNodes().values()) {
			IDToInt.put(count++, node.getId());
		}

		ArrayList<Integer> test = new ArrayList<Integer>(count);
		count = 1;
		String filename = "MetiscompatibleGraph"+myID+".txt.part." + parts;
		BufferedReader in = null;
		try {
			in = new BufferedReader( new FileReader(filename));
			for (String s; (s = in.readLine()) != null; ) {
				int partition = Integer.parseInt(s);
				QueueNode node = (QueueNode)network.getNode(IDToInt.get(count).toString());
				//System.out.println("METIS mapped node: " + IDToInt.get(count).toString() + " to partition " + partition);
				// Set nodes Partition info
				node.setPartitionId(partition);
				test.add(partition);
				count++;
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
		count = 0;
		int linkcount = 0;
		int fromID = 0, toID = 0;
		for (QueueLink link : network.getLinks().values()) {
			linkcount++;
			fromID = ((QueueNode)link.getFromNode()).getPartitionId();
			toID = ((QueueNode)link.getToNode()).getPartitionId();

			if (fromID != toID) {
				count++;
				//System.out.println("Partition link " + count + " has freetravelduration of  " + link.getFreeTravelDuration()+ " s");
			}
		}
		System.out.println("METIS found " + count + " links of " + linkcount + " links on boundary of the " + parts + " partitions, that is " + count*100/linkcount + " %");
		return test;
	}

	public static boolean callMetisExe(String myID, int parts) throws IOException {
		String filename = "MetiscompatibleGraph"+myID+".txt " + parts ;
		Process p = Runtime.getRuntime().exec( "./kmetis.exe " + filename);
		BufferedReader in = null;
		try {
			in = new BufferedReader( new InputStreamReader(p.getInputStream()) );
			for (String s; (s = in.readLine()) != null; ) System.out.println( s );

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
		System.out.println( "Return value: " + p.exitValue() );
		return p.exitValue()==0;
	}

	public static void writeNetworkToFile(NetworkLayer network, String myID) throws IOException {
		TreeMap<Id, Integer> IDToInt = new TreeMap<Id, Integer>();
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter("MetiscompatibleGraph"+myID+".txt"));

			// write node, link count in first row
			out.write(" " + network.getNodes().size() + " " + network.getLocations().size() + "\n");

			int count = 1;
			// make up a mapping of node IDs to their position in the array
			for (Node node : network.getNodes().values()) {
				IDToInt.put(node.getId(), count++);
			}

			// Save every node with all its connected nodes
			for (Node node : network.getNodes().values()) {
				// save all incoming nodes
				for (Node node2 : node.getInNodes().values()) {
					int nodeCount = IDToInt.get(node2.getId());
					out.write(" " + nodeCount);
				}
				// write outgoing nodes
				for (Node node2 : node.getOutNodes().values()) {
					int nodeCount = IDToInt.get(node2.getId());
					out.write(" " + nodeCount);
				}
				out.write(" \n");
			}
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

}
