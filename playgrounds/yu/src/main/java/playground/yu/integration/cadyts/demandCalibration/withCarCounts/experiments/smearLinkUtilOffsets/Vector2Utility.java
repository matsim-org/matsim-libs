/* *********************************************************************** *
 * project: org.matsim.*
 * LinkUtilOffset2QGIS.java
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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.smearLinkUtilOffsets;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;

import playground.yu.utils.io.SimpleWriter;
import playground.yu.utils.qgis.MATSimNet2QGIS;
import playground.yu.utils.qgis.X2QGIS;

/**
 * added linkUtilityOffset value as attributes of links into network
 * 
 * @author yu
 * 
 */
public class Vector2Utility implements X2QGIS {
	private Network network;
	private int calibrationStartTimeBin, calibrationEndTimeBin;
	private SimpleWriter writer;
	private Map<Integer, Map<Id, Double>> timeBinLinkOffsets = null;

	public Vector2Utility(Network network, int calibrationStartTimeBin,
			int calibrationEndTimeBin, String vectorFilename,
			String utilityFilename, boolean toQGIS) {
		this.network = network;
		this.calibrationStartTimeBin = calibrationStartTimeBin;
		this.calibrationEndTimeBin = calibrationEndTimeBin;
		writer = new SimpleWriter(utilityFilename);
		writer.writeln("timeBin\tlinkId\tOffset");
		if (toQGIS) {
			timeBinLinkOffsets = new HashMap<Integer, Map<Id, Double>>();
		}

		Map<Integer/* line no. */, Double/* offset value */> vector = VectorUtils
				.readVector(vectorFilename);

		int idx = 0;
		for (Link link : this.network.getLinks().values()) {
			Id linkId = link.getId();
			String linkIdStr = linkId.toString();
			for (int tb = calibrationStartTimeBin; tb <= calibrationEndTimeBin; tb++) {
				Double value = vector.get(idx);
				if (value != null) {
					StringBuffer sb = new StringBuffer(Integer.toString(tb));
					sb.append('\t');
					sb.append(linkIdStr);
					sb.append('\t');
					sb.append(value);
					writer.writeln(sb);
					if (toQGIS) {
						Map<Id, Double> linkOffsets = timeBinLinkOffsets
								.get(tb);
						if (linkOffsets == null) {
							linkOffsets = new HashMap<Id, Double>();
							timeBinLinkOffsets.put(tb, linkOffsets);
						}
						linkOffsets.put(link.getId(), value);
					}
				} else {
					Logger.getLogger(Vector2Utility.class.getName()).info(
							"in vector, line\t" + idx + "\ti.e. link\t"
									+ linkIdStr + "\ttimeBin\t" + tb
									+ "\tvalue DOESN'T EXIST!!!");
					System.exit(1);
				}
				idx++;
			}
		}
		writer.close();
	}

	public void output2QGIS(Scenario scenario, String shapeFilenameBase) {

		for (Entry<Integer, Map<Id, Double>> timeBinLinkOffsetPair : timeBinLinkOffsets
				.entrySet()) {
			MATSimNet2QGIS mn2q = new MATSimNet2QGIS(scenario, ch1903);
			int time = timeBinLinkOffsetPair.getKey();

			StringBuffer sb = new StringBuffer(Integer.toString(time - 1));
			sb.append("-");
			sb.append(time);
			sb.append("hOffset");
			mn2q.addParameter(sb.toString(), Double.class,
					timeBinLinkOffsetPair.getValue());
			mn2q.writeShapeFile(shapeFilenameBase + time + ".shp");
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFilename = "/work/chen/data/ivtch/input/ivtch-osm.xml"//
		, vectorFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.x.log.gz"//
		, utilityFilename = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.timeBinLinkUtilOffsets.log.gz"//
		, shapeFilenameBase = "../matsim-bse/outputs/4SE_DC/middle_um1/ITERS/it.1000/test/1000.timeBinLinkUtilOffsets.";

		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		Network network = scenario.getNetwork();

		Vector2Utility v2u = new Vector2Utility(network, 7, 20, vectorFilename,
				utilityFilename, true);
		v2u.output2QGIS(scenario, shapeFilenameBase);
	}

}
