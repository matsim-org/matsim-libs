package org.matsim.contrib.drt.optimizer.rebalancing.toolbox;

import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;

/** Input: delta of each zone Output: Rebalance flow among zones */
public interface InterZonalRebalanceCalculator {
	List<Triple<DrtZone, DrtZone, Integer>> calculateInterZonalRebalanceFlow();
}
