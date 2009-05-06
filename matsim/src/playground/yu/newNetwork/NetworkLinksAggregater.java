/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkLinksAggregater.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * @author yu
 * 
 */
public class NetworkLinksAggregater implements Runnable {
	private NetworkLayer net = null;

	public NetworkLinksAggregater(String oldNetFilename, String newNetFilename) {
		net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(oldNetFilename);
	}

	public void run() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new NetworkLinksAggregater("", "").run();
	}

}
