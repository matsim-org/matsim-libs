package org.matsim.contrib.drt.extension.eshifts.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.edrt.scheduler.EmptyVehicleChargingScheduler;
import org.matsim.contrib.drt.extension.eshifts.dispatcher.EDrtShiftDispatcherImpl;
import org.matsim.contrib.drt.extension.eshifts.optimizer.ShiftEDrtVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.eshifts.schedule.ShiftEDrtActionCreator;
import org.matsim.contrib.drt.extension.eshifts.schedule.ShiftEDrtTaskFactoryImpl;
import org.matsim.contrib.drt.extension.eshifts.scheduler.EShiftTaskScheduler;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftDrtTaskFactory;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShifts;
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

	private final ShiftDrtConfigGroup shiftConfigGroup;

	public ShiftEDrtModeOptimizerQSimModule(DrtConfigGroup drtCfg, ShiftDrtConfigGroup shiftConfigGroup) {
		super(drtCfg.getMode());
		this.shiftConfigGroup = shiftConfigGroup;
	}

	@Override
	protected void configureQSim() {

		//set to null to avoid runtime exception
		bindModal(EmptyVehicleChargingScheduler.class).toProvider(modalProvider(
				getter -> null)
		).asEagerSingleton();

		bindModal(DrtShiftDispatcher.class).toProvider(modalProvider(
				getter -> new EDrtShiftDispatcherImpl(getter.getModal(DrtShifts.class), getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class), getter.getModal(OperationFacilities.class), getter.getModal(OperationFacilityFinder.class),
						getter.getModal(EShiftTaskScheduler.class), getter.getModal(Network.class),
						getter.getModal(ChargingInfrastructure.class), getter.get(EventsManager.class), shiftConfigGroup))
		).asEagerSingleton();

		bindModal(VehicleEntry.EntryFactory.class).toProvider(
				ShiftEDrtVehicleDataEntryFactory.ShiftEDrtVehicleDataEntryFactoryProvider.class
		).asEagerSingleton();

		final ShiftEDrtTaskFactoryImpl taskFactory = new ShiftEDrtTaskFactoryImpl(new EDrtTaskFactoryImpl());
		bindModal(DrtTaskFactory.class).toInstance(taskFactory);
		bindModal(ShiftDrtTaskFactory.class).toInstance(taskFactory);

		bindModal(EShiftTaskScheduler.class).toProvider(modalProvider(
				getter -> new EShiftTaskScheduler(getter.getModal(Network.class), getter.getModal(TravelTime.class),
						getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
						getter.get(MobsimTimer.class), taskFactory,	shiftConfigGroup, getter.getModal(ChargingInfrastructure.class))
		)).asEagerSingleton();

		bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(modalProvider(
				getter -> new ShiftEDrtActionCreator(getter.getModal(PassengerHandler.class),
						getter.get(MobsimTimer.class), getter.get(DvrpConfigGroup.class)))
		).asEagerSingleton();
	}
}
