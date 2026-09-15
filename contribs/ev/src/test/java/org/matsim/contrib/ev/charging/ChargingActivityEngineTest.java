package org.matsim.contrib.ev.charging;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.vehicles.Vehicle;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Behavior-path tests for {@link ChargingActivityEngine}.
 * <p>
 * Uses a mocked {@code ActivityEngine} as the delegate. The key interaction is that
 * VCH2 wraps the delegate's {@link InternalInterface} with its own
 * {@code InternalInterfaceDelegate}. Tests capture that delegate via
 * {@link ArgumentCaptor} and call {@code arrangeNextAgentState} on it directly to
 * simulate the agent waking up from its activity.
 */
public class ChargingActivityEngineTest {

	private static final Id<Link> CHARGER_LINK = Id.createLinkId("l1");
	private static final Id<Charger> CHARGER_ID = Id.create("charger", Charger.class);
	private static final double BATTERY_CAPACITY_J = 10_000.0;
	private static final double CHARGER_POWER_W = 1_000.0;

	private double simTime = 0.0;

	private List<Event> collectedEvents;
	private ChargingInfrastructure chargingInfrastructure;
	private Charger charger;
	private ElectricVehicle ev;
	private ElectricFleet electricFleet;

	@BeforeEach
	void setUp() {
		simTime = 0.0;

		collectedEvents = new ArrayList<>();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler((BasicEventHandler) collectedEvents::add);

		// Charger: 1 plug at CHARGER_LINK
		var spec = ImmutableChargerSpecification.newBuilder()
			.id(CHARGER_ID)
			.chargerType(ChargerSpecification.DEFAULT_CHARGER_TYPE)
			.linkId(CHARGER_LINK)
			.plugPower(CHARGER_POWER_W)
			.plugCount(1)
			.build();
		var infraSpec = ChargingInfrastructureUtils.createChargingInfrastructureSpecification();
		infraSpec.addChargerSpecification(spec);
		var config = new EvConfigGroup();
		config.setChargeTimeStep(1);

		var logicFactory = new ChargingWithQueueingLogic.Factory(eventsManager,
			_ -> (_, _) -> true,
			new DefaultChargerPower.Factory(config));

		// Build a minimal single-link network so ChargerDefaultImpl is happy
		var network = org.matsim.core.network.NetworkUtils.createNetwork();
		var n1 = org.matsim.core.network.NetworkUtils.createNode(Id.createNodeId("n1"));
		var n2 = org.matsim.core.network.NetworkUtils.createNode(Id.createNodeId("n2"));
		network.addNode(n1);
		network.addNode(n2);
		var link = org.matsim.core.network.NetworkUtils.createLink(CHARGER_LINK, n1, n2, network, 100, 10, 1000, 1);
		network.addLink(link);

		chargingInfrastructure = ChargingInfrastructureUtils.createChargingInfrastructure(
			infraSpec, network.getLinks()::get, logicFactory);
		charger = chargingInfrastructure.getChargers().get(CHARGER_ID);

		ev = createEv(Id.create("ev", Vehicle.class), BATTERY_CAPACITY_J, 1.0);
		var fleet = mock(ElectricFleet.class);
		when(fleet.getVehicle(ev.getId())).thenReturn(ev);
		when(fleet.hasVehicle(ev.getId())).thenReturn(true);
		electricFleet = fleet;
	}

	// -------------------------------------------------------------------------
	// Test 1: charging completes naturally before activity ends.
	// Verifies the notifyChargingEnded bug fix: no IllegalStateException.
	// -------------------------------------------------------------------------

