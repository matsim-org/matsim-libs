package org.matsim.contrib.drt.extension.operations.eshifts.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.edrt.scheduler.EmptyVehicleChargingScheduler;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.DrtWithOperationsConfigGroup;
import org.matsim.contrib.drt.extension.operations.eshifts.dispatcher.EDrtAssignShiftToVehicleLogic;
import org.matsim.contrib.drt.extension.operations.eshifts.dispatcher.EDrtShiftDispatcherImpl;
import org.matsim.contrib.drt.extension.operations.eshifts.dispatcher.EDrtShiftStartLogic;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.ShiftEDrtActionCreator;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.ShiftEDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.operations.eshifts.scheduler.EShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DefaultAssignShiftToVehicleLogic;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DefaultShiftStartLogic;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcherImpl;
import org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.scheduler.ShiftTaskScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShifts;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.api.experimental.events.EventsManager;
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
		this.drtOperationsParams = ((DrtWithOperationsConfigGroup) drtCfg).getDrtOperationsParams();
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		ShiftsParams drtShiftParams = drtOperationsParams.getShiftsParams().orElseThrow();

		//set to null to avoid runtime exception
		bindModal(EmptyVehicleChargingScheduler.class).toProvider(modalProvider(
				getter -> null)
		).asEagerSingleton();

		bindModal(DrtShiftDispatcher.class).toProvider(modalProvider(
				getter -> new EDrtShiftDispatcherImpl(((EShiftTaskScheduler) getter.getModal(ShiftTaskScheduler.class)), getter.getModal(ChargingInfrastructure.class),
						drtShiftParams, getter.getModal(OperationFacilities.class), new DrtShiftDispatcherImpl(getter.getModal(DrtShifts.class), getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.getModal(OperationFacilities.class), getter.getModal(OperationFacilityFinder.class),
						getter.getModal(ShiftTaskScheduler.class), getter.getModal(Network.class), getter.get(EventsManager.class),
						drtShiftParams, new EDrtShiftStartLogic(new DefaultShiftStartLogic()),
						new EDrtAssignShiftToVehicleLogic(new DefaultAssignShiftToVehicleLogic(drtShiftParams), drtShiftParams)),
						getter.getModal(Fleet.class)))).asEagerSingleton();

		bindModal(VehicleEntry.EntryFactory.class).toProvider(modalProvider(getter -> new ShiftVehicleDataEntryFactory(new EDrtVehicleDataEntryFactory(drtCfg, 0)))).asEagerSingleton();

		final ShiftEDrtTaskFactoryImpl taskFactory = new ShiftEDrtTaskFactoryImpl(new EDrtTaskFactoryImpl());
		bindModal(DrtTaskFactory.class).toInstance(taskFactory);
		bindModal(ShiftDrtTaskFactory.class).toInstance(taskFactory);

		bindModal(ShiftTaskScheduler.class).toProvider(modalProvider(
				getter -> new EShiftTaskScheduler(getter.getModal(Network.class), getter.getModal(TravelTime.class),
						getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
						getter.get(MobsimTimer.class), taskFactory, drtShiftParams, getter.getModal(ChargingInfrastructure.class),
						getter.getModal(OperationFacilities.class), getter.getModal(Fleet.class))
		)).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(modalProvider(
				getter -> new ShiftEDrtActionCreator(getter.getModal(PassengerHandler.class),
						getter.get(MobsimTimer.class), getter.get(DvrpConfigGroup.class)))
		).asEagerSingleton();
	}
}
