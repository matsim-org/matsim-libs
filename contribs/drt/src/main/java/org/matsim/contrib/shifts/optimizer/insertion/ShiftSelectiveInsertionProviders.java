package org.matsim.contrib.shifts.optimizer.insertion;

import java.util.concurrent.ForkJoinPool;

import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimeEstimator;
import org.matsim.contrib.drt.optimizer.insertion.SelectiveInsertionProvider;
import org.matsim.contrib.drt.optimizer.insertion.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class ShiftSelectiveInsertionProviders {
	public static SelectiveInsertionProvider create(DrtConfigGroup drtCfg, MobsimTimer timer,
			CostCalculationStrategy costCalculationStrategy, DvrpTravelTimeMatrix dvrpTravelTimeMatrix,
			ForkJoinPool forkJoinPool) {
		var insertionParams = (SelectiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
		var restrictiveDetourTimeEstimator = DetourTimeEstimator.createFreeSpeedZonalTimeEstimator(
				insertionParams.getRestrictiveBeelineSpeedFactor(), dvrpTravelTimeMatrix);
		return new SelectiveInsertionProvider(restrictiveDetourTimeEstimator, forkJoinPool,
				new ShiftInsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy, Double::doubleValue,
						restrictiveDetourTimeEstimator));
	}
}
