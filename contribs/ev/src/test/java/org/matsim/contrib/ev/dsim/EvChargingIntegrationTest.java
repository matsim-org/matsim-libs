package org.matsim.contrib.ev.dsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.*;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEvent;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEventHandler;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test for EV charging in both QSim and DSim settings.
 * <p>
 * Scenario: one EV drives from home (l1) to a charger (l2), charges until full,
 * then continues to work (l3). The charger is at l2, which sits on partition 1 of the
 * three-partition network.
 * <p>
 * Tests verify:
 * <ul>
 *   <li>Exactly one {@link ChargingStartEvent} and one {@link ChargingEndEvent} are fired.</li>
 *   <li>Total charged energy equals the exact shortfall: capacity − charge-at-arrival.</li>
 *   <li>Battery is fully charged at the end of the charging event.</li>
 *   <li>Drive consumption (l2 + l3) equals the same 1,100 J as in {@link EvDriveDischargingIntegrationTest}.</li>
 * </ul>
 *
 * @see EvDSimTestFixture
 */
public class EvChargingIntegrationTest {

	// -------------------------------------------------------------------------
	// Constants — energy budget
	// -------------------------------------------------------------------------

	/** EV starts at 50 % state-of-charge. */
	private static final double INITIAL_SOC = 0.5;

	/**
	 * l2 is 1,000 m long; the drive model charges 1 J/m.
	 * The agent consumes this while driving from l1 to the charger at l2.
	 */
	private static final double L2_CONSUMPTION_J = 1_000.0;

	/**
	 * Energy needed to reach 100 % SOC after arriving at the charger.
	 * = RANGE_M − (RANGE_M × INITIAL_SOC − L2_CONSUMPTION_J)
	 * = 10 000 − (5 000 − 1 000) = 6 000 J
	 */
	private static final double EXPECTED_CHARGED_J =
		EvDSimTestFixture.RANGE_M - (EvDSimTestFixture.RANGE_M * INITIAL_SOC - L2_CONSUMPTION_J);

	/**
	 * Total drive consumption: l2 (1 000 J) when driving to charger + l3 (100 J) when leaving.
	 * Same value as in {@link EvDriveDischargingIntegrationTest}.
	 */
	private static final double EXPECTED_DRIVE_J = 1_100.0;

	// -------------------------------------------------------------------------
	// Tests
	// -------------------------------------------------------------------------

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void qsim() {
		var config = EvDSimTestFixture.createConfig(utils.getOutputDirectory());
		var scenario = createScenario(config, 1);

		var controller = new Controler(scenario);
		installModules(controller);

		var verifier = new ChargingVerifier();
		verifier.install(controller);

		controller.run();
		verifier.assertExpected();
	}

	@Test
	void threePartitions() {
		var config = EvDSimTestFixture.createConfig(utils.getOutputDirectory());
		EvDSimTestFixture.configureDSim(config, 3);
		var scenario = createScenario(config, 3);

		var controller = new Controler(scenario);
		installModules(controller);

		var verifier = new ChargingVerifier();
		verifier.install(controller);

		controller.run();
		verifier.assertExpected();
	}

	// -------------------------------------------------------------------------
	// Scenario construction
	// -------------------------------------------------------------------------

	private static Scenario createScenario(org.matsim.core.config.Config config, int numParts) {
		var scenario = ScenarioUtils.createScenario(config);

		var network = EvDSimTestFixture.createNetwork(numParts);
		network.getNodes().values().forEach(n -> scenario.getNetwork().addNode(n));
		network.getLinks().values().forEach(l -> scenario.getNetwork().addLink(l));

		var evType = EvDSimTestFixture.createEvVehicleType(scenario.getVehicles());

		var vehicleId = Id.createVehicleId("ev-test-person");
		var vehicle = scenario.getVehicles().getFactory().createVehicle(vehicleId, evType);
		scenario.getVehicles().addVehicle(vehicle);
		ElectricFleetUtils.setInitialSoc(vehicle, INITIAL_SOC);

		var f = scenario.getPopulation().getFactory();
		var plan = f.createPlan();

		// home → car → charger (l2) → charging → car → work (l3)
		var home = f.createActivityFromLinkId("home", Id.createLinkId("l1"));
		home.setEndTime(10.0);
		plan.addActivity(home);

		var leg1 = f.createLeg(TransportMode.car);
		leg1.setRoutingMode(TransportMode.car);
		var route1 = RouteUtils.createLinkNetworkRouteImpl(
			Id.createLinkId("l1"), Collections.emptyList(), Id.createLinkId("l2"));
		route1.setVehicleId(vehicleId); // required by VehicleChargingHandler2 (reads vehicleId from route)
		leg1.setRoute(route1);
		plan.addLeg(leg1);

		var chargingAct = f.createActivityFromLinkId(
			VehicleChargingHandler2.CHARGING_INTERACTION, Id.createLinkId("l2"));
		chargingAct.setMaximumDuration(200.0); // 200 s >> ~44 s needed to fully charge at 50 C
		plan.addActivity(chargingAct);

		var leg2 = f.createLeg(TransportMode.car);
		leg2.setRoutingMode(TransportMode.car);
		var route2 = RouteUtils.createLinkNetworkRouteImpl(
			Id.createLinkId("l2"), Collections.emptyList(), Id.createLinkId("l3"));
		route2.setVehicleId(vehicleId);
		leg2.setRoute(route2);
		plan.addLeg(leg2);

		plan.addActivity(f.createActivityFromLinkId("work", Id.createLinkId("l3")));

		var person = f.createPerson(Id.createPersonId("ev-test-person"));
		person.addPlan(plan);
		VehicleUtils.insertVehicleIdsIntoPersonAttributes(person,
			Collections.singletonMap(TransportMode.car, vehicleId));
		scenario.getPopulation().addPerson(person);

		return scenario;
	}

