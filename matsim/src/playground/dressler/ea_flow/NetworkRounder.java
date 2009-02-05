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


package playground.dressler.ea_flow;

import org.matsim.network.*;

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
	public static void roundNetwork(NetworkLayer network,int newcap){
		double oldcap = network.getCapacityPeriod();
		if(_debug){
			System.out.println(oldcap);
		}
		double divisor = oldcap/newcap;
		if(_debug){
			System.out.println(divisor);
		}
		for (Link link : network.getLinks().values()){
			double newspeed = Math.round(link.getFreespeed(0.)*newcap);
			if(_debug){
				System.out.println("old v: "+link.getFreespeed(0.)+" new v: "+newspeed);
			}
			link.setFreespeed(newspeed);
			double newcapacity =Math.ceil(link.getCapacity(1.)/divisor);
			if(_debug){
				System.out.println("old c: "+link.getCapacity(1.)+" new c: "+newcapacity);
			}
			link.setCapacity(newcapacity);
			//link.setCapacity(Math.round(link.getCapacity(1.)/divisor));
		}
	}
	
	public static NetworkLayer roundNetwork(String filename, int newcap){
		//read network
		NetworkLayer network = new NetworkLayer();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
		networkReader.readFile(filename);
		roundNetwork(network, newcap);
		return network;
		
	}

	public static void main(String[] args){
		if (args.length!=3 && args.length!=1 && args.length!=0){
			System.out.println("USAGE: NetworkRounder <inputfile> <outputfile> <cap> OR JUST: NetworkRounder <cap>");
			return;
		}
		int cap =30;
		String inputfile  = "/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/siouxfalls_network.xml";
		String outputfile = "/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/siouxfalls_network_rounded_30.xml";
		if(args.length==3){
			inputfile  = args[0];
			outputfile = args[1];
			cap = Integer.valueOf(args[2]);
		}
		if (args.length == 1){
			cap =Integer.valueOf(args[0]);
		}
		NetworkLayer network = roundNetwork(inputfile,cap);
		NetworkWriter writer = new NetworkWriter( network, outputfile);
		writer.write();
		
	}
}
