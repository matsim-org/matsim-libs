/* *********************************************************************** *
 * project: org.matsim.*
 * PtCountsFixture.java
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

package playground.yu.ptCounts;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.yu.analysis.pt.OccupancyAnalyzer;
import playground.yu.counts.pt.PtCountsComparisonAlgorithm;

/**
 * this is modified copy of {@code CountsFixture}
 * 
 * @author yu
 * 
 */
public abstract class PtCountsFixture {
	// @Rule
	// public MatsimTestUtils testUtils = new MatsimTestUtils();
	protected Config config;
	protected Network network;
	private Scenario scenario;
	protected Counts counts = new Counts();
	private String countFileParamName;
	protected OccupancyAnalyzer oa = new OccupancyAnalyzer(3600, 24 * 3600 - 1);

	public PtCountsFixture(String countFileParamName) {
		this.countFileParamName = countFileParamName;
	}

	final protected String MODULE_NAME = "ptCounts";

	public Network getNetwork() {
		return this.network;
	}

	public void setUp() {
		String configFile = // 
//		"../playgrounds/yu/" + // should be commented out before committed
				"test/input/playground/yu/ptCounts/config.xml";

		this.scenario = new ScenarioLoaderImpl(configFile).getScenario();
		config = scenario.getConfig();

		new MatsimCountsReader(this.counts).readFile(config.findParam(
				MODULE_NAME, countFileParamName));

		this.network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network()
				.getInputFile());
	}

	public abstract PtCountsComparisonAlgorithm getCCA();

	public List<CountSimComparison> ceateCountSimCompList() {
		List<CountSimComparison> csc_l = new Vector<CountSimComparison>(24);
		for (int i = 0; i < 24; i++) {
			csc_l.add(new CountSimComparisonImpl(new IdImpl(100), i + 1, 1.0,
					1.0));
		}
		return csc_l;
	}
}
