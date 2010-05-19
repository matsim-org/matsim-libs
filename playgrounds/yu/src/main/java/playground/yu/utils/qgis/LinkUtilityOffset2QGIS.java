/* *********************************************************************** *
 * project: org.matsim.*
 * UtiliyOffset2QGIS.java
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.yu.utils.io.LinkUtilityOffsetsReader;

/**
 * @author yu
 * 
 */
public class LinkUtilityOffset2QGIS implements X2QGIS {

	private Map<Integer/* timeBin */, Map<Id/* linkId */, Double/* utiliyOffset */>> linkUtiliyOffsets = new HashMap<Integer, Map<Id, Double>>();
	// private Set<Integer> timBins;
	private Map<String/* linkId */, Id/* linkId */> stringIds = new HashMap<String, Id>();
	private Network network;

	// public Set<Integer> getTimBins() {
	// return timBins;
	// }

	public Map<Integer, Map<Id, Double>> getLinkUtiliyOffsets() {
		return linkUtiliyOffsets;
	}

	public LinkUtilityOffset2QGIS(String utilityOffsetsFilename, Network network) {
		this.network = network;
		LinkUtilityOffsetsReader reader = new LinkUtilityOffsetsReader(
				utilityOffsetsFilename);
		reader.parse();
		this.linkUtiliyOffsets = reader.getLinkUtiliyOffsets();
		// timBins = this.linkUtiliyOffsets.keySet();
		this.RationalizeIds();
	}

	private void RationalizeIds() {
		for (Id linkId : this.network.getLinks().keySet())
			this.stringIds.put(linkId.toString(), linkId);
		// end for
		Map<Integer, Map<Id, Double>> tmpLinkUtiliyOffsets = new HashMap<Integer, Map<Id, Double>>();
		for (Integer timeBin : this.linkUtiliyOffsets.keySet()) {
			Map<Id, Double> linkIdUO = this.linkUtiliyOffsets.get(timeBin);
			Map<Id, Double> rationalizedlinkIdUO = new HashMap<Id, Double>();
			for (Entry<Id, Double> linkIdUOEntry : linkIdUO.entrySet()) {
				rationalizedlinkIdUO.put(this.stringIds.get(linkIdUOEntry
						.getKey().toString()), linkIdUOEntry.getValue());
			}
			tmpLinkUtiliyOffsets.put(timeBin, rationalizedlinkIdUO);
		}
		this.linkUtiliyOffsets.putAll(tmpLinkUtiliyOffsets);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../integration-demandCalibration1.0.1/test/input/calibration/CalibrationTest/testLogLikelihood/network.xml", //
		utilityOffsetsFilename = "../integration-demandCalibration1.0.1/test/output/prepare/linkIdTimeBinX.log";

		MATSimNet2QGIS net2qgis = new MATSimNet2QGIS(netFilename, gk4);
		LinkUtilityOffset2QGIS luos2qgis = new LinkUtilityOffset2QGIS(
				utilityOffsetsFilename, net2qgis.getNetwork());
		for (Integer timeBin : luos2qgis.getLinkUtiliyOffsets().keySet()) {
			net2qgis.addParameter(timeBin + "hour", Double.class, luos2qgis
					.getLinkUtiliyOffsets().get(timeBin));
		}
		net2qgis
				.writeShapeFile("../integration-demandCalibration1.0.1/test/output/calibration/CalibrationTest/testLogLikelihood/linkUtilityOffset.shp");
	}

}
