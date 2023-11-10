package org.matsim.contrib.drt.extension.prebooking;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider;
import org.matsim.contrib.drt.extension.edrt.run.EDrtControlerCreator;
import org.matsim.contrib.drt.extension.prebooking.logic.FixedSharePrebookingLogic;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.charging.ChargeUpToMaxSocStrategy;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.FixedSpeedCharging;
import org.matsim.contrib.ev.temperature.TemperatureService;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class PrebookingExampleIT {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDrtWithPrebooking() {
		Id.resetCaches();
		
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl,
				new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			DrtWithExtensionsConfigGroup extensionsConfig = (DrtWithExtensionsConfigGroup) drtCfg;
			DrtPrebookingParams prebookingParams = new DrtPrebookingParams();
			extensionsConfig.advanceRequestPlanningHorizon = Double.POSITIVE_INFINITY;
			extensionsConfig.addParameterSet(prebookingParams);
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Controler controller = DrtControlerCreator.createControler(config, false);
		controller.addOverridingModule(new PrebookingModule());
		FixedSharePrebookingLogic.install("drt", 0.5, 4.0 * 3600.0, controller);

		Tracker tracker = new Tracker();
		tracker.install(controller);
		
		controller.run();
		
		assertEquals(157, tracker.immediateScheduled);
		assertEquals(205, tracker.prebookedScheduled);
		assertEquals(26, tracker.immediateRejected);
		assertEquals(0, tracker.prebookedRejected);
	}

	@Test
	public void testElectricDrtWithPrebooking() {
		Id.resetCaches();
		
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_edrt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl,
				new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new), new DvrpConfigGroup(),
				new OTFVisConfigGroup(), new EvConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		Controler controler = EDrtControlerCreator.createControler(config, false);

		for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.getMode()) {
				@Override
				public void install() {
					bind(EDrtVehicleDataEntryFactoryProvider.class)
							.toInstance(new EDrtVehicleDataEntryFactoryProvider(drtCfg, 0.2));
				}
			});

			DrtWithExtensionsConfigGroup extensionsConfig = (DrtWithExtensionsConfigGroup) drtCfg;
			DrtPrebookingParams prebookingParams = new DrtPrebookingParams();
			extensionsConfig.advanceRequestPlanningHorizon = Double.POSITIVE_INFINITY;
			extensionsConfig.addParameterSet(prebookingParams);
		}

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ChargingLogic.Factory.class).toProvider(new ChargingWithQueueingAndAssignmentLogic.FactoryProvider(
						charger -> new ChargeUpToMaxSocStrategy(charger, 0.8)));
				bind(ChargingPower.Factory.class).toInstance(ev -> new FixedSpeedCharging(ev, 1.0));
				bind(TemperatureService.class).toInstance(linkId -> 20.0);
			}
		});

		controler.addOverridingModule(new ElectricPrebookingModule());
		FixedSharePrebookingLogic.install("drt", 0.5, 4.0 * 3600.0, controler);

		Tracker tracker = new Tracker();
		tracker.install(controler);
		
		controler.run();

		assertEquals(74, tracker.immediateScheduled);
		assertEquals(178, tracker.prebookedScheduled);
		assertEquals(109, tracker.immediateRejected);
		assertEquals(27, tracker.prebookedRejected);
	}
	
	static private class Tracker implements PassengerRequestRejectedEventHandler, PassengerRequestScheduledEventHandler {
		int immediateScheduled = 0;
		int prebookedScheduled = 0;
		int immediateRejected = 0;
		int prebookedRejected = 0;
		
		@Override
		public void handleEvent(PassengerRequestScheduledEvent event) {
			if (event.getRequestId().toString().contains("prebooked")) {
				prebookedScheduled++;
			} else {
				immediateScheduled++;
			}
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			if (event.getRequestId().toString().contains("prebooked")) {
				prebookedRejected++;
			} else {
				immediateRejected++;
			}
		}
		
		void install(Controler controller) {
			Tracker thisTracker = this;
			
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(thisTracker);
				}
			});
		}
	}
}
