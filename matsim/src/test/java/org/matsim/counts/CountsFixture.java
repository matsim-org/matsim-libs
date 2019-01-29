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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CountsFixture {

	private Network network;
	private Scenario scenario;
	public final Counts counts = new Counts();

	public Network getNetwork() {
		return this.network;
	}

	public void setUp() {
		String configFile = "test/input/org/matsim/counts/config.xml";

		Config config = ConfigUtils.loadConfig(configFile);
		MatsimRandom.reset(config.global().getRandomSeed());
		this.scenario = ScenarioUtils.createScenario(config);

		MatsimCountsReader counts_parser = new MatsimCountsReader(this.counts);
		counts_parser.readFile(config.counts().getCountsFileName());

		this.network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
	}

	public CountsComparisonAlgorithm getCCA() {
		final CalcLinkStats linkStats = new AttributeFactory().createLinkStats(this.network);
		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(new CountsComparisonAlgorithm.VolumesForId() {
		
			@Override
			public double[] getVolumesForStop(Id<TransitStopFacility> locationId) {
				return linkStats.getAvgLinkVolumes(Id.create(locationId, Link.class));
			}
		
		}, this.counts, this.network,
				1.0);
	//	cca.setDistanceFilter(100.0, "0");
		return cca;
	}

	public List<CountSimComparison> ceateCountSimCompList() {
		List<CountSimComparison> csc_l = new Vector<CountSimComparison>(24);
		for (int i=0; i<24; i++) {
			csc_l.add(new CountSimComparisonImpl(Id.create(100, Link.class), "", i+1, 1.0, 1.0));
		}
		return csc_l;
	}
}
