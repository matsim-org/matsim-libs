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

import playground.yu.counts.pt.PtCountsComparisonAlgorithm;
import playground.yu.counts.pt.PtOccupancyCountComparisonAlgorithm;

public class PtOccupancyCountsFixture extends PtCountsFixture {

	public PtOccupancyCountsFixture() {
		super("inputOccupancyCountsFile");
	}

	@Override
	public PtCountsComparisonAlgorithm getCCA() {
		Map<Id, int[]> occupancies = new HashMap<Id, int[]>();

		int[] occupancyArrayStop1 = new int[24];
		occupancyArrayStop1[8] = 65;
		occupancies.put(new IdImpl("stop1"), occupancyArrayStop1);

		int[] occupancyArrayStop2 = new int[24];
		occupancyArrayStop2[8] = 65;
		occupancies.put(new IdImpl("stop2"), occupancyArrayStop2);

		int[] occupancyArrayStop3 = new int[24];
		occupancyArrayStop3[8] = 15;
		occupancies.put(new IdImpl("stop3"), occupancyArrayStop3);

		int[] occupancyArrayStop4 = new int[24];
		occupancies.put(new IdImpl("stop4"), occupancyArrayStop4);

		this.oa.setOccupancies(occupancies);
		PtCountsComparisonAlgorithm cca = new PtOccupancyCountComparisonAlgorithm(
				oa, counts, network, Double.parseDouble(config.findParam(
						MODULE_NAME, "countsScaleFactor")));
		cca.setDistanceFilter(Double.valueOf(config.findParam(MODULE_NAME,
				"distanceFilter")), config.findParam(MODULE_NAME,
				"distanceFilterCenterNode"));
		return cca;
	}
}