	@Test
	void chargingCompletesNaturally_agentWakesUpWithoutException() {
		ev.getBattery().setCharge(9_000.0);

		var delegateEngine = mockDelegateEngine();
		var internalInterface = buildMockInternalInterface();
		var vch2 = buildVch2(new EvConfigGroup(), internalInterface, delegateEngine);
		var capturedDelegate = captureDelegate(delegateEngine);

		var agent = buildChargingAgent(Id.createPersonId("person"), ev.getId(), 200.0);
		assertTrue(vch2.handleActivity(agent));

		chargeVehicles(1.0);                        // plugs the EV
		assertEventFired(ChargingStartEvent.class, 1);
		assertEquals(9_000.0, ev.getBattery().getCharge(), 1e-6);

		simTime = 1.0;
		chargeVehicles(1.0);                        // delivers 1000 J → full → ChargingEndEvent
		assertEventFired(EnergyChargedEvent.class, 1);
		assertEventFired(ChargingEndEvent.class, 1);
		assertEquals(10_000.0, ev.getBattery().getCharge(), 1e-6);

		// Simulate the delegate engine deciding the activity is over.
		// With the bug fix this must not throw; without it, removeVehicle would
		// be called on an already-finished vehicle → IllegalStateException.
		capturedDelegate.arrangeNextAgentState(agent);

		verify(internalInterface).arrangeNextAgentState(agent);
	}

	// -------------------------------------------------------------------------
	// Test 2: agent departs while charging is still in progress.
	// -------------------------------------------------------------------------

	@Test
	void agentLeavesBeforeChargingCompletes_chargingEndedAtDeparture() {
		ev.getBattery().setCharge(0.0);

		var delegateEngine = mockDelegateEngine();
		var internalInterface = buildMockInternalInterface();
		var vch2 = buildVch2(new EvConfigGroup(), internalInterface, delegateEngine);
		var capturedDelegate = captureDelegate(delegateEngine);

		var agent = buildChargingAgent(Id.createPersonId("person"), ev.getId(), 0.5);
		vch2.handleActivity(agent);

		chargeVehicles(0.5);                        // plugs EV; no energy charged yet
		assertEventFired(ChargingStartEvent.class, 1);
		assertEventFired(ChargingEndEvent.class, 0); // not done yet

		// Activity ends → InternalInterfaceDelegate intercepts
		// → endChargingActivity removes vehicle → ChargingEndEvent fired
		capturedDelegate.arrangeNextAgentState(agent);

		assertEventFired(ChargingEndEvent.class, 1);
		verify(internalInterface).arrangeNextAgentState(agent);
	}

	// -------------------------------------------------------------------------
	// Test 3: plug full → agent queued → activity extended → plug opens → charging starts.
	// -------------------------------------------------------------------------

	@Test
	void agentQueued_activityExtendedWhileWaiting_removedFromQueueWhenChargingStarts() {
		// Pre-occupy the single plug with another EV.
		var otherEv = createEv(Id.create("other-ev", Vehicle.class), BATTERY_CAPACITY_J, 0.0);
		charger.getLogic().addVehicle(otherEv,
			new ChargeUpToMaxSocStrategy(charger.getSpecification(), otherEv, 1.0),
			new ChargingListener() {}, 0.0);
		chargeVehicles(1.0);  // otherEv plugged

		var delegateEngine = mockDelegateEngine();
		var internalInterface = buildMockInternalInterface();
		var config = new EvConfigGroup();
		config.setEnforceChargingInteractionDuration(true);
		var vch2 = buildVch2(config, internalInterface, delegateEngine);
		var capturedDelegate = captureDelegate(delegateEngine);

		var agent = buildChargingAgent(Id.createPersonId("person"), ev.getId(), 200.0);
		vch2.handleActivity(agent);

		simTime = 1.0;
		chargeVehicles(1.0);                        // ev goes to charger queue
		assertEventFired(QueuedAtChargerEvent.class, 1);

		// While in queue, doSimStep should keep requesting activity extension.
		vch2.doSimStep(1.0);
		vch2.doSimStep(2.0);
		verify(delegateEngine, times(2)).rescheduleActivityEnd(agent);

		// Agent must still be sleeping — arrangeNextAgentState not yet called.
		verify(internalInterface, never()).arrangeNextAgentState(agent);

		// Free the plug → ev promoted to active charging
		simTime = 2.0;

		// we should see one start event for the other ev. After removing other ev, we should see
		// two start events, as the agent's vehicle should start charging.
		assertEventFired(ChargingStartEvent.class, 1);
		charger.getLogic().removeVehicle(otherEv, simTime);
		assertEventFired(ChargingStartEvent.class, 2);

		// doSimStep after charging started: agent no longer in charger queue, no more extension.
		vch2.doSimStep(3.0);
		verify(delegateEngine, times(2)).rescheduleActivityEnd(agent); // still 2, no new call

		// Activity ends → capturedDelegate is called by the simulated delegate
		// before we have one end event from other ev. Then we should see two, as the agent's ev
		// ends charging as well
		assertEventFired(ChargingEndEvent.class, 1);
		capturedDelegate.arrangeNextAgentState(agent);
		assertEventFired(ChargingEndEvent.class, 2);

		// fired by removeVehicle on departure
		verify(internalInterface).arrangeNextAgentState(agent);
	}

