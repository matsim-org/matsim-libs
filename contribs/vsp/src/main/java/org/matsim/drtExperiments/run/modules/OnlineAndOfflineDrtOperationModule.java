package org.matsim.drtExperiments.run.modules;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.optimizer.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.drtExperiments.basicStructures.OnlineAndOfflineDrtOptimizer;
import org.matsim.drtExperiments.offlineStrategy.OfflineSolver;
import org.matsim.drtExperiments.offlineStrategy.OfflineSolverJsprit;
import org.matsim.drtExperiments.offlineStrategy.OfflineSolverRegretHeuristic;
import org.matsim.drtExperiments.offlineStrategy.OfflineSolverSeqInsertion;
import org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate.RuinAndRecreateOfflineSolver;
import org.matsim.drtExperiments.onlineStrategy.OnlineSolver;
import org.matsim.drtExperiments.onlineStrategy.OnlineSolverBasicInsertionStrategy;

import java.util.Random;

public class OnlineAndOfflineDrtOperationModule extends AbstractDvrpModeQSimModule {
    private final Population prebookedPlans;
    private final DrtConfigGroup drtConfigGroup;
    private final double horizon;
    private final double interval;
    private final int maxIteration;
    private final boolean multiThread;
    private final long seed;
    private final OfflineSolverType offlineSolverType;

    public OnlineAndOfflineDrtOperationModule( Population prebookedPlans, DrtConfigGroup drtConfigGroup, double horizon,
                                               double interval, int maxIterations, boolean multiThread, long seed, OfflineSolverType type ) {
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
        addModalComponent(DrtOptimizer.class, this.modalProvider((getter) -> new OnlineAndOfflineDrtOptimizer(getter.getModal(Network.class ), getter.getModal(TravelTime.class ),
                getter.get(MobsimTimer.class), getter.getModal(DrtTaskFactory.class),
                getter.get(EventsManager.class), getter.getModal(ScheduleTimingUpdater.class),
                getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
                drtConfigGroup, getter.getModal(Fleet.class),
                getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool(),
                getter.getModal(VehicleEntry.EntryFactory.class),
                getter.getModal( OfflineSolver.class ),
                getter.getModal( OnlineSolver.class ),
                getter.get(Population.class), horizon, interval, prebookedPlans)));

        bindModal(OnlineSolver.class).toProvider(modalProvider(
                getter -> new OnlineSolverBasicInsertionStrategy(getter.getModal(Network.class ), drtConfigGroup,
                        getter.getModal( TravelTimeMatrix.class ), getter.getModal(TravelTime.class ),
                        getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)))));

        switch (offlineSolverType) {
            case JSPRIT -> bindModal(OfflineSolver.class).toProvider(modalProvider(
                    getter -> new OfflineSolverJsprit(
                            new OfflineSolverJsprit.Options(maxIteration, multiThread, new Random(seed)),
                            drtConfigGroup, getter.getModal(Network.class), getter.getModal(TravelTime.class))));
            case SEQ_INSERTION -> bindModal(OfflineSolver.class).toProvider(modalProvider(
                    getter -> new OfflineSolverSeqInsertion(
                            getter.getModal(Network.class), getter.getModal(TravelTime.class), drtConfigGroup)));
            case REGRET_INSERTION -> bindModal(OfflineSolver.class).toProvider(modalProvider(
                    getter -> new OfflineSolverRegretHeuristic(
                            getter.getModal(Network.class), getter.getModal(TravelTime.class), drtConfigGroup)));
            case RUIN_AND_RECREATE -> bindModal(OfflineSolver.class).toProvider(modalProvider(
                    getter -> new RuinAndRecreateOfflineSolver(maxIteration,
                            getter.getModal(Network.class), getter.getModal(TravelTime.class), drtConfigGroup,
                            new Random(seed))));
            default -> throw new RuntimeException("The solver is not implemented!");
        }

        addModalComponent(QSimScopeForkJoinPoolHolder.class,
                () -> new QSimScopeForkJoinPoolHolder( drtConfigGroup.getNumberOfThreads() ) );

//        bindModal(VehicleEntry.EntryFactory.class ).toInstance(new VehicleDataEntryFactoryImpl(drtConfigGroup) );
        // the above is what I found, but the ctor taking drtConfigGroup as an arg no longer exists.  Replacing by the below w/o knowing if it is correct:
        bindModal(VehicleEntry.EntryFactory.class).toProvider(modalProvider(getter ->
                                                                                    new VehicleDataEntryFactoryImpl( getter.getModal(DvrpLoadType.class), getter.getModal( StopWaypointFactory.class ) ) ) )
                                                  .asEagerSingleton();

        bindModal( StopWaypointFactory.class ).toProvider( modalProvider( getter ->
                                                                                  new StopWaypointFactoryImpl( getter.getModal( DvrpLoadType.class ), true )
                                                                        ) );
        // (yy I am just guessing this. In particular, I am guessing the value of scheduleWaitBeforeDrive.  kai, may'26)

    }
}
