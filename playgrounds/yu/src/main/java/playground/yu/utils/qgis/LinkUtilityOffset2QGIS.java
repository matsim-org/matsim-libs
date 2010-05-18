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
import java.util.Set;

import org.matsim.api.core.v01.Id;

import playground.yu.utils.io.LinkUtilityOffsetsReader;

/**
 * @author yu
 * 
 */
public class LinkUtilityOffset2QGIS implements X2QGIS {
	// public static class LinkUtilityOffset2PolygonGraph extends
	// Network2PolygonGraph {
	//
	// public LinkUtilityOffset2PolygonGraph(NetworkLayer network,
	// CoordinateReferenceSystem crs) {
	// super(network, crs);
	// }
	//
	// protected double getLinkWidth(Link link) {
	// Integer i = (Integer) parameters.get(0).get(link.getId());
	// return (i.intValue()) * 1e4;
	// }
	//
	// @Override
	// public Collection<Feature> getFeatures() throws SchemaException,
	// NumberFormatException, IllegalAttributeException {
	// for (int i = 0; i < attrTypes.size(); i++)
	// defaultFeatureTypeFactory.addType(attrTypes.get(i));
	// FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();
	// for (Link link : this.links2paint == null ? this.network.getLinks()
	// .values() : this.links2paint) {
	// LinearRing lr = getLinearRing(link);
	// Polygon p = new Polygon(lr, null, this.geofac);
	// MultiPolygon mp = new MultiPolygon(new Polygon[] { p },
	// this.geofac);
	// int size = 9 + parameters.size();
	// Object[] o = new Object[size];
	// o[0] = mp;
	// o[1] = link.getId().toString();
	// o[2] = link.getFromNode().getId().toString();
	// o[3] = link.getToNode().getId().toString();
	// o[4] = link.getLength();
	// o[5] = link.getCapacity() / network.getCapacityPeriod()
	// * 3600.0;
	// o[6] = (((LinkImpl) link).getType() != null) ? Integer
	// .parseInt(((LinkImpl) link).getType()) : 0;
	// o[7] = link.getFreespeed();
	// o[8] = link.getAllowedModes();
	// for (int i = 0; i < parameters.size(); i++) {
	// o[i + 8] = parameters.get(i).get(link.getId());
	// }
	// // parameters.get(link.getId().toString()) }
	// Feature ft = ftRoad.create(o, "network");
	// features.add(ft);
	// }
	// return features;
	// }
	// }

	// ------------------------------------------------------------------------
	private Map<Integer/* timeBin */, Map<Id/* linkId */, Double/* utiliyOffset */>> linkUtiliyOffsets = new HashMap<Integer/* timeBin */, Map<Id/* linkId */, Double/* utiliyOffset */>>();
	private Set<Integer> timBins;

	public Set<Integer> getTimBins() {
		return timBins;
	}

	public Map<Integer, Map<Id, Double>> getLinkUtiliyOffsets() {
		return linkUtiliyOffsets;
	}

	public LinkUtilityOffset2QGIS(String utilityOffsetsFilename) {
		LinkUtilityOffsetsReader reader = new LinkUtilityOffsetsReader(
				utilityOffsetsFilename);
		reader.parse();
		this.linkUtiliyOffsets = reader.getLinkUtiliyOffsets();
		timBins = this.linkUtiliyOffsets.keySet();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../integration-demandCalibration1.0.1/test/input/calibration/CalibrationTest/testLogLikelihood/network.xml", //
		utilityOffsetsFilename = "../integration-demandCalibration1.0.1/test/output/prepare/linkIdTimeBinX.log";

		MATSimNet2QGIS net2qgis = new MATSimNet2QGIS(netFilename, gk4);
		LinkUtilityOffset2QGIS luos2qgis = new LinkUtilityOffset2QGIS(
				utilityOffsetsFilename);
		for (Integer timeBin : luos2qgis.getTimBins()) {
			net2qgis.addParameter(timeBin + "hour", Double.class, luos2qgis
					.getLinkUtiliyOffsets().get(timeBin));

		}
		net2qgis
				.writeShapeFile("../integration-demandCalibration1.0.1/test/output/calibration/CalibrationTest/testLogLikelihood/linkUtilityOffset.shp");
	}

}
