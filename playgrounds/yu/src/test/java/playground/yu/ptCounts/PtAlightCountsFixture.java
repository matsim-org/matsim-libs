/* *********************************************************************** *
 * project: org.matsim.*
 * PtBoardCountsFixture.java
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

package playground.yu.ptCounts;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.yu.counts.pt.PtAlightCountComparisonAlgorithm;
import playground.yu.counts.pt.PtCountsComparisonAlgorithm;

public class PtAlightCountsFixture extends PtCountsFixture {

	public PtAlightCountsFixture() {
		super("inputAlightCountsFile");
	}

	@Override
	public PtCountsComparisonAlgorithm getCCA() {
		Map<Id, int[]> alights = new HashMap<Id, int[]>();

		int[] alightArrayStop3 = new int[24];
		alightArrayStop3[8] = 50;
		alights.put(new IdImpl("stop3"), alightArrayStop3);

		int[] alightArrayStop4 = new int[24];
		alightArrayStop4[8] = 15;
		alights.put(new IdImpl("stop4"), alightArrayStop4);

		this.oa.setAlights(alights);
		PtCountsComparisonAlgorithm cca = new PtAlightCountComparisonAlgorithm(
				oa, counts, network, Double.parseDouble(config.findParam(
						MODULE_NAME, "countsScaleFactor")));
		cca.setDistanceFilter(Double.valueOf(config.findParam(MODULE_NAME,
				"distanceFilter")), config.findParam(MODULE_NAME,
				"distanceFilterCenterNode"));
		return cca;
	}
}
