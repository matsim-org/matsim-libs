package org.matsim.contrib.shifts.optimizer.insertion;

import com.google.common.annotations.VisibleForTesting;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.*;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.mobsim.framework.MobsimTimer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;

public class ShiftExtensiveInsertionProvider implements DefaultDrtInsertionSearch.InsertionProvider {
    public static ShiftExtensiveInsertionProvider create(DrtConfigGroup drtCfg, MobsimTimer timer,
                                                         CostCalculationStrategy costCalculationStrategy, DvrpTravelTimeMatrix dvrpTravelTimeMatrix,
                                                         ForkJoinPool forkJoinPool) {
        var insertionParams = (ExtensiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
        var restrictiveDetourTimeEstimator = DetourTimeEstimator.createFreeSpeedZonalTimeEstimator(
                insertionParams.getAdmissibleBeelineSpeedFactor(), dvrpTravelTimeMatrix);
        return new ShiftExtensiveInsertionProvider(drtCfg, timer, costCalculationStrategy, restrictiveDetourTimeEstimator,
                forkJoinPool);
    }

    private final ExtensiveInsertionSearchParams insertionParams;
    private final InsertionCostCalculator<Double> admissibleCostCalculator;
    private final DetourTimeEstimator admissibleDetourTimeEstimator;
    private final InsertionGenerator insertionGenerator;
    private final ForkJoinPool forkJoinPool;

    public ShiftExtensiveInsertionProvider(DrtConfigGroup drtCfg, MobsimTimer timer,
                                      CostCalculationStrategy costCalculationStrategy, DetourTimeEstimator admissibleDetourTimeEstimator,
                                      ForkJoinPool forkJoinPool) {
        this((ExtensiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams(),
                new ShiftInsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy, Double::doubleValue,
                        admissibleDetourTimeEstimator), admissibleDetourTimeEstimator, new InsertionGenerator(),
                forkJoinPool);
    }

    @VisibleForTesting
    ShiftExtensiveInsertionProvider(ExtensiveInsertionSearchParams insertionParams,
                               InsertionCostCalculator<Double> admissibleCostCalculator, DetourTimeEstimator admissibleDetourTimeEstimator,
                               InsertionGenerator insertionGenerator, ForkJoinPool forkJoinPool) {
        this.insertionParams = insertionParams;
        this.admissibleCostCalculator = admissibleCostCalculator;
        this.admissibleDetourTimeEstimator = admissibleDetourTimeEstimator;
        this.insertionGenerator = insertionGenerator;
        this.forkJoinPool = forkJoinPool;
    }

    @Override
    public List<InsertionGenerator.Insertion> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries) {
        DetourData<Double> admissibleTimeData = DetourData.create(admissibleDetourTimeEstimator, drtRequest);

        // Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
        List<InsertionWithDetourData<Double>> preFilteredInsertions = forkJoinPool.submit(
                () -> vehicleEntries.parallelStream()
                        //generate feasible insertions (wrt occupancy limits)
                        .flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
                        //map insertions to insertions with admissible detour times (i.e. admissible beeline speed factor)
                        .map(admissibleTimeData::createInsertionWithDetourData)
                        //optimistic pre-filtering wrt admissible cost function
                        .filter(insertion -> admissibleCostCalculator.calculate(drtRequest, insertion)
                                < INFEASIBLE_SOLUTION_COST)
                        //collect
                        .collect(Collectors.toList())).join();

        if (preFilteredInsertions.isEmpty()) {
            return List.of();
        }

        return KNearestInsertionsAtEndFilter.filterInsertionsAtEnd(insertionParams.getNearestInsertionsAtEndLimit(),
                insertionParams.getAdmissibleBeelineSpeedFactor(), preFilteredInsertions);
    }
}

