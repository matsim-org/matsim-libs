package org.matsim.contrib.drt.extension.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.services.run.EDrtServicesControlerCreator;
import org.matsim.contrib.drt.extension.services.services.params.ChargingStartedTriggerParam;
import org.matsim.contrib.drt.extension.services.services.params.DrtServiceParams;
import org.matsim.contrib.drt.extension.services.services.params.DrtServicesParams;
import org.matsim.contrib.drt.extension.services.trackers.ChargingTracker;
import org.matsim.contrib.drt.extension.services.trackers.ServiceTracker;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.ev.charging.ChargeUpToMaxSocStrategy;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.FixedSpeedCharging;
import org.matsim.contrib.ev.temperature.TemperatureService;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Key;

public class RunEDrtWithServicesScenarioIT {
	public static final double MINIMUM_RELATIVE_SOC = 0.2;
	public static final double MAX_SOC = 1.0;
	public static final double RELATIVE_SPEED = 1.0;
	public static final double TEMPERATURE = 20.;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();


    @Test
    void test() {
		final String outputDirectory = utils.getOutputDirectory();
		final Config config = ServicesTestUtils.configure(outputDirectory, true);
		var multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		var drtConfigGroup = multiModeDrtConfigGroup.getModalElements().stream().findFirst().orElseThrow();
		drtConfigGroup.idleVehiclesReturnToDepots = true; // Required for standard eDrt

		DrtServicesParams drtServicesParams = new DrtServicesParams();

		{
			DrtServiceParams clean = new DrtServiceParams("clean");
			clean.duration = 300;
			clean.executionLimit = 2;
			clean.enableTaskStacking = true;
			var condition1 = new ChargingStartedTriggerParam();
			clean.addParameterSet(condition1);
			drtServicesParams.addParameterSet(clean);
		}

		{
			DrtServiceParams clean = new DrtServiceParams("plug in/out");
			clean.duration = 15;
			clean.enableTaskStacking = false;
			var condition1 = new ChargingStartedTriggerParam();
			clean.addParameterSet(condition1);
			drtServicesParams.addParameterSet(clean);
		}



		drtConfigGroup.addParameterSet(drtServicesParams);

        final Controler controler = EDrtServicesControlerCreator.createControler(config,false);
		controler.addOverridingModule(new AbstractDvrpModeModule(drtConfigGroup.getMode()) {
			@Override
			public void install() {
				bindModal(EDrtVehicleDataEntryFactory.class).toProvider(
					new EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider(getMode(), MINIMUM_RELATIVE_SOC)
				);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ChargingLogic.Factory.class).to(ChargingWithQueueingAndAssignmentLogic.Factory.class);
				bind(Key.get(ChargingStrategy.Factory.class, DvrpModes.mode(drtConfigGroup.mode))).toInstance(new ChargeUpToMaxSocStrategy.Factory(MAX_SOC));
				bind(ChargingPower.Factory.class).toInstance(ev -> new FixedSpeedCharging(ev, RELATIVE_SPEED));
				bind(TemperatureService.class).toInstance(linkId -> TEMPERATURE);
			}
		});

		ServiceTracker serviceTracker = new ServiceTracker();
		ChargingTracker chargingTracker = new ChargingTracker();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(serviceTracker);
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(chargingTracker);
			}
		});


		controler.run();

		// Check vehicles have been cleaned while charging
		for ( var entrySet: chargingTracker.chargingTracker.entrySet())
		{
			Id<DvrpVehicle> vehicleId = entrySet.getKey();
			int nCharging = entrySet.getValue().size();
			int nCleaning = (int) serviceTracker.serviceTracker.get(vehicleId).stream().filter(s -> s.getServiceType().equals("clean")).count();
			int reqCharging = Math.min(nCharging,2);
			Assertions.assertEquals(reqCharging,nCleaning);
		}

		// Check vehicles have been plugged in/out while charging
		for ( var entrySet: chargingTracker.chargingTracker.entrySet())
		{
			Id<DvrpVehicle> vehicleId = entrySet.getKey();
			int nCharging = entrySet.getValue().size();
			int nPlugInOut = (int) serviceTracker.serviceTracker.get(vehicleId).stream().filter(s -> s.getServiceType().equals("plug in/out")).count();
			Assertions.assertEquals(nCharging,nPlugInOut);
		}

    }





}
