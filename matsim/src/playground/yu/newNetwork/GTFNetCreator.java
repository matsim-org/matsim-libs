/* *********************************************************************** *
 * project: org.matsim.*
 * GTFNetCreator.java
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

import java.util.HashMap;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;

import playground.yu.utils.io.GTFParser;

/**
 * @author yu
 * 
 */
public class GTFNetCreator {
	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch/network/ivtch-changed.xml";
		Gbl.createConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		GTFParser g = new GTFParser(
				new TreeMap<String, HashMap<Double, Double>>());
		g.readFile("../schweiz-ivtch/greentimes/ivtch.xml");

		for (String linkId : g.getLinkgtfsMap().keySet()) {
			Link l = network.getLink(linkId);
			if (l != null) {
				System.out
						.println("#a-->linkId:\t"
								+ linkId
								+ "\tcapacity:\t"
								+ l
										.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME));
				System.out.println("greentimefraction:\t"
						+ g.getAvgGtfs(linkId));
				l.setCapacity(l
						.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME)
						* g.getAvgGtfs(linkId));
				System.out.println("#b-->linkId:\t"
						+ linkId
						+ "\tcapacity:\t"
						+ network.getLink(linkId).getCapacity(
								org.matsim.utils.misc.Time.UNDEFINED_TIME));
				System.out.println("-------------------------");
			}
		}

		new NetworkWriter(network,
				"../schweiz-ivtch/network/ivtch-changed-with-GTF.xml").write();

		System.out.println("done!");
	}

}
