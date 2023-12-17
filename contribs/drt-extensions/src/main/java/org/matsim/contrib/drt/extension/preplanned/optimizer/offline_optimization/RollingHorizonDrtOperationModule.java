package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver.VrpSolver;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver.VrpSolverJsprit;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver.VrpSolverRegretHeuristic;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver.VrpSolverSeqInsertion;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver.ruin_and_recreate.VrpSolverRuinAndRecreate;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

import java.util.Random;

public class RollingHorizonDrtOperationModule extends AbstractDvrpModeQSimModule {
    private final Population prebookedPlans;
    private final DrtConfigGroup drtConfigGroup;
    private final double horizon;
    private final double interval;
    private final int maxIteration;
    private final boolean multiThread;
    private final long seed;
    private final OfflineSolverType offlineSolverType;

    public RollingHorizonDrtOperationModule(Population prebookedPlans, DrtConfigGroup drtConfigGroup, double horizon,
											double interval, int maxIterations, boolean multiThread, long seed, OfflineSolverType type) {
        super(drtConfigGroup.getMode());
        this.prebookedPlans = prebookedPlans;
        this.drtConfigGroup = drtConfigGroup;
        this.horizon = horizon;
        this.interval = interval;
        this.maxIteration = maxIterations;
        this.multiThread = multiThread;
        this.seed = seed;
        this.offlineSolverType = type;
    }

    public enum OfflineSolverType {JSPRIT, SEQ_INSERTION, REGRET_INSERTION, RUIN_AND_RECREATE}

    @Override
    protected void configureQSim() {
        addModalComponent(DrtOptimizer.class, this.modalProvider((getter) -> new RollingHorizonOfflineDrtOptimizer(getter.getModal(Network.class), getter.getModal(TravelTime.class),
                getter.get(MobsimTimer.class), getter.getModal(DrtTaskFactory.class),
                getter.get(EventsManager.class), getter.getModal(ScheduleTimingUpdater.class),
                getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
                drtConfigGroup, getter.getModal(Fleet.class),
                getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool(),
                getter.getModal(VehicleEntry.EntryFactory.class),
                getter.getModal(VrpSolver.class),
                getter.get(Population.class), horizon, interval, prebookedPlans)));

        switch (offlineSolverType) {
            case JSPRIT -> bindModal(VrpSolver.class).toProvider(modalProvider(
                    getter -> new VrpSolverJsprit(
                            new VrpSolverJsprit.Options(maxIteration, multiThread, new Random(seed)),
                            drtConfigGroup, getter.getModal(Network.class), getter.getModal(TravelTime.class))));
            case SEQ_INSERTION -> bindModal(VrpSolver.class).toProvider(modalProvider(
                    getter -> new VrpSolverSeqInsertion(
                            getter.getModal(Network.class), getter.getModal(TravelTime.class), drtConfigGroup)));
            case REGRET_INSERTION -> bindModal(VrpSolver.class).toProvider(modalProvider(
                    getter -> new VrpSolverRegretHeuristic(
                            getter.getModal(Network.class), getter.getModal(TravelTime.class), drtConfigGroup)));
            case RUIN_AND_RECREATE -> bindModal(VrpSolver.class).toProvider(modalProvider(
                    getter -> new VrpSolverRuinAndRecreate(maxIteration,
                            getter.getModal(Network.class), getter.getModal(TravelTime.class), drtConfigGroup,
                            new Random(seed))));
            default -> throw new RuntimeException("The solver is not implemented!");
        }

        addModalComponent(QSimScopeForkJoinPoolHolder.class,
                () -> new QSimScopeForkJoinPoolHolder(drtConfigGroup.numberOfThreads));
        bindModal(VehicleEntry.EntryFactory.class).toInstance(new VehicleDataEntryFactoryImpl());

    }
}
