package org.matsim.contrib.drt.extension.operations.shifts.run;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.DrtWithOperationsConfigGroup;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DefaultAssignShiftToVehicleLogic;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DefaultShiftStartLogic;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcherImpl;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.DefaultShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftDrtOptimizer;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftRequestInsertionScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.insertion.ShiftInsertionCostCalculator;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtActionCreator;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftDrtScheduleInquiry;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskSchedulerImpl;
import org.matsim.contrib.drt.extension.operations.shifts.shift.*;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryQueue;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTaskEndTimeCalculator;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.schedule.StopDurationEstimator;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.Fleets;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
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
		this.drtOperationsParams = ((DrtWithOperationsConfigGroup) drtCfg).getDrtOperationsParams();
	}

	@Override
	protected void configureQSim() {

		ShiftsParams shiftsParams = drtOperationsParams.getShiftsParams().orElseThrow();
		bindModal(DrtShifts.class).toProvider(new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {
			@Override
			public DrtShifts get() {
				DrtShiftsSpecification shiftsSpecification = getModalInstance(DrtShiftsSpecification.class);
				ImmutableMap<Id<DrtShift>, DrtShiftImpl> shifts = shiftsSpecification.getShiftSpecifications().values()
						.stream()
						.map(spec -> {
							DefaultShiftBreakImpl shiftBreak = null;
							DrtShiftBreakSpecification breakSpec = spec.getBreak().orElse(null);
							if(breakSpec != null) {
								shiftBreak = new DefaultShiftBreakImpl(
										breakSpec.getEarliestBreakStartTime(),
										breakSpec.getLatestBreakEndTime(),
										breakSpec.getDuration());
							}
							return new DrtShiftImpl(spec.getId(), spec.getStartTime(), spec.getEndTime(), spec.getOperationFacilityId().orElse(null), shiftBreak);
						})
						.collect(ImmutableMap.toImmutableMap(DrtShift::getId, s -> s));
				return () -> shifts;
			}
		}).asEagerSingleton();

		addModalComponent(DrtOptimizer.class, modalProvider(
				getter -> new ShiftDrtOptimizer(
						new DefaultDrtOptimizer(drtCfg, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
							getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
							getter.getModal(DrtScheduleInquiry.class), getter.getModal(ScheduleTimingUpdater.class),
							getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class),
							getter.getModal(DrtRequestInsertionRetryQueue.class)
						),
						getter.getModal(DrtShiftDispatcher.class),
						getter.getModal(ScheduleTimingUpdater.class))));

			bindModal(DrtShiftDispatcher.class).toProvider(modalProvider(
				getter -> new DrtShiftDispatcherImpl(getter.getModal(DrtShifts.class), getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.getModal(OperationFacilities.class), getter.getModal(OperationFacilityFinder.class),
						getter.getModal(ShiftTaskScheduler.class), getter.getModal(Network.class), getter.get(EventsManager.class),
						shiftsParams, new DefaultShiftStartLogic(), new DefaultAssignShiftToVehicleLogic(shiftsParams)))
		).asEagerSingleton();

		bindModal(InsertionCostCalculator.class).toProvider(modalProvider(
				getter -> new ShiftInsertionCostCalculator(getter.get(MobsimTimer.class),
						getter.getModal(CostCalculationStrategy.class))));

		bindModal(VehicleEntry.EntryFactory.class).toInstance(new ShiftVehicleDataEntryFactory(drtCfg));

		final ShiftDrtTaskFactoryImpl taskFactory = new ShiftDrtTaskFactoryImpl(new DrtTaskFactoryImpl());
		bindModal(DrtTaskFactory.class).toInstance(taskFactory);
		bindModal(ShiftDrtTaskFactory.class).toInstance(taskFactory);

		bindModal(ShiftTaskScheduler.class).toProvider(modalProvider(
				getter -> new ShiftTaskSchedulerImpl(getter.getModal(Network.class),
						getter.getModal(TravelTime.class),
						getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
						getter.get(MobsimTimer.class), taskFactory, shiftsParams,
						getter.getModal(OperationFacilities.class), getter.getModal(Fleet.class)))
		).asEagerSingleton();

		bindModal(DrtScheduleInquiry.class).to(ShiftDrtScheduleInquiry.class).asEagerSingleton();
		bindModal(RequestInsertionScheduler.class).toProvider(modalProvider(
				getter -> new ShiftRequestInsertionScheduler(
						getter.get(MobsimTimer.class), getter.getModal(TravelTime.class),
						getter.getModal(ScheduleTimingUpdater.class), getter.getModal(ShiftDrtTaskFactory.class),
						getter.getModal(StopDurationEstimator.class)))
		).asEagerSingleton();

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
				getter -> new ScheduleTimingUpdater(getter.get(MobsimTimer.class),
						new ShiftDrtStayTaskEndTimeCalculator(shiftsParams,
								new DrtStayTaskEndTimeCalculator(getter.getModal(StopDurationEstimator.class)))))
		).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(modalProvider(
				(getter) -> new ShiftDrtActionCreator(getter.getModal(PassengerHandler.class),
						new DrtActionCreator(getter.getModal(PassengerHandler.class), getter.get(MobsimTimer.class),
								getter.get(DvrpConfigGroup.class))))
		).asEagerSingleton();


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
