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

import org.matsim.interfaces.networks.basicNet.BasicNodeSetI;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNode;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.identifiers.IdI;

public class MetisExeWrapper {

	public static ArrayList<Integer> decomposeNetwork (NetworkLayer network, int parts, String networkID) throws IOException
	{
		if (parts > 1) {
			writeNetworkToFile(network, networkID);
			callMetisExe(networkID, parts);
			return readPartitionInfoFromFile(network, networkID, parts);
		}

		return null;
	}
	public static ArrayList<Integer> readPartitionInfoFromFile(NetworkLayer network, String myID, int parts) throws IOException {
		int count = 1;
		TreeMap<Integer, IdI> IDToInt = new TreeMap<Integer, IdI>();
		BasicNodeSetI nodes = network.getNodes();
		// make up a mapping of node IDs to their position in the array, but this time the other way round
		for (Object key : nodes) {
			Node node = (Node)key;
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
				QueueNode node = (QueueNode)nodes.get(IDToInt.get(count));
				//System.out.println("METIS mapped node: " + IDToInt.get(count).toString() + " to partition " + partition);
				// Set nodes Partition info
				node.setPartitionID(partition);
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
		for (Object iter : network.getLinks()) {
			QueueLink link = (QueueLink)iter;
			linkcount++;
			fromID = ((QueueNode)link.getFromNode()).getPartitionID();
			toID = ((QueueNode)link.getToNode()).getPartitionID();

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
		TreeMap<IdI, Integer> IDToInt = new TreeMap<IdI, Integer>();
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter("MetiscompatibleGraph"+myID+".txt"));
	
			BasicNodeSetI nodes = network.getNodes();
			// write node, link count in first row
			out.write(" " + nodes.size() + " " + network.getLocations().size() + "\n");
	
			int count = 1;
			// make up a mapping of node IDs to their position in the array
			for (Object key : nodes) {
				Node node = (Node)key;
				IDToInt.put(node.getId(), count++);
			}
	
			// Save every node with all its connected nodes
			for (Object key : nodes) {
				Node node = (Node)key;
				// save all incoming nodes
				for (Object key2 : node.getInNodes()) {
					Node node2 = (Node)key2;
					int nodeCount = IDToInt.get(node2.getId());
					out.write(" " + nodeCount);
				}
				// write outgoing nodes
				for (Object key2 : node.getOutNodes()) {
					Node node2 = (Node)key2;
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
