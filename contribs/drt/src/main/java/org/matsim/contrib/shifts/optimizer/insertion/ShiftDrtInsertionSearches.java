package org.matsim.contrib.shifts.optimizer.insertion;

import org.matsim.contrib.drt.optimizer.insertion.BestInsertionFinder;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DefaultDrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.DetourPathCalculator;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class ShiftDrtInsertionSearches {
	public static DrtInsertionSearch<OneToManyPathSearch.PathData> createShiftDrtInsertionSearch(
			DefaultDrtInsertionSearch.InsertionProvider insertionProvider, DetourPathCalculator detourPathCalculator,
			CostCalculationStrategy costCalculationStrategy, DrtConfigGroup drtCfg, MobsimTimer timer) {
		return new DefaultDrtInsertionSearch(insertionProvider, detourPathCalculator, new BestInsertionFinder<>(
				new ShiftInsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy,
						OneToManyPathSearch.PathData::getTravelTime, null)));
	}
}
