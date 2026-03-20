package org.matsim.contrib.drt.extension.operations.shifts.run;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityReservationManager;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.*;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.DefaultShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftDrtOptimizer;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.insertion.ShiftInsertionCostCalculator;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.DrtOperationsActionCreator;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.DrtOperationsTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftDrtScheduleInquiry;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskSchedulerImpl;
import org.matsim.contrib.drt.optimizer.*;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DefaultInsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.prebooking.PrebookingActionCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.schedule.DrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.Fleets;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.schedule.DriveTaskUpdater;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdaterImpl;
import org.matsim.contrib.dvrp.tracker.OnlineTrackerListener;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class ShiftDrtModeOptimizerQSimModule extends AbstractDvrpModeQSimModule {

	private final DrtConfigGroup drtCfg;
	private final DrtOperationsParams drtOperationsParams;

	public ShiftDrtModeOptimizerQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.drtOperationsParams = ((DrtWithExtensionsConfigGroup) drtCfg).getDrtOperationsParams().orElseThrow();
	}

	@Override
	protected void configureQSim() {

		ShiftsParams shiftsParams = drtOperationsParams.getShiftsParams().orElseThrow();

		addModalComponent(DrtOptimizer.class, modalProvider(
				getter -> {
					return new ShiftDrtOptimizer(
							new DefaultDrtOptimizer(
									getter.getModal(QsimScopeForkJoinPool.class), drtCfg, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
									getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
									getter.getModal(ScheduleInquiry.class), getter.getModal(ScheduleTimingUpdater.class),
									getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class),
									getter.getModal(DrtRequestInsertionRetryQueue.class)
							),
							getter.getModal(DrtShiftDispatcher.class),
							getter.getModal(ScheduleTimingUpdater.class));
				}));

		bindModal(AssignShiftToVehicleLogic.class).toProvider(modalProvider(getter ->
				(new AssignShiftToVehicleLogicImpl(shiftsParams))
		));

		bindModal(DrtShiftDispatcher.class).toProvider(modalProvider(
				getter -> new DrtShiftDispatcherImpl(getMode(), getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
						getter.getModal(OperationFacilities.class), getter.getModal(OperationFacilityFinder.class),
						getter.getModal(ShiftTaskScheduler.class), getter.get(EventsManager.class),
						shiftsParams, new DefaultShiftStartLogic(), getter.getModal(AssignShiftToVehicleLogic.class),
						getter.getModal(ShiftScheduler.class), getter.getModal(OperationFacilityReservationManager.class)))
		).asEagerSingleton();

		bindModal(InsertionCostCalculator.class).toProvider(modalProvider(
				getter -> new ShiftInsertionCostCalculator(
						new DefaultInsertionCostCalculator(getter.getModal(CostCalculationStrategy.class),
								drtCfg.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet()))));

		bindModal(VehicleEntry.EntryFactory.class).toProvider(modalProvider(getter -> {
			DvrpLoadType loadType = getter.getModal(DvrpLoadType.class);
			return new ShiftVehicleDataEntryFactory(new VehicleDataEntryFactoryImpl(loadType, getter.getModal(StopWaypointFactory.class)),
					shiftsParams.isConsiderUpcomingShiftsForInsertion());
		}));

		bindModal(DrtTaskFactory.class).toProvider(modalProvider(getter ->  new DrtOperationsTaskFactory(
				new DrtTaskFactoryImpl(),
				getter.getModal(OperationFacilities.class),
				getter.getModal(OperationFacilityReservationManager.class)
		)));

		bindModal(ShiftDrtTaskFactory.class).toProvider(modalProvider(getter -> ((ShiftDrtTaskFactory) getter.getModal(DrtTaskFactory.class))));

		bindModal(ShiftTaskScheduler.class).toProvider(modalProvider(
				getter -> new ShiftTaskSchedulerImpl(
						getter.getModal(OperationFacilities.class),
						getter.getModal(ShiftDrtTaskFactory.class),
						getter.getModal(Network.class),
						getter.getModal(OperationFacilityReservationManager.class),
						shiftsParams,
						getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
						getter.getModal(TravelTime.class),
						getter.getModal(OperationFacilityFinder.class),
						getter.getModal(VehicleEntry.EntryFactory.class),
						getter.getModal(ScheduleTimingUpdater.class)))
		).asEagerSingleton();

		bindModal(ScheduleInquiry.class).to(ShiftDrtScheduleInquiry.class).asEagerSingleton();

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
				getter -> new DrtScheduleTimingUpdater(new ScheduleTimingUpdaterImpl(getter.get(MobsimTimer.class),
						new ShiftDrtStayTaskEndTimeCalculator(shiftsParams,
								new DrtStayTaskEndTimeCalculator(getter.getModal(StopTimeCalculator.class))),
						getter.getModal(DriveTaskUpdater.class)), getter.getModal(PassengerStopDurationProvider.class))
		)).asEagerSingleton();

		// see DrtModeOptimizerQSimModule
		bindModal(VrpLegFactory.class).toProvider(modalProvider(getter -> {
			DvrpConfigGroup dvrpCfg = getter.get(DvrpConfigGroup.class);
			MobsimTimer timer = getter.get(MobsimTimer.class);

			return v -> VrpLegFactory.createWithOnlineTracker(dvrpCfg.getMobsimMode(), v, OnlineTrackerListener.NO_LISTENER,
					timer);
		})).in(Singleton.class);

		bindModal(DrtOperationsActionCreator.class).toProvider(modalProvider((getter) -> {
			VrpAgentLogic.DynActionCreator delegate = drtCfg.getPrebookingParams().isPresent()
					? getter.getModal(PrebookingActionCreator.class)
					: getter.getModal(DrtActionCreator.class);

			// adds shift tasks
			return new DrtOperationsActionCreator(getter.getModal(PassengerHandler.class), delegate, getter.get(MobsimTimer.class));
		})).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).to(modalKey(DrtOperationsActionCreator.class));

		bindModal(Fleet.class).toProvider(new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {
			@Override
			public Fleet get() {
				FleetSpecification fleetSpecification = getModalInstance(FleetSpecification.class);
				Network network = getModalInstance(Network.class);
				return Fleets.createCustomFleet(fleetSpecification,
						s -> new DefaultShiftDvrpVehicle(new DvrpVehicleImpl(s, network.getLinks().get(s.getStartLinkId()))));

			}
		}).asEagerSingleton();
	}
}
