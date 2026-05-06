package org.matsim.contrib.drt.extension.operations.eshifts.run;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.edrt.EDrtActionCreator;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.edrt.scheduler.EmptyVehicleChargingScheduler;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityReservationManager;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.DrtOperationsActionCreator;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.DrtOperationsTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskSchedulerImpl;
import org.matsim.contrib.drt.optimizer.StopWaypointFactory;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.prebooking.PrebookingActionCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftEDrtModeOptimizerQSimModule extends AbstractDvrpModeQSimModule {

	private final DrtOperationsParams drtOperationsParams;
	private DrtConfigGroup drtCfg;

	public ShiftEDrtModeOptimizerQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtOperationsParams = ((DrtWithExtensionsConfigGroup) drtCfg).getDrtOperationsParams().orElseThrow();
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		ShiftsParams drtShiftParams = drtOperationsParams.getShiftsParams().orElseThrow();

		//set to null to avoid runtime exception
		bindModal(EmptyVehicleChargingScheduler.class).toProvider(modalProvider(
				getter -> null)
		).asEagerSingleton();

		bindModal(VehicleEntry.EntryFactory.class).toProvider(modalProvider(getter ->
				new ShiftVehicleDataEntryFactory(new EDrtVehicleDataEntryFactory(0, getter.getModal(DvrpLoadType.class),
						getter.getModal(StopWaypointFactory.class)),
                        drtShiftParams.isConsiderUpcomingShiftsForInsertion()))).asEagerSingleton();

		bindModal(DrtTaskFactory.class).toProvider(modalProvider(getter ->
						new DrtOperationsTaskFactory(new EDrtTaskFactoryImpl(),
								getter.getModal(OperationFacilities.class),
								getter.getModal(OperationFacilityReservationManager.class))))
				.in(Singleton.class);
		bindModal(ShiftDrtTaskFactory.class).toProvider(modalProvider(getter -> ((ShiftDrtTaskFactory) getter.getModal(DrtTaskFactory.class))));

		bindModal(ShiftTaskScheduler.class).toProvider(modalProvider(
				getter -> new ShiftTaskSchedulerImpl(
						getter.getModal(OperationFacilities.class),
						getter.getModal(ShiftDrtTaskFactory.class),
						getter.getModal(Network.class),
						getter.getModal(OperationFacilityReservationManager.class),
						drtShiftParams,
						getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
						getter.getModal(TravelTime.class),
						getter.getModal(OperationFacilityFinder.class),
						getter.getModal(VehicleEntry.EntryFactory.class),
						getter.getModal(ScheduleTimingUpdater.class),
						getter.getModal(ChargingStrategy.Factory.class),
						getter.getModal(ChargingInfrastructure.class)
						))).asEagerSingleton();

		// See EDrtModeOptimizerQSimModule
		bindModal(VrpLegFactory.class).toProvider(modalProvider(getter -> {
			DvrpConfigGroup dvrpCfg = getter.get(DvrpConfigGroup.class);
			MobsimTimer timer = getter.get(MobsimTimer.class);

			// Makes basic DrtActionCreator create legs with consumption tracker
			return v -> EDrtActionCreator.createLeg(dvrpCfg.getMobsimMode(), v, timer);
		})).in(Singleton.class);

		bindModal(DrtOperationsActionCreator.class).toProvider(modalProvider(getter -> {
			VrpAgentLogic.DynActionCreator delegate = drtCfg.getPrebookingParams().isPresent()
					? getter.getModal(PrebookingActionCreator.class)
					: getter.getModal(DrtActionCreator.class);

			return new DrtOperationsActionCreator(
					getter.getModal(PassengerHandler.class), new EDrtActionCreator(delegate,
					getter.get(MobsimTimer.class)), getter.get(MobsimTimer.class));
		})).asEagerSingleton();

	}
}
