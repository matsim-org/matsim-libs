/* *********************************************************************** *
 * project: org.matsim.*
 * CapacityTest.java
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

package playground.yu.test;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.IOUtils;

public class CapacityTest {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch/network/ivtch-osm-wu.xml";
		final String outputFilename = "test/yu/test/captest.txt";
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outputFilename);
			for (Link link : network.getLinks().values()) {
				out
						.write(link.getId()
								+ "\t"
								+ link
										.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME)
								+ "\n");
				out.flush();
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
