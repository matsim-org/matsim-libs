package org.matsim.contrib.shifts.optimizer.insertion;

import org.matsim.contrib.drt.optimizer.insertion.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class ShiftDrtInsertionSearch extends DefaultDrtInsertionSearch implements DrtInsertionSearch<OneToManyPathSearch.PathData> {

    public ShiftDrtInsertionSearch(InsertionProvider insertionProvider, DetourPathCalculator detourPathCalculator,
                                   CostCalculationStrategy costCalculationStrategy, DrtConfigGroup drtCfg, MobsimTimer timer) {
        super(insertionProvider, detourPathCalculator, new BestInsertionFinder<>(
                new ShiftInsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy, OneToManyPathSearch.PathData::getTravelTime, null)));
    }
}
