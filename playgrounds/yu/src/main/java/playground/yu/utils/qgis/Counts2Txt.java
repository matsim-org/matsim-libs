/* *********************************************************************** *
 * project: org.matsim.*
 * Counts2Txt.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.utils.qgis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.utils.io.SimpleWriter;

/**
 * extracts txt file from countsfile, which could also be insert in QGIS file
 * 
 * @author yu
 * 
 */
public class Counts2Txt {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFilename = "../integration-demandCalibration1.0.1/test/input/calibration/CalibrationTest/testLogLikelihood/network.xml", //
		countsFilename = "../integration-demandCalibration1.0.1/test/input/calibration/CalibrationTest/testLogLikelihood/counts1k.xml", //
		outputFilename = "../integration-demandCalibration1.0.1/test/input/calibration/CalibrationTest/testLogLikelihood/counts1k.txt";

		Scenario snr = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = snr.getNetwork();
		new MatsimNetworkReader(snr).readFile(networkFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		SimpleWriter writer = new SimpleWriter(outputFilename);
		writer.writeln("x\ty\tlocId");

		for (Count count : counts.getCounts().values()) {
			Coord coord = count.getCoord();
			if (coord == null)
				coord = net.getLinks().get(count.getLocId()).getCoord();

			writer.writeln(coord.getX() + "\t" + coord.getY() + "\t"
					+ count.getLocId());
		}

		writer.close();
	}
}
