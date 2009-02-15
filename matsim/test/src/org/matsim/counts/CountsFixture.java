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
import org.matsim.basic.v01.IdImpl;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

public class CountsFixture {

	private NetworkLayer network;
	public final Counts counts = new Counts();

	public NetworkLayer getNetwork() {
		return this.network;
	}

	public void setUp() {
		String[] args = {"test/input/org/matsim/counts/config.xml"};
		org.matsim.config.Config config = Gbl.createConfig(args);

		MatsimCountsReader counts_parser = new MatsimCountsReader(this.counts);
		counts_parser.readFile(config.counts().getCountsFileName());

		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(config.network().getInputFile());
	}

	public CountsComparisonAlgorithm getCCA() {
		CalcLinkStats linkStats = new AttributeFactory().createLinkStats(this.network);
		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(linkStats, this.counts, this.network);
		cca.setDistanceFilter(100.0, "0");
		return cca;
	}

	public List<CountSimComparison> ceateCountSimCompList() {
		List<CountSimComparison> csc_l = new Vector<CountSimComparison>(24);
		for (int i=0; i<24; i++) {
			csc_l.add(new CountSimComparisonImpl(new IdImpl(100), i+1, 1.0, 1.0));
		}
		return csc_l;
	}
}
