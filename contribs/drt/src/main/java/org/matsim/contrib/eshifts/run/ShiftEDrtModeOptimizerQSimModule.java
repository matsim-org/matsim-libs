package org.matsim.contrib.eshifts.run;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.insertion.*;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.edrt.optimizer.depot.NearestChargerAsDepot;
import org.matsim.contrib.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.eshifts.dispatcher.EDrtShiftDispatcherImpl;
import org.matsim.contrib.eshifts.optimizer.ShiftEDrtVehicleDataEntryFactory;
import org.matsim.contrib.eshifts.schedule.ShiftEDrtActionCreator;
import org.matsim.contrib.eshifts.schedule.ShiftEDrtTaskFactoryImpl;
import org.matsim.contrib.eshifts.scheduler.EShiftTaskScheduler;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructures;
import org.matsim.contrib.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.shifts.operationFacilities.NearestOperationFacilityWithCapacityFinder;
import org.matsim.contrib.shifts.operationFacilities.OperationFacilitiesUtils;
import org.matsim.contrib.shifts.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.shifts.optimizer.ShiftDrtOptimizer;
import org.matsim.contrib.shifts.optimizer.ShiftRequestInsertionScheduler;
import org.matsim.contrib.shifts.optimizer.insertion.ShiftExtensiveInsertionSearchQSimModule;
import org.matsim.contrib.shifts.optimizer.insertion.ShiftSelectiveInsertionSearchQSimModule;
import org.matsim.contrib.shifts.schedule.ShiftDrtStayTaskEndTimeCalculator;
import org.matsim.contrib.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.shifts.scheduler.ShiftDrtScheduleInquiry;
import org.matsim.contrib.shifts.shift.DrtShiftUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class ShiftEDrtModeOptimizerQSimModule extends AbstractDvrpModeQSimModule {
    private final DrtConfigGroup drtCfg;

    private final ShiftDrtConfigGroup shiftConfigGroup;


    public ShiftEDrtModeOptimizerQSimModule(DrtConfigGroup drtCfg, ShiftDrtConfigGroup shiftConfigGroup) {
        super(drtCfg.getMode());
        this.drtCfg = drtCfg;
        this.shiftConfigGroup = shiftConfigGroup;
    }

    @Override
    protected void configureQSim() {
        this.addModalComponent(DrtOptimizer.class, this.modalProvider((getter) ->
                new ShiftDrtOptimizer(this.drtCfg, getter.getModal(DefaultDrtOptimizer.class),
                        getter.getModal(DrtShiftDispatcher.class),
                        getter.getModal(EShiftTaskScheduler.class),
                        getter.getModal(ScheduleTimingUpdater.class))));

        bindModal(DefaultDrtOptimizer.class).toProvider(modalProvider(
                getter -> new DefaultDrtOptimizer(drtCfg, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
                        getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
                        getter.getModal(DrtScheduleInquiry.class), getter.getModal(ScheduleTimingUpdater.class),
                        getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class),
                        getter.getModal(DrtRequestInsertionRetryQueue.class))))
                .asEagerSingleton();

        bindModal(ChargingInfrastructure.class).toProvider(modalProvider(
                getter -> ChargingInfrastructures.createModalNetworkChargers(getter.get(ChargingInfrastructure.class),
                        getter.getModal(Network.class), getMode()))).asEagerSingleton();

        // XXX if overridden to something else, make sure that the depots are equipped with chargers
        //  otherwise vehicles will not re-charge
        bindModal(DepotFinder.class).to(NearestChargerAsDepot.class);

		bindModal(DrtRequestInsertionRetryQueue.class).toInstance(new DrtRequestInsertionRetryQueue(
				drtCfg.getDrtRequestInsertionRetryParams().orElse(new DrtRequestInsertionRetryParams())));

        this.bindModal(OperationFacilityFinder.class).toProvider(new ModalProviders.AbstractProvider<>(this.drtCfg.getMode()) {
            @Inject
            private Scenario scenario;

            @Override
            public OperationFacilityFinder get() {
                return new NearestOperationFacilityWithCapacityFinder(OperationFacilitiesUtils.getFacilities(scenario));
            }
        }).asEagerSingleton();


        this.bindModal(DrtShiftDispatcher.class).toProvider(new ModalProviders.AbstractProvider<DrtShiftDispatcher>(this.drtCfg.getMode()) {
            @Inject
            private MobsimTimer timer;
            @Inject
            private Scenario scenario;
            @Inject
            private EventsManager eventsManager;

            @Override
            public DrtShiftDispatcher get() {
                var chargingInfrastructure = getModalInstance(ChargingInfrastructure.class);

                return new EDrtShiftDispatcherImpl(DrtShiftUtils.getShifts(scenario), this.getModalInstance(Fleet.class),
                        this.timer, this.getModalInstance(OperationFacilityFinder.class), this.getModalInstance(EShiftTaskScheduler.class), this.getModalInstance(Network.class),
                        chargingInfrastructure, eventsManager, shiftConfigGroup);
            }
        }).asEagerSingleton();

        this.addMobsimScopeEventHandlerBinding().to(modalKey(DrtShiftDispatcher.class));

        addModalComponent(QSimScopeForkJoinPoolHolder.class,
                () -> new QSimScopeForkJoinPoolHolder(drtCfg.getNumberOfThreads()));

        bindModal(UnplannedRequestInserter.class).toProvider(modalProvider(
                getter -> new DefaultUnplannedRequestInserter(drtCfg, getter.getModal(Fleet.class),
                        getter.get(MobsimTimer.class), getter.get(EventsManager.class),
                        getter.getModal(RequestInsertionScheduler.class),
                        getter.getModal(VehicleEntry.EntryFactory.class),
                        getter.getModal(new TypeLiteral<DrtInsertionSearch<OneToManyPathSearch.PathData>>() {}),
                        getter.getModal(DrtRequestInsertionRetryQueue.class),
                        getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool()))).asEagerSingleton();


        this.install(getInsertionSearchQSimModule(this.drtCfg));

        bindModal(VehicleEntry.EntryFactory.class).toProvider(
                ShiftEDrtVehicleDataEntryFactory.ShiftEDrtVehicleDataEntryFactoryProvider.class).asEagerSingleton();

        bindModal(CostCalculationStrategy.class).to(drtCfg.isRejectRequestIfMaxWaitOrTravelTimeViolated() ?
                CostCalculationStrategy.RejectSoftConstraintViolations.class :
                CostCalculationStrategy.DiscourageSoftConstraintViolations.class).asEagerSingleton();

        final ShiftEDrtTaskFactoryImpl taskFactory = new ShiftEDrtTaskFactoryImpl(new EDrtTaskFactoryImpl());
        this.bindModal(DrtTaskFactory.class).toInstance(taskFactory);
        this.bindModal(ShiftDrtTaskFactory.class).toInstance(taskFactory);
        this.bindModal(EmptyVehicleRelocator.class).toProvider(new ModalProviders.AbstractProvider<EmptyVehicleRelocator>(this.drtCfg.getMode()) {
            @Inject
            @Named("dvrp_estimated")
            private TravelTime travelTime;
            @Inject
            private MobsimTimer timer;

            @Override
            public EmptyVehicleRelocator get() {
                Network network = (Network) this.getModalInstance(Network.class);
                DrtTaskFactory taskFactory = (DrtTaskFactory) this.getModalInstance(DrtTaskFactory.class);
                TravelDisutility travelDisutility = ((TravelDisutilityFactory) this.getModalInstance(TravelDisutilityFactory.class)).createTravelDisutility(this.travelTime);
                return new EmptyVehicleRelocator(network, this.travelTime, travelDisutility, this.timer, taskFactory);
            }
        }).asEagerSingleton();

        this.bindModal(EShiftTaskScheduler.class).toProvider(new ModalProviders.AbstractProvider<EShiftTaskScheduler>(this.drtCfg.getMode()) {
            @Inject
            @Named("dvrp_estimated")
            private TravelTime travelTime;
            @Inject
            private MobsimTimer timer;

            @Override
            public EShiftTaskScheduler get() {
                var chargingInfrastructure = getModalInstance(ChargingInfrastructure.class);
                Network network = this.getModalInstance(Network.class);
                ShiftDrtTaskFactory taskFactory = this.getModalInstance(ShiftDrtTaskFactory.class);
                TravelDisutility travelDisutility = this.getModalInstance(TravelDisutilityFactory.class).createTravelDisutility(this.travelTime);
                return new EShiftTaskScheduler(network, this.travelTime, travelDisutility, this.timer, taskFactory, shiftConfigGroup, chargingInfrastructure);
            }
        }).asEagerSingleton();

        bindModal(DrtScheduleInquiry.class).to(ShiftDrtScheduleInquiry.class).asEagerSingleton();

        bindModal(RequestInsertionScheduler.class).toProvider(modalProvider(
                getter -> new ShiftRequestInsertionScheduler(drtCfg, getter.getModal(Fleet.class),
                        getter.get(MobsimTimer.class),
                        getter.getNamed(TravelTime.class, DvrpTravelTimeModule.DVRP_ESTIMATED),
                        getter.getModal(ScheduleTimingUpdater.class), getter.getModal(ShiftDrtTaskFactory.class), OperationFacilitiesUtils.getFacilities(getter.get(Scenario.class)))))
                .asEagerSingleton();

        this.bindModal(ScheduleTimingUpdater.class).toProvider(this.modalProvider((getter) -> {
            return new ScheduleTimingUpdater(getter.get(MobsimTimer.class), new ShiftDrtStayTaskEndTimeCalculator(shiftConfigGroup, new DrtStayTaskEndTimeCalculator(this.drtCfg)));
        })).asEagerSingleton();

        this.bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(this.modalProvider((getter) -> new ShiftEDrtActionCreator(getter.getModal(PassengerHandler.class),
                getter.get(MobsimTimer.class), getter.get(DvrpConfigGroup.class))))
                .asEagerSingleton();


        bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));
    }

    public static AbstractDvrpModeQSimModule getInsertionSearchQSimModule(DrtConfigGroup drtCfg) {
        switch (drtCfg.getDrtInsertionSearchParams().getName()) {
            case ExtensiveInsertionSearchParams.SET_NAME:
                return new ShiftExtensiveInsertionSearchQSimModule(drtCfg);

            case SelectiveInsertionSearchParams.SET_NAME:
                return new ShiftSelectiveInsertionSearchQSimModule(drtCfg);

            default:
                throw new RuntimeException(
                        "Unsupported DRT insertion search type: " + drtCfg.getDrtInsertionSearchParams().getName());
        }
    }
}