	// -------------------------------------------------------------------------
	// Test 4: handleActivity returns false for non-charging scenarios.
	// -------------------------------------------------------------------------

	@Test
	void nonChargingActivity_handleActivityReturnsFalse() {
		var delegateEngine = mockDelegateEngine();
		var vch2 = buildVch2(new EvConfigGroup(), buildMockInternalInterface(), delegateEngine);

		var activity = PopulationUtils.getFactory().createActivityFromLinkId("work", CHARGER_LINK);
		var leg = PopulationUtils.getFactory().createLeg("car");
		var route = RouteUtils.createLinkNetworkRouteImpl(CHARGER_LINK, CHARGER_LINK);
		route.setVehicleId(ev.getId());
		leg.setRoute(route);
		var agent = buildPlanAgentMock(Id.createPersonId("p"), activity, leg, 200.0);

		assertFalse(vch2.handleActivity(agent));
		assertEquals(0, countEventsOfType(EnergyChargedEvent.class));
	}

	@Test
	void nonPlanAgent_handleActivityReturnsFalse() {
		var delegateEngine = mockDelegateEngine();
		var vch2 = buildVch2(new EvConfigGroup(), buildMockInternalInterface(), delegateEngine);

		// A plain MobsimAgent that does NOT implement PlanAgent.
		MobsimAgent nonPlanAgent = mock(MobsimAgent.class);
		assertFalse(vch2.handleActivity(nonPlanAgent));
	}

	@Test
	void routeWithoutVehicleId_handleActivityReturnsFalse() {
		var delegateEngine = mockDelegateEngine();
		var vch2 = buildVch2(new EvConfigGroup(), buildMockInternalInterface(), delegateEngine);

		Activity chargingAct = PopulationUtils.getFactory().createActivityFromLinkId(
			ChargingActivityEngine.CHARGING_INTERACTION, CHARGER_LINK);
		Leg leg = PopulationUtils.getFactory().createLeg("car");
		// Route that does NOT implement HasVehicleId
		leg.setRoute(RouteUtils.createGenericRouteImpl(CHARGER_LINK, CHARGER_LINK));

		var agent = buildPlanAgentMock(Id.createPersonId("p"), chargingAct, leg, 200.0);
		assertFalse(vch2.handleActivity(agent));
	}

	@Test
	void nonEvVehicle_handleActivityReturnsFalse() {
		var delegateEngine = mockDelegateEngine();
		var vch2 = buildVch2(new EvConfigGroup(), buildMockInternalInterface(), delegateEngine);

		var agent = buildChargingAgent(Id.createPersonId("p"), Id.create("non-ev", Vehicle.class), 200.0);
		assertFalse(vch2.handleActivity(agent));
	}

	// -------------------------------------------------------------------------
	// Test 5: enforceChargingInteractionDuration=true + quit queue → throws.
	// -------------------------------------------------------------------------

	@Test
	void enforceInteractionDuration_vehicleQuitsQueue_throws() {
		var otherEv = createEv(Id.create("other-ev", Vehicle.class), BATTERY_CAPACITY_J, 0.0);
		charger.getLogic().addVehicle(otherEv,
			new ChargeUpToMaxSocStrategy(charger.getSpecification(), otherEv, 1.0),
			new ChargingListener() {}, 0.0);
		chargeVehicles(1.0);  // plug occupied

		var evCfg = new EvConfigGroup();
		evCfg.setEnforceChargingInteractionDuration(true);
		var delegateEngine = mockDelegateEngine();
		var vch2 = buildVch2(evCfg, buildMockInternalInterface(), delegateEngine);

		var agent = buildChargingAgent(Id.createPersonId("person"), ev.getId(), 200.0);
		vch2.handleActivity(agent);

		simTime = 1.0;
		chargeVehicles(1.0);  // ev queued

		assertThrows(RuntimeException.class, () -> charger.getLogic().removeVehicle(ev, 2.0));
	}

