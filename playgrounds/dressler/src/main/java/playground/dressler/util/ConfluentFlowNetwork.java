/* *********************************************************************** *
 * project: org.matsim.*
 * ConfluentFlowNetwork.java
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


package playground.dressler.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;

/**
 * @author Daniel Dressler, Manuel Schneider
 *
 */
public class ConfluentFlowNetwork {

	public static boolean _debug =false;


	public static void debug(boolean debug){
		_debug=debug;
	}

	/**
	 * rounds network to given cap period, adjusting flowcap etc
	 * travel times are recalculated (in seconds)
	 * @param network
	 * @param newcap
	 */
	public static void roundNetwork(NetworkLayer network,int newcap, double flowCapacityFactor, double lengthFactor){
		double oldcap = network.getCapacityPeriod();
		int roundedtozerocap = 0;
		int roundedtozerotime = 0;
		if(_debug){
			System.out.println(oldcap);
		}
		double divisor = oldcap/newcap;
		if(_debug){
			System.out.println(divisor);
		}
		for (LinkImpl link : network.getLinks().values()){

			link.setLength(link.getLength()*lengthFactor);
			//link.setLength(link.getEuklideanDistance()*lengthFactor);

			double newTravelTime = link.getLength() / link.getFreespeed();

			newTravelTime = Math.round(newTravelTime); // to seconds

			double newspeed;
			if (newTravelTime == 0.) {
				newspeed = 999999999999.;
			} else {
			  newspeed = link.getLength() / newTravelTime;
			}

			if(_debug){
				System.out.println("old v: "+link.getFreespeed()+" new v: "+newspeed);
			}
			link.setFreespeed(newspeed);

			double newcapacity =Math.round(link.getCapacity()/divisor*flowCapacityFactor);

			if (newcapacity == 0d && link.getCapacity() != 0d) roundedtozerocap++;
			if (Math.round(link.getLength()/link.getFreespeed(0)) == 0) {
				System.out.println(link.getId());
				roundedtozerotime++;
			}

			if(_debug){
				System.out.println("old c: "+link.getCapacity()+" new c: "+newcapacity);
			}

			link.setCapacity(newcapacity);
		}
		network.setCapacityPeriod(newcap);
		//if (_debug){
			System.out.println("Edge capacities rounded to zero: " + roundedtozerocap);
			System.out.println("Transit times rounded to zero: " + roundedtozerotime);
		//}
	}

	public static NetworkLayer roundNetwork(String filename, int newcap, double flowCapacityFactor, double lengthFactor){
		//read network
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(filename);
		System.out.println("Network stats: Nodes = " + network.getNodes().size() + ", Edges = " + network.getLinks().size());
		roundNetwork(network, newcap, flowCapacityFactor, lengthFactor);
		return network;

	}

	public static void writeCFdata (String outfile, NetworkLayer network, HashMap<Node,Integer> demands){
		FileWriter fout;
        try {
            fout = new FileWriter(outfile);
            BufferedWriter out = new BufferedWriter(fout);
            out.write("% generated from matsim data");
            out.newLine();
            out.write("N " + network.getNodes().size());
            out.newLine();

            HashMap<Node,Integer> newNodeNames = new HashMap<Node,Integer>();
            int max = 0;
            for (Node node : network.getNodes().values()) {
            	try {
            		int i = Integer.parseInt(node.getId().toString());
            		if (i > 0) 	newNodeNames.put(node,i);
            		if (i > max) max = i;
            	} catch (Exception except) {

                }
            }

            for (Node node : network.getNodes().values()) {
            	try {
            		int i = Integer.parseInt(node.getId().toString());
            	} catch (Exception except) {
            		max += 1;
                    newNodeNames.put(node, max);
                    out.write("% node " + max + " was '" + node.getId()+ "'");
                    out.newLine();
                }
            }

            for (Node node : network.getNodes().values()) {
            	if (demands.containsKey(node)) {
            		int d = demands.get(node);
            		if (d > 0) {
            			out.write("S " + newNodeNames.get(node) + " " + d);
            			out.newLine();
            		}
            		if (d < 0) {
            			out.write("T " + newNodeNames.get(node) + " " + (-d));
            			out.newLine();
            		}
            	}
            }

            for (LinkImpl link : network.getLinks().values()) {
                out.write("E " + (newNodeNames.get(link.getFromNode())) + " " + (newNodeNames.get(link.getToNode())) + " " + (int) link.getCapacity());
                out.newLine();
            }

            out.close();
            fout.close();
        } catch (Exception except) {
            System.out.println(except.getMessage());
        }

    }



   private static HashMap<Node,Integer> readDemands(final NetworkLayer network, final String filename) throws IOException{
	BufferedReader in = new BufferedReader(new FileReader(filename));
	HashMap<Node,Integer> demands = new HashMap<Node,Integer>();
	String inline = null;
	while ((inline = in.readLine()) != null) {
		String[] line = inline.split(";");
		Node node = network.getNodes().get(new IdImpl(line[0].trim()));
		Integer d = Integer.valueOf(line[1].trim());
		demands.put(node, d);
	}
	return demands;
   }

	public static void main(String[] args){

		int cap = 60;
		double flowCapacityFactor = 1.; // 12*3600.0d / 60.0d;
		double lengthFactor = 1.0;

		//String inputfile  = "/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/siouxfalls_network.xml";
		String inputfile  = "/homes/combi/dressler/V/code/workspace/matsim/examples/meine_EA/swissold_network_5s.xml";
		//String inputfile = "/homes/combi/dressler/V/Project/testcases/swiss_old/matsimevac/swiss_old_network_evac.xml";
		//String inputfile = "./examples/meine_EA/siouxfalls_network.xml";

		String plansfile = null;
		int uniformDemands = 5;


		String outputfile_forCF = null;
		outputfile_forCF  = "/homes/combi/dressler/F/Confluent_Flow/code/frommatsim.dat";

		NetworkLayer network = roundNetwork(inputfile, cap, flowCapacityFactor, lengthFactor);

		HashMap<Node,Integer> demands = new HashMap<Node, Integer>();

		//Node sink = network.getNode("supersink");
		Node sink = network.getNodes().get(new IdImpl("en1"));

		if (plansfile != null) {
			try {
			  demands = readDemands(network, plansfile);
		    } catch (Exception except) {
              System.out.println(except.getMessage());
           }
		} else {
			for (Node node : network.getNodes().values()) {
		//		if (!node.getId().equals(sink.getId())) {
					demands.put(node, Math.max(uniformDemands,0));
		//		}
			}
		}

		demands.put(sink, -1);

		// TODO HACK, REMOVE ME
		/*int verticestokeep = 300;
		int count = 0;
		LinkedList<Node> todelete = new LinkedList<Node>();
		for (Node node : network.getNodes().values()) {
			count += 1;
			if (count > verticestokeep) {
				if (node != sink)
				  todelete.add(node);
			}
		}

		for (Node node : todelete) {
			network.removeNode(node);
		}*/

		writeCFdata (outputfile_forCF, network, demands);

	}
}
