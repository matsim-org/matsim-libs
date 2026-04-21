package org.matsim.contrib.ev.dsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEvent;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEventHandler;
import org.matsim.core.communication.LocalCommunicator;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.dsim.DistributedContext;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test for EV drive discharging in a distributed (DSim) setting.
 * <p>
 * Scenario: one EV drives from link l1 through l2 to l3 in the three-link
 * network, crossing all three partition boundaries.  No chargers are present.
 * <p>
 * Tests verify:
 * <ul>
 *   <li>Total energy consumed equals the expected value (l2: 1000 J + l3: 100 J = 1100 J).</li>
 *   <li>Each {@link DrivingEnergyConsumptionEvent} fires at the correct time relative to
 *       its triggering event: one step later in QSim (deferred processing), same step in
 *       DSim (synchronous processing in {@code DistributedDriveDischargingHandler}).</li>
 * </ul>
 *
 * @see EvDSimTestFixture
 */
public class EvDriveDischargingIntegrationTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void qsim() {
		var config = EvDSimTestFixture.createConfig(utils.getOutputDirectory());
		var scenario = EvDSimTestFixture.createScenario(config, 1 /* single partition */);

		var controller = new Controler(scenario);
		EvDSimTestFixture.installEvModules(controller);

		// QSim defers discharge by one step: DrivingEnergyConsumptionEvent.time = triggeringEvent.time + 1
		var verifier = new DriveDischargeVerifier(1);
		verifier.install(controller);

		controller.run();
		verifier.assertExpected();
	}

	/**
	 * Runs DSim with three partitions on a single thread pool (NullCommunicator).
	 * All inter-partition communication is in-process.
	 */
	@Test
	void threePartitions() {
		var config = EvDSimTestFixture.createConfig(utils.getOutputDirectory());
		EvDSimTestFixture.configureDSim(config, 3);
		var scenario = EvDSimTestFixture.createScenario(config, 3);

		var controller = new Controler(scenario);
		EvDSimTestFixture.installEvModules(controller);

		// DSim discharges synchronously: DrivingEnergyConsumptionEvent.time = triggeringEvent.time
		var verifier = new DriveDischargeVerifier(0);
		verifier.install(controller);

		controller.run();
		verifier.assertExpected();
	}

	/**
	 * Runs DSim with three separate JVM threads, each owning one partition.
	 * Uses {@link LocalCommunicator} for in-process message passing between ranks.
	 * <p>
	 * Disabled until WP3–WP5 are implemented: threads fail during Guice injection
	 * (singleton EV handlers not yet partition-aware) and the {@link LocalCommunicator}
	 * receive loops spin indefinitely waiting for peers that already failed.
	 */
	@Test
	@Timeout(value = 2, unit = TimeUnit.MINUTES)
	//@Disabled("Enable once EV handlers are partition-aware (WP3-WP5)")
	void threeNodes() throws InterruptedException, ExecutionException, TimeoutException {
		var outputDirectory = utils.getOutputDirectory();
		var size = 3;
		var comms = LocalCommunicator.create(size);

		try (var pool = Executors.newFixedThreadPool(size)) {
			var futures = comms.stream()
				.map(comm -> pool.submit(() -> {
					var config = EvDSimTestFixture.createConfig(outputDirectory);
					config.controller().setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
					EvDSimTestFixture.configureDSim(config, 1 /* 1 partition per node */);

					var scenario = EvDSimTestFixture.createScenario(config, 3);
					var ctx = DistributedContext.create(comm, config);
					var controller = new Controler(scenario, ctx);
					EvDSimTestFixture.installEvModules(controller);
					controller.run();

					try {
						comm.close();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}))
				.toList();

			try {
				for (var f : futures) {
					f.get(1, TimeUnit.MINUTES);
				}
			} catch (ExecutionException | TimeoutException e) {
				pool.shutdownNow();
				throw e;
			}
		}

		// TODO: add DriveDischargeVerifier once multi-thread aggregation is solved
	}

	// -------------------------------------------------------------------------
	// Verifier
	// -------------------------------------------------------------------------

	/**
	 * Collects triggering events ({@link LinkLeaveEvent}, {@link VehicleLeavesTrafficEvent})
	 * and {@link DrivingEnergyConsumptionEvent}s, then asserts:
	 * <ol>
	 *   <li>Total energy == 1100 J (l2: 1000 J + l3: 100 J; l1 is the first link and is skipped).</li>
	 *   <li>For each discharge event: {@code dischargeEvent.time == triggeringEvent.time + timeDelta}
	 *       (timeDelta = 1 for QSim, 0 for DSim).</li>
	 * </ol>
	 */
	static class DriveDischargeVerifier
		implements DrivingEnergyConsumptionEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {

		private final int timeDelta;
		private final Map<Id<Link>, Double> triggerTimes = new HashMap<>();
		private final Map<Id<Link>, DrivingEnergyConsumptionEvent> dischargeEvents = new HashMap<>();

		DriveDischargeVerifier(int timeDelta) {
			this.timeDelta = timeDelta;
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			triggerTimes.put(event.getLinkId(), event.getTime());
		}

		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			triggerTimes.put(event.getLinkId(), event.getTime());
		}

		@Override
		public void handleEvent(DrivingEnergyConsumptionEvent event) {
			dischargeEvents.put(event.getLinkId(), event);
		}

		void install(Controler controller) {
			DriveDischargeVerifier self = this;
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(self);
				}
			});
		}

		void assertExpected() {
			double totalEnergy_J = dischargeEvents.values().stream()
				.mapToDouble(DrivingEnergyConsumptionEvent::getEnergy)
				.sum();
			assertEquals(EvUnits.J_to_kWh(1100.0), EvUnits.J_to_kWh(totalEnergy_J), 1e-6,
				"Total energy mismatch");

			for (var entry : dischargeEvents.entrySet()) {
				var linkId = entry.getKey();
				var discharge = entry.getValue();
				Double triggerTime = triggerTimes.get(linkId);
				assertNotNull(triggerTime, "No triggering event recorded for link " + linkId);
				assertEquals(triggerTime + timeDelta, discharge.getTime(), 1e-6,
					"Discharge event time mismatch for link " + linkId);
			}
		}
	}
}
