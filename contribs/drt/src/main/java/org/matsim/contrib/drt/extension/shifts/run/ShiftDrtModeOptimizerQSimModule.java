package org.matsim.contrib.drt.extension.shifts.run;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.shifts.dispatcher.DrtShiftDispatcherImpl;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.NearestOperationFacilityWithCapacityFinder;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.shifts.optimizer.ShiftDrtOptimizer;
import org.matsim.contrib.drt.extension.shifts.optimizer.ShiftRequestInsertionScheduler;
import org.matsim.contrib.drt.extension.shifts.optimizer.ShiftVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.shifts.optimizer.insertion.ShiftInsertionCostCalculator;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftDrtActionCreator;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftDrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.shifts.scheduler.ShiftDrtScheduleInquiry;
import org.matsim.contrib.drt.extension.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShifts;
import org.matsim.contrib.drt.optimizer.*;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.NearestStartLinkAsDepot;
import org.matsim.contrib.drt.optimizer.insertion.*;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class ShiftDrtModeOptimizerQSimModule extends AbstractDvrpModeQSimModule {

	private final DrtConfigGroup drtCfg;
	private final ShiftDrtConfigGroup shiftConfigGroup;

	public ShiftDrtModeOptimizerQSimModule(DrtConfigGroup drtCfg, ShiftDrtConfigGroup shiftConfigGroup) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.shiftConfigGroup = shiftConfigGroup;
	}

	@Override
	protected void configureQSim() {
		addModalComponent(DrtOptimizer.class, modalProvider(
				getter -> new ShiftDrtOptimizer(drtCfg, getter.getModal(DefaultDrtOptimizer.class),
						getter.getModal(DrtShiftDispatcher.class),
						getter.getModal(ScheduleTimingUpdater.class))));

		bindModal(DefaultDrtOptimizer.class).toProvider(modalProvider(
				getter -> new DefaultDrtOptimizer(drtCfg, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
						getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
						getter.getModal(DrtScheduleInquiry.class), getter.getModal(ScheduleTimingUpdater.class),
						getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class),
						getter.getModal(DrtRequestInsertionRetryQueue.class)))).asEagerSingleton();

		bindModal(DepotFinder.class).toProvider(
				modalProvider(getter -> new NearestStartLinkAsDepot(getter.getModal(Fleet.class)))).asEagerSingleton();

		bindModal(DrtRequestInsertionRetryQueue.class).toInstance(new DrtRequestInsertionRetryQueue(
				drtCfg.getDrtRequestInsertionRetryParams().orElse(new DrtRequestInsertionRetryParams())));

		bindModal(OperationFacilityFinder.class).toProvider(new ModalProviders.AbstractProvider<>(drtCfg.getMode(), DvrpModes::mode) {

			@Override
			public OperationFacilityFinder get() {
				return new NearestOperationFacilityWithCapacityFinder(getModalInstance(OperationFacilities.class));
			}
		}).asEagerSingleton();

		bindModal(DrtShiftDispatcher.class).toProvider(new ModalProviders.AbstractProvider<>(drtCfg.getMode(), DvrpModes::mode) {
			@Inject
			private MobsimTimer timer;
			@Inject
			private Scenario scenario;
			@Inject
			private EventsManager eventsManager;

			@Override
			public DrtShiftDispatcher get() {
				return new DrtShiftDispatcherImpl(getModalInstance(DrtShifts.class), getModalInstance(Fleet.class),
						timer, getModalInstance(OperationFacilityFinder.class),
						getModalInstance(ShiftTaskScheduler.class), getModalInstance(Network.class), eventsManager,
						shiftConfigGroup);
			}
		}).asEagerSingleton();

		addModalComponent(QSimScopeForkJoinPoolHolder.class,
				() -> new QSimScopeForkJoinPoolHolder(drtCfg.getNumberOfThreads()));

		bindModal(UnplannedRequestInserter.class).toProvider(modalProvider(
				getter -> new DefaultUnplannedRequestInserter(drtCfg, getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.get(EventsManager.class),
						getter.getModal(RequestInsertionScheduler.class),
						getter.getModal(VehicleEntry.EntryFactory.class),
						getter.getModal(new TypeLiteral<DrtInsertionSearch<OneToManyPathSearch.PathData>>() {
						}), getter.getModal(DrtRequestInsertionRetryQueue.class),
						getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool()))).asEagerSingleton();

		bindModal(InsertionCostCalculator.InsertionCostCalculatorFactory.class).toProvider(modalProvider(
				getter -> ShiftInsertionCostCalculator.createFactory(drtCfg, getter.get(MobsimTimer.class),
						getter.getModal(CostCalculationStrategy.class))));

		install(DrtModeOptimizerQSimModule.getInsertionSearchQSimModule(drtCfg));

		bindModal(VehicleEntry.EntryFactory.class).toInstance(new ShiftVehicleDataEntryFactory(drtCfg));
		bindModal(CostCalculationStrategy.class).to(drtCfg.isRejectRequestIfMaxWaitOrTravelTimeViolated() ?
				CostCalculationStrategy.RejectSoftConstraintViolations.class :
				CostCalculationStrategy.DiscourageSoftConstraintViolations.class).asEagerSingleton();

		final ShiftDrtTaskFactoryImpl taskFactory = new ShiftDrtTaskFactoryImpl(new DrtTaskFactoryImpl());

		bindModal(DrtTaskFactory.class).toInstance(taskFactory);
		bindModal(ShiftDrtTaskFactory.class).toInstance(taskFactory);
		bindModal(EmptyVehicleRelocator.class).toProvider(new ModalProviders.AbstractProvider<>(drtCfg.getMode(), DvrpModes::mode) {
			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Inject
			private MobsimTimer timer;

			@Override
			public EmptyVehicleRelocator get() {
				Network network = getModalInstance(Network.class);
				DrtTaskFactory taskFactory = getModalInstance(DrtTaskFactory.class);
				TravelDisutility travelDisutility = getModalInstance(
						TravelDisutilityFactory.class).createTravelDisutility(travelTime);
				return new EmptyVehicleRelocator(network, travelTime, travelDisutility, timer, taskFactory);
			}
		}).asEagerSingleton();

		bindModal(ShiftTaskScheduler.class).toProvider(
				new ModalProviders.AbstractProvider<DvrpMode, ShiftTaskScheduler>(drtCfg.getMode(), DvrpModes::mode) {
					@Inject
					@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
					private TravelTime travelTime;
					@Inject
					private MobsimTimer timer;

					@Override
					public ShiftTaskScheduler get() {
						Network network = getModalInstance(Network.class);
						ShiftDrtTaskFactory taskFactory = getModalInstance(ShiftDrtTaskFactory.class);
						TravelDisutility travelDisutility = getModalInstance(
								TravelDisutilityFactory.class).createTravelDisutility(travelTime);
						return new ShiftTaskScheduler(network, travelTime, travelDisutility, timer, taskFactory,
								shiftConfigGroup);
					}
				}).asEagerSingleton();

		bindModal(DrtScheduleInquiry.class).to(ShiftDrtScheduleInquiry.class).asEagerSingleton();
		bindModal(RequestInsertionScheduler.class).toProvider(modalProvider(
				getter -> new ShiftRequestInsertionScheduler(drtCfg, getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class),
						getter.getNamed(TravelTime.class, DvrpTravelTimeModule.DVRP_ESTIMATED),
						getter.getModal(ScheduleTimingUpdater.class), getter.getModal(ShiftDrtTaskFactory.class),
						getter.getModal(OperationFacilities.class)))).asEagerSingleton();

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
				getter -> new ScheduleTimingUpdater(getter.get(MobsimTimer.class),
						new ShiftDrtStayTaskEndTimeCalculator(shiftConfigGroup,
								new DrtStayTaskEndTimeCalculator(drtCfg))))).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(modalProvider(
				(getter) -> new ShiftDrtActionCreator(getter.getModal(PassengerHandler.class),
						new DrtActionCreator(getter.getModal(PassengerHandler.class), getter.get(MobsimTimer.class),
								getter.get(DvrpConfigGroup.class))))).asEagerSingleton();
		bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));
	}
}