	// -------------------------------------------------------------------------
	// Test 6: enforceChargingInteractionDuration=false + quit queue → clean removal.
	// -------------------------------------------------------------------------

	@Test
	void noEnforcement_vehicleQuitsQueue_agentRemovedCleanly() {
		var otherEv = createEv(Id.create("other-ev", Vehicle.class), BATTERY_CAPACITY_J, 0.0);
		charger.getLogic().addVehicle(otherEv,
			new ChargeUpToMaxSocStrategy(charger.getSpecification(), otherEv, 1.0),
			new ChargingListener() {}, 0.0);
		chargeVehicles(1.0);

		var delegateEngine = mockDelegateEngine();
		var internalInterface = buildMockInternalInterface();
		var vch2 = buildVch2(new EvConfigGroup(), internalInterface, delegateEngine);

		var agent = buildChargingAgent(Id.createPersonId("person"), ev.getId(), 200.0);
		vch2.handleActivity(agent);

		simTime = 1.0;
		chargeVehicles(1.0);                        // ev queued
		assertEventFired(QueuedAtChargerEvent.class, 1);

		// While in charger queue, activity is not being extended as we dont set enforceChargingInteractionDuration.
		vch2.doSimStep(1.0);
		verify(delegateEngine, never()).rescheduleActivityEnd(agent);

		// Agent quits queue → QuitQueueAtChargerEvent
		charger.getLogic().removeVehicle(ev, 2.0);
		assertEventFired(QuitQueueAtChargerEvent.class, 1);

		// After quit, doSimStep must no longer extend the activity.
		verify(internalInterface, never()).arrangeNextAgentState(agent);
		vch2.doSimStep(2.0);
		verify(delegateEngine, never()).rescheduleActivityEnd(agent); // no new calls
		verify(internalInterface, never()).arrangeNextAgentState(agent);
	}

	// -------------------------------------------------------------------------
	// Helpers — infrastructure and charging
	// -------------------------------------------------------------------------

	private void chargeVehicles(double period) {
		charger.getLogic().chargeVehicles(period, simTime);
	}

	private void assertEventFired(Class<? extends Event> type, int expectedCount) {
		assertEquals(expectedCount, countEventsOfType(type),
			"Expected " + expectedCount + " event(s) of type " + type.getSimpleName());
	}

	private int countEventsOfType(Class<? extends Event> type) {
		return (int) collectedEvents.stream().filter(type::isInstance).count();
	}

	private ElectricVehicle createEv(Id<Vehicle> vehicleId, double capacityJ, double initialSoc) {
		record TestEvSpec(Id<Vehicle> getId, Vehicle getMatsimVehicle, String getVehicleType,
		                  ImmutableList<String> getChargerTypes, double getBatteryCapacity,
		                  double getInitialSoc) implements ElectricVehicleSpecification {}

		var evSpec = new TestEvSpec(vehicleId, null, "electric",
			ImmutableList.of(ChargerSpecification.DEFAULT_CHARGER_TYPE), capacityJ, initialSoc);

		return ElectricFleetUtils.create(evSpec,
			vehicle -> (link, travelTime, linkEnterTime) -> {throw new UnsupportedOperationException();},
			vehicle -> (beginTime, duration, linkId) -> {throw new UnsupportedOperationException();},
			vehicle -> chargerSpec -> CHARGER_POWER_W);
	}

	private ChargingActivityEngine buildVch2(EvConfigGroup evCfg, InternalInterface internalInterface,
	                                         ActivityEngine delegateEngine) {
		var handler = new ChargingActivityEngine(
			chargingInfrastructure, electricFleet, evCfg,
			(chargerSpec, vehicle) -> new ChargeUpToMaxSocStrategy(chargerSpec, vehicle, 1.0),
			delegateEngine);
		handler.setInternalInterface(internalInterface);
		return handler;
	}

