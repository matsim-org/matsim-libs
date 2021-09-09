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
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import static java.util.stream.Collectors.toList;

public class ShiftSelectiveInsertionProvider implements DefaultDrtInsertionSearch.InsertionProvider {
    public static ShiftSelectiveInsertionProvider create(DrtConfigGroup drtCfg, MobsimTimer timer,
                                                         CostCalculationStrategy costCalculationStrategy, DvrpTravelTimeMatrix dvrpTravelTimeMatrix,
                                                         ForkJoinPool forkJoinPool) {
        var insertionParams = (SelectiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
        var restrictiveDetourTimeEstimator = DetourTimeEstimator.createFreeSpeedZonalTimeEstimator(
                insertionParams.getRestrictiveBeelineSpeedFactor(), dvrpTravelTimeMatrix);
        return new ShiftSelectiveInsertionProvider(drtCfg, timer, costCalculationStrategy, restrictiveDetourTimeEstimator,
                forkJoinPool);
    }

    private final DetourTimeEstimator restrictiveDetourTimeEstimator;
    private final BestInsertionFinder<Double> initialInsertionFinder;
    private final InsertionGenerator insertionGenerator;
    private final ForkJoinPool forkJoinPool;

    public ShiftSelectiveInsertionProvider(DrtConfigGroup drtCfg, MobsimTimer timer,
                                      CostCalculationStrategy costCalculationStrategy, DetourTimeEstimator restrictiveDetourTimeEstimator,
                                      ForkJoinPool forkJoinPool) {
        this(restrictiveDetourTimeEstimator, new BestInsertionFinder<>(
                new ShiftInsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy, Double::doubleValue,
                        restrictiveDetourTimeEstimator)), new InsertionGenerator(), forkJoinPool);
    }

    @VisibleForTesting
    ShiftSelectiveInsertionProvider(DetourTimeEstimator restrictiveDetourTimeEstimator,
                               BestInsertionFinder<Double> initialInsertionFinder, InsertionGenerator insertionGenerator,
                               ForkJoinPool forkJoinPool) {
        this.restrictiveDetourTimeEstimator = restrictiveDetourTimeEstimator;
        this.initialInsertionFinder = initialInsertionFinder;
        this.insertionGenerator = insertionGenerator;
        this.forkJoinPool = forkJoinPool;
    }

    @Override
    public List<InsertionGenerator.Insertion> getInsertions(DrtRequest drtRequest, Collection<VehicleEntry> vehicleEntries) {
        DetourData<Double> restrictiveTimeData = DetourData.create(restrictiveDetourTimeEstimator, drtRequest);

        // Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
        Optional<InsertionWithDetourData<Double>> bestInsertion = forkJoinPool.submit(
                // find best insertion given a stream of insertion with time data
                () -> initialInsertionFinder.findBestInsertion(drtRequest,
                        //for each vehicle entry
                        vehicleEntries.parallelStream()
                                //generate feasible insertions (wrt occupancy limits)
                                .flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
                                //map them to insertions with admissible detour times
                                .map(restrictiveTimeData::createInsertionWithDetourData))).join();

        return bestInsertion.map(InsertionWithDetourData::getInsertion).stream().collect(toList());
    }
}
