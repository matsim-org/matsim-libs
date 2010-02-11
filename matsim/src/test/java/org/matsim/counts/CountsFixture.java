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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

public class CountsFixture {

	private Network network;
	private Scenario scenario;
	public final Counts counts = new Counts();

	public Network getNetwork() {
		return this.network;
	}

	public void setUp() {
		String configFile = "test/input/org/matsim/counts/config.xml";

		this.scenario = new ScenarioLoaderImpl(configFile).getScenario();
		Config config = scenario.getConfig();

		MatsimCountsReader counts_parser = new MatsimCountsReader(this.counts);
		counts_parser.readFile(config.counts().getCountsFileName());

		this.network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
	}

	public CountsComparisonAlgorithm getCCA() {
		CalcLinkStats linkStats = new AttributeFactory().createLinkStats(this.network);
		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(linkStats, this.counts, this.network, 1.0);
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
