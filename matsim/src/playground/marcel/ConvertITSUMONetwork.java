/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertITSUMONetwork.java
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

package playground.marcel;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;

import playground.andreas.itsumo.ITSUMONetworkReader;

public class ConvertITSUMONetwork {

	public static void run(String[] args) {
		Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		ITSUMONetworkReader reader = new ITSUMONetworkReader(network);
		reader.read(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		System.out.println("  writing the network...");
		NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write();
		System.out.println("  done.");

		System.out.println("convertITSUMONetwork finished.");
		System.out.println();			
	}
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		run(args);
	}
}
