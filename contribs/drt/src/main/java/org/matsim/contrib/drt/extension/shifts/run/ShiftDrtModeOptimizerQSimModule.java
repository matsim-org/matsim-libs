package org.matsim.contrib.drt.extension.shifts.run;

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
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtModeOptimizerQSimModule;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.NearestStartLinkAsDepot;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.insertion.DrtRequestInsertionRetryQueue;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
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
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;

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
				getter -> new ShiftDrtOptimizer(
						new DefaultDrtOptimizer(drtCfg, getter.getModal(Fleet.class), getter.get(MobsimTimer.class),
							getter.getModal(DepotFinder.class), getter.getModal(RebalancingStrategy.class),
							getter.getModal(DrtScheduleInquiry.class), getter.getModal(ScheduleTimingUpdater.class),
							getter.getModal(EmptyVehicleRelocator.class), getter.getModal(UnplannedRequestInserter.class),
							getter.getModal(DrtRequestInsertionRetryQueue.class)
						),
						getter.getModal(DrtShiftDispatcher.class),
						getter.getModal(ScheduleTimingUpdater.class))));

		bindModal(OperationFacilityFinder.class).toProvider(modalProvider(
				getter -> new NearestOperationFacilityWithCapacityFinder(getter.getModal(OperationFacilities.class)))
		).asEagerSingleton();

		bindModal(DrtShiftDispatcher.class).toProvider(modalProvider(
				getter -> new DrtShiftDispatcherImpl(getter.getModal(DrtShifts.class), getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.getModal(OperationFacilityFinder.class),
						getter.getModal(ShiftTaskScheduler.class), getter.getModal(Network.class), getter.get(EventsManager.class),
						shiftConfigGroup))
		).asEagerSingleton();

		bindModal(InsertionCostCalculator.InsertionCostCalculatorFactory.class).toProvider(modalProvider(
				getter -> ShiftInsertionCostCalculator.createFactory(drtCfg, getter.get(MobsimTimer.class),
						getter.getModal(CostCalculationStrategy.class))));

		bindModal(VehicleEntry.EntryFactory.class).toInstance(new ShiftVehicleDataEntryFactory(drtCfg));

		final ShiftDrtTaskFactoryImpl taskFactory = new ShiftDrtTaskFactoryImpl(new DrtTaskFactoryImpl());
		bindModal(DrtTaskFactory.class).toInstance(taskFactory);
		bindModal(ShiftDrtTaskFactory.class).toInstance(taskFactory);

		bindModal(ShiftTaskScheduler.class).toProvider(modalProvider(
				getter -> new ShiftTaskScheduler(getter.getModal(Network.class),
						getter.getModal(TravelTime.class),
						getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
						getter.get(MobsimTimer.class), taskFactory,	shiftConfigGroup))
		).asEagerSingleton();

		bindModal(DrtScheduleInquiry.class).to(ShiftDrtScheduleInquiry.class).asEagerSingleton();
		bindModal(RequestInsertionScheduler.class).toProvider(modalProvider(
				getter -> new ShiftRequestInsertionScheduler(drtCfg, getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.getModal(TravelTime.class),
						getter.getModal(ScheduleTimingUpdater.class), getter.getModal(ShiftDrtTaskFactory.class),
						getter.getModal(OperationFacilities.class)))
		).asEagerSingleton();

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
				getter -> new ScheduleTimingUpdater(getter.get(MobsimTimer.class),
						new ShiftDrtStayTaskEndTimeCalculator(shiftConfigGroup,
								new DrtStayTaskEndTimeCalculator(drtCfg))))
		).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(modalProvider(
				(getter) -> new ShiftDrtActionCreator(getter.getModal(PassengerHandler.class),
						new DrtActionCreator(getter.getModal(PassengerHandler.class), getter.get(MobsimTimer.class),
								getter.get(DvrpConfigGroup.class))))
		).asEagerSingleton();
	}
}
