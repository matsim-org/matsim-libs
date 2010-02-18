/* *********************************************************************** *
 * project: org.matsim.*
 * PtOccupancyCountComparisonAlgorithm.java
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

package playground.yu.counts.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.counts.Counts;

import playground.yu.analysis.pt.OccupancyAnalyzer;

public class PtOccupancyCountComparisonAlgorithm extends
		PtCountsComparisonAlgorithm {

	public PtOccupancyCountComparisonAlgorithm(OccupancyAnalyzer oa,
			Counts counts, Network net) {
		super(oa, counts, net);
	}

	@Override
	protected int[] getVolumesForStop(Id stopId) {
		return this.oa.getOccupancyVolumesForStop(stopId);
	}

}
