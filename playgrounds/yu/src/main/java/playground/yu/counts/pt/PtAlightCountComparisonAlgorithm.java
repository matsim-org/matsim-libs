/* *********************************************************************** *
 * project: org.matsim.*
 * PtBoardCountComparisonAlgorithm.java
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
package playground.yu.counts.pt;

import org.matsim.api.core.v01.network.Network;
import org.matsim.counts.Count;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.yu.analysis.pt.OccupancyAnalyzer;

/**
 * @author yu
 * 
 */
public class PtAlightCountComparisonAlgorithm extends
		PtCountsComparisonAlgorithm {

	/**
	 * @param oa
	 * @param counts
	 * @param network
	 */
	public PtAlightCountComparisonAlgorithm(OccupancyAnalyzer oa,
			Counts counts, Network network) {
		super(oa, counts, network);
	}

	protected void compare() {
		double countValue;
		for (Count count : this.counts.getCounts().values()) {
			if (!isInRange(count.getCoord())) {
				System.out.println("InRange?\t" + isInRange(count.getCoord()));
				continue;
			}
			// -------------------------------------------------------------------
			int[] volumes = this.oa.getAlightVolumesForStop(count.getLocId());
			// ------------------------^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
			if (volumes == null) {
				log.warn("No volumes for stop: " + count.getLocId().toString());
				continue;
			} else /* volumes!=null */if (volumes.length == 0) {
				log.warn("No volumes for stop: " + count.getLocId().toString());
				continue;
			}
			for (int hour = 1; hour <= 24; hour++) {
				// real volumes:
				Volume volume = count.getVolume(hour);
				if (volume != null) {
					countValue = volume.getValue();
					double simValue = volumes[hour - 1];
					simValue *= this.countsScaleFactor;
					this.countSimComp.add(new CountSimComparisonImpl(count
							.getLocId(), hour, countValue, simValue));
				} else {
					countValue = 0.0;
				}
			}
		}
	}

}