	// -------------------------------------------------------------------------
	// Helpers — mock construction
	// -------------------------------------------------------------------------

	/**
	 * Creates a mock {@link ActivityEngine} whose {@code handleActivity} returns {@code true}.
	 */
	private ActivityEngine mockDelegateEngine() {
		var delegateEngine = mock(ActivityEngine.class);
		when(delegateEngine.handleActivity(any())).thenReturn(true);
		return delegateEngine;
	}

	/**
	 * Captures the {@link InternalInterface} that VCH2 passes to the delegate engine's
	 * {@code setInternalInterface}. This is VCH2's internal {@code InternalInterfaceDelegate},
	 * which intercepts {@code arrangeNextAgentState} to clean up charging state.
	 * <p>
	 * Calling {@code capturedDelegate.arrangeNextAgentState(agent)} simulates the delegate
	 * engine deciding the agent's activity is over.
	 */
	private InternalInterface captureDelegate(ActivityEngine delegateEngine) {
		var captor = ArgumentCaptor.forClass(InternalInterface.class);
		verify(delegateEngine).setInternalInterface(captor.capture());
		return captor.getValue();
	}

	/**
	 * Creates a mock {@link InternalInterface} backed by a real {@link MobsimTimer}
	 * that reads from {@code simTime}. A mocked {@link Netsim} is returned from
	 * {@code getMobsim()}.
	 */
	private InternalInterface buildMockInternalInterface() {
		var timer = new MobsimTimer() {
			@Override
			public double getTimeOfDay() {return simTime;}
		};
		var agentCounter = mock(AgentCounter.class);
		var netsim = mock(Netsim.class);
		when(netsim.getSimTimer()).thenReturn(timer);
		when(netsim.getAgentCounter()).thenReturn(agentCounter);

		var internalInterface = mock(InternalInterface.class);
		when(internalInterface.getMobsim()).thenReturn(netsim);
		return internalInterface;
	}

	/**
	 * Creates an agent mock that implements both {@link MobsimAgent} and {@link PlanAgent},
	 * with a {@link ChargingActivityEngine#CHARGING_INTERACTION} activity preceded by a leg
	 * carrying the given vehicle ID.
	 */
	private MobsimAgent buildChargingAgent(Id<Person> personId, Id<Vehicle> vehicleId, double activityEndTime) {
		var chargingAct = PopulationUtils.getFactory()
			.createActivityFromLinkId(ChargingActivityEngine.CHARGING_INTERACTION, CHARGER_LINK);
		var leg = PopulationUtils.getFactory().createLeg("car");
		var route = RouteUtils.createLinkNetworkRouteImpl(CHARGER_LINK, CHARGER_LINK);
		route.setVehicleId(vehicleId);
		leg.setRoute(route);
		return buildPlanAgentMock(personId, chargingAct, leg, activityEndTime);
	}

	/**
	 * Builds a Mockito mock that satisfies {@link MobsimAgent}, {@link PlanAgent}, and
	 * {@link HasModifiablePlan} (the last is required by
	 * {@code WithinDayAgentUtils.resetCaches} called from VCH2's {@code doSimStep}).
	 */
	private MobsimAgent buildPlanAgentMock(Id<Person> personId, Activity currentAct, Leg prevLeg,
	                                       double activityEndTime) {
		var agent = mock(MobsimAgent.class,
			withSettings().extraInterfaces(PlanAgent.class, HasModifiablePlan.class));
		when(agent.getId()).thenReturn(personId);
		when(agent.getState()).thenReturn(MobsimAgent.State.ACTIVITY);
		when(agent.getActivityEndTime()).thenReturn(activityEndTime);
		when(agent.getCurrentLinkId()).thenReturn(CHARGER_LINK);

		var pa = (PlanAgent) agent;
		when(pa.getCurrentPlanElement()).thenReturn(currentAct);
		when(pa.getPreviousPlanElement()).thenReturn(prevLeg);

		return agent;
	}
}
