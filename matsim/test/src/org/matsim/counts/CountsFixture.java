/* *********************************************************************** *
 * project: org.matsim.*
 * CountsFixture.java
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

package org.matsim.counts;

import java.util.List;
import java.util.Vector;

import org.matsim.analysis.CalcLinkStats;
import org.matsim.basic.v01.Id;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;


public class CountsFixture {

	public void setUp() {
		String[] args={"test/input/org/matsim/counts/config.xml"};
		org.matsim.config.Config config = Gbl.createConfig(args);

		MatsimCountsReader counts_parser = new MatsimCountsReader(Counts.getSingleton());
		counts_parser.readFile(config.counts().getCountsFileName());
	}

	public CountsComparisonAlgorithm getCCA() {
		CalcLinkStats linkStats = new AttributeFactory().createLinkStats();
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);

		network.createNode("0", "2.0", "1.0", "test");
		network.createNode("1", "1.0", "1.0", "test");
		/* String id,  String from,  String to, String length, String freespeed,
		 *    String capacity,  String permlanes, String origid, final String type)  */
		network.createLink("100", "0", "1", "3", "1.0", "1.0", "1", "0", "test" );
		CountsComparisonAlgorithm cca= new CountsComparisonAlgorithm(linkStats, Counts.getSingleton(), network);
		cca.setDistanceFilter(100.0, "0");
		return cca;
	}

	public List<CountSimComparison> ceateCountSimCompList() {

		List<CountSimComparison> csc_l=new Vector<CountSimComparison>();
		for (int i=0; i<24; i++) {
			csc_l.add(new CountSimComparisonImpl(new Id(i+1), 1, 1.0, 1.0));
		}

		return csc_l;
	}


}
