/* *********************************************************************** *
 * project: org.matsim.*
 * FakeTravelTimeCost.java
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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;

/**
 * @author Manuel Schneider
 *
 */
public class NetworkRounder {

	public static boolean _debug =false;


	public static void debug(boolean debug){
		_debug=debug;
	}

	/**
	 * rounds networ will change the instance of network!!!!!
	 * @param network
	 * @param newcap
	 */
	public static void roundNetwork(NetworkLayer network,int newcap, double flowCapacityFactor, double lengthFactor, boolean forEAF){
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

			newTravelTime = Math.round(newTravelTime/newcap)*newcap;

			double newspeed;
			if (newTravelTime == 0.) {
				newspeed = 999999999999.;
			} else {
				if (forEAF) {
					newspeed = newcap * link.getLength() / newTravelTime;
				} else {
					newspeed = link.getLength() / newTravelTime;
				}
			}

			if(_debug){
				System.out.println("old v: "+link.getFreespeed()+" new v: "+newspeed);
			}
			link.setFreespeed(newspeed);

			//double newcapacity =Math.ceil(link.getCapacity()/divisor*flowCapacityFactor);
			double newcapacity =Math.round(link.getCapacity()/divisor*flowCapacityFactor);

			if (newcapacity == 0d && link.getCapacity() != 0d) roundedtozerocap++;
			if (Math.round(link.getLength()/link.getFreespeed()) == 0) {
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

	public static NetworkLayer roundNetwork(String filename, int newcap, double flowCapacityFactor, double lengthFactor, boolean forEAF){
		//read network
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(filename);
		System.out.println("Network stats: Nodes = " + network.getNodes().size() + ", Edges = " + network.getLinks().size());
		roundNetwork(network, newcap, flowCapacityFactor, lengthFactor, forEAF);
		return network;

	}

	public static void main(String[] args){
//		if (args.length!=3 && args.length!=1 && args.length!=0){
//			System.out.println("USAGE: NetworkRounder <inputfile> <outputfile> <cap> OR JUST: NetworkRounder <cap>");
//			return;
//		}
		int cap = 60;
		double flowCapacityFactor = 1.; // 12*3600.0d / 60.0d;
		double lengthFactor = 1.0;
		String inputfile  = "/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/siouxfalls_network.xml";
		//String inputfile = "/homes/combi/dressler/V/Project/testcases/swiss_old/matsimevac/swiss_old_network_evac.xml";
		//String inputfile = "./examples/meine_EA/siouxfalls_network.xml";

		String outputfile_forEAF = null;
		String outputfile_forMatsim = null;
		//String outputfile = "./examples/meine_EA/siouxfalls_network_5s.xml";
		outputfile_forEAF  = "/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/siouxfalls_network_60s_EAF.xml";
		outputfile_forMatsim  = "/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/siouxfalls_network_60s_MATSIM.xml";
		//String outputfile = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac_100p_flow_2s_cap.xml";
		//String outputfile = "./examples/meine_EA/swissold_network_5s.xml";

//		if(args.length >=2){
//			inputfile  = args[0];
//			outputfile = args[1];
//			cap = Integer.valueOf(args[2]);
//		} if (args.length==4){
//			inputfile  = args[0];
//			outputfile = args[1];
//			cap = Integer.valueOf(args[2]);
//			flowCapacityFactor = Double.valueOf(args[3]);
//		}
//		if (args.length >= 5) {
//			lengthFactor = Double.valueOf(args[4]);
//		}
//
//		if (args.length == 1){
//			cap =Integer.valueOf(args[0]);
//		}

		if (outputfile_forEAF != null) {
		  NetworkLayer network = roundNetwork(inputfile,cap, flowCapacityFactor, lengthFactor, true);
		  new NetworkWriter(network).write(outputfile_forEAF);
		}
		if (outputfile_forMatsim != null) {
			  // Matsim needs the real transit time ("false") & capacity ("1.0d")
			  NetworkLayer network = roundNetwork(inputfile,cap, 1.0d, lengthFactor, false);
			  new NetworkWriter(network).write(outputfile_forMatsim);
		}
	}
}
