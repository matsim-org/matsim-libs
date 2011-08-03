/* *********************************************************************** *
 * project: org.matsim.*
 * LinkCostOffsets2QGISTest.java
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.utils.qgis.X2QGIS;
import cadyts.utilities.misc.DynamicData;

public class LinkCostOffsets2QGISTest {
	private static boolean isInRange(final Id linkId, final Network net) {
		Coord distanceFilterCenterNodeCoord = net.getNodes()
				.get(new IdImpl("2531")).getCoord();
		double distanceFilter = 30000;
		Link l = net.getLinks().get(linkId);
		if (l == null) {
			System.out.println("Cannot find requested link: "
					+ linkId.toString());
			return false;
		}
		return ((LinkImpl) l).calcDistance(distanceFilterCenterNodeCoord) < distanceFilter;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// MATSimNet2QGIS mn2q = new MATSimNet2QGIS();
		String netFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml.gz";
		// String netFilename = "examples/equil/network.xml";
		// String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";
		String linkCostOffsetFilename = "../../runs-svn/run1532/ITERS/it.1900/1532.1900.linkCostOffsets.xml";
		// String linkCostOffsetFilename =
		// "../matsimTests/Calibration/linkCostOffsets/output/ITERS/it.100/100.linkCostOffsets.xml";
		String countsFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/vmz_di-do.xml";
		// String countsFilename = "examples/equil/counts100.xml";
		String outputBase = "../../runs-svn/run1532/ITERS/it.1900/1532.1900.linkUtilOffsets.";
		// String outputBase =
		// "../matsimTests/Calibration/linkCostOffsets/output/ITERS/it.100/100.";
		String crs = X2QGIS.gk4;
		int arStartTime = 20/* 7 */;
		int arEndTime = /* 20 */20;

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(netFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		Collection<Link> countLinks = new HashSet<Link>();
		Set<Id> countLinkIds = new HashSet<Id>();
		// countLinkIds.addAll(counts.getCounts().keySet());
		// Set<Id> countLinkIds2remove = new HashSet<Id>();
		for (Id linkId : counts.getCounts().keySet()) {
			Link link = net.getLinks().get(linkId);
			if (link != null && isInRange(linkId, net)) {
				countLinks.add(link);
				countLinkIds.add(linkId);
			}
			// else
			// countLinkIds2remove.add(linkId);
		}
		// countLinkIds.removeAll(countLinkIds2remove);

		BseLinkCostOffsetsXMLFileIO reader = new BseLinkCostOffsetsXMLFileIO(
				net);
		System.out.println("-----parse begins (...Test)-----");
		DynamicData<Link> linkCostOffsets = reader.read(linkCostOffsetFilename);
		System.out.println("-----parse ends (...Test)-----");

		for (int i = arStartTime; i <= arEndTime; i++) {
			// LinkCostOffsets2QGIS lco2QGSI = new LinkCostOffsets2QGIS//
			LinkCostOffsets2QGISWithArrowhead lco2QGSI = new LinkCostOffsets2QGISWithArrowhead//
			(i, i, netFilename, crs);
			lco2QGSI.createLinkCostOffsets(countLinks, linkCostOffsets);
			lco2QGSI.output(countLinkIds, outputBase);
		}
	}
}
