/* *********************************************************************** *
 * project: org.matsim.*
 * DoublePermlanesNetwork.java
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

/**
 * 
 */
package playground.yu.newNetwork;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;

/**
 * @author yu
 * 
 */
public class DoublePermlanesNetwork {
	//
	// /**
	// * @param network
	// */
	// public DoublePermlanesNetwork(NetworkLayer network) {
	// super(network);
	// // TODO Auto-generated constructor stub
	// }
	//
	// /**
	// * @param network
	// * @param filename
	// */
	// public DoublePermlanesNetwork(NetworkLayer network, String filename) {
	// super(network, filename);
	// // TODO Auto-generated constructor stub
	// }

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String inputNetFilename = "../schweiz-ivtch/network/ivtch-changed-wu.xml";
		final String outputNetFilename = "../schweiz-ivtch/tmp/ivtch-changed-wu.xml";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputNetFilename);
		new NetworkWriter(network).writeFile(outputNetFilename);
	}

}
