package org.matsim.contrib.shifts.optimizer.insertion;

import java.util.concurrent.ForkJoinPool;

import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DefaultDrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimeEstimator;
import org.matsim.contrib.drt.optimizer.insertion.ExtensiveInsertionProvider;
import org.matsim.contrib.drt.optimizer.insertion.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class ShiftExtensiveInsertionProviders {
	public static ExtensiveInsertionProvider create(DrtConfigGroup drtCfg, MobsimTimer timer,
			CostCalculationStrategy costCalculationStrategy, DvrpTravelTimeMatrix dvrpTravelTimeMatrix,
			ForkJoinPool forkJoinPool) {
		var insertionParams = (ExtensiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
		var restrictiveDetourTimeEstimator = DetourTimeEstimator.createFreeSpeedZonalTimeEstimator(
				insertionParams.getAdmissibleBeelineSpeedFactor(), dvrpTravelTimeMatrix);
		return new ExtensiveInsertionProvider(drtCfg, restrictiveDetourTimeEstimator, forkJoinPool,
				new ShiftInsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy, Double::doubleValue,
						restrictiveDetourTimeEstimator));
	}
}