	// -------------------------------------------------------------------------
	// Module installation
	// -------------------------------------------------------------------------

	private static void installModules(Controler controller) {
		controller.addOverridingModule(new EvModule());
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(org.matsim.contrib.ev.discharging.DriveEnergyConsumption.Factory.class)
					.toInstance(EvDSimTestFixture.DRIVE_CONSUMPTION);
				bind(org.matsim.contrib.ev.discharging.AuxEnergyConsumption.Factory.class)
					.toInstance(EvDSimTestFixture.AUX_CONSUMPTION);
				bind(ChargingInfrastructureSpecification.class)
					.toInstance(createChargingInfraSpec());
				// 50 C rate so charging completes in ~44 sim steps instead of ~2 160.
				// The default (1 C) would require the agent to charge for 36 minutes,
				// far exceeding the simulation end time.  Total energy charged is
				// independent of rate since ChargingWithQueueingLogic caps the last step
				// at (capacity − currentCharge).
				bind(ChargingPower.Factory.class)
					.toInstance(ev -> new FixedSpeedCharging(ev, 50.0));
			}
		});
	}

	private static ChargingInfrastructureSpecification createChargingInfraSpec() {
		var spec = new ChargingInfrastructureSpecificationDefaultImpl();
		spec.addChargerSpecification(ImmutableChargerSpecification.newBuilder()
			.id(Id.create("charger-l2", Charger.class))
			.linkId(Id.createLinkId("l2"))
			.chargerType(ChargerSpecification.DEFAULT_CHARGER_TYPE)
			// 1 kW plug power >> vehicle's actual 50 C rate (~0.04 mW at 10 kJ capacity).
			// DefaultChargerPower.calcMaximumEnergyToCharge = plugPower * chargeTimeStep,
			// so this never limits the per-step energy; the vehicle rate limits instead.
			.plugPower(1.0)
			.plugCount(1)
			.build());
		return spec;
	}

	// -------------------------------------------------------------------------
	// Verifier
	// -------------------------------------------------------------------------

	/**
	 * Collects charging and driving events, then asserts:
	 * <ol>
	 *   <li>Exactly one {@link ChargingStartEvent} and one {@link ChargingEndEvent}.</li>
	 *   <li>Total charged energy == {@link #EXPECTED_CHARGED_J} converted to kWh.</li>
	 *   <li>Battery is at full capacity ({@code RANGE_M} J) when charging ends.</li>
	 *   <li>Total drive consumption == {@link #EXPECTED_DRIVE_J} converted to kWh.</li>
	 * </ol>
	 */
	static class ChargingVerifier
		implements EnergyChargedEventHandler, ChargingStartEventHandler,
		ChargingEndEventHandler, DrivingEnergyConsumptionEventHandler {

		private int chargingStartCount = 0;
		private int chargingEndCount = 0;
		private double totalCharged_kWh = 0.0;
		private double batteryAtChargingEnd_kWh = Double.NaN;
		private double totalDriving_kWh = 0.0;

		@Override
		public void handleEvent(ChargingStartEvent event) {
			chargingStartCount++;
		}

		@Override
		public void handleEvent(ChargingEndEvent event) {
			chargingEndCount++;
			batteryAtChargingEnd_kWh = event.getCharge();
		}

		@Override
		public void handleEvent(EnergyChargedEvent event) {
			totalCharged_kWh += event.getEnergy();
		}

		@Override
		public void handleEvent(DrivingEnergyConsumptionEvent event) {
			totalDriving_kWh += event.getEnergy();
		}

		void install(Controler controller) {
			ChargingVerifier self = this;
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(self);
				}
			});
		}

		void assertExpected() {
			assertEquals(1, chargingStartCount, "Expected exactly one ChargingStartEvent");
			assertEquals(1, chargingEndCount, "Expected exactly one ChargingEndEvent");
			assertEquals(EvUnits.J_to_kWh(EXPECTED_CHARGED_J), totalCharged_kWh, 1e-9,
				"Total charged energy mismatch");
			assertEquals(EvUnits.J_to_kWh(EvDSimTestFixture.RANGE_M), batteryAtChargingEnd_kWh, 1e-9,
				"Battery should be fully charged when ChargingEndEvent fires");
			assertEquals(EvUnits.J_to_kWh(EXPECTED_DRIVE_J), totalDriving_kWh, 1e-9,
				"Drive consumption: l2 (1 000 J) + l3 (100 J)");
		}
	}
}
