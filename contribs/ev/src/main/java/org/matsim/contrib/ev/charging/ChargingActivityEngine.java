package org.matsim.contrib.ev.charging;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.mobsim.dsim.DistributedActivityHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class handles activities of type `** charging interaction`. It takes care of plugging electric vehicles into a suitable charger
 * It will unplug electriv vehicles if the activity ends before the vehicle is fully charged. It will keep the agent at the activity until
 * the activity end time even when the vehicle is fully charged.
 * <p>
 * Internally, a default bound {@link ActivityEngine} is used to manage agents at the activity.
 */
public class ChargingActivityEngine implements DistributedActivityHandler, DistributedMobsimEngine, ChargingListener {

	public static final String CHARGING_IDENTIFIER = " charging";
	public static final String CHARGING_INTERACTION = ScoringConfigGroup.createStageActivityType(
		CHARGING_IDENTIFIER);

	private final ChargingInfrastructure chargingInfrastructure;
	private final ElectricFleet electricFleet;
	private final ImmutableListMultimap<Id<Link>, Charger> chargersAtLinks;
	private final EvConfigGroup evCfg;
	private final ChargingStrategy.Factory strategyFactory;
	private final ActivityEngine delegateEngine;

	private final HashMap<Id<Person>, ChargingActivity> personsCharging = new HashMap<>();
	private final HashMap<Id<Vehicle>, Id<Person>> vehiclesAtCharger = new HashMap<>();

	private final Set<Id<Person>> agentsInChargerQueue = new HashSet<>();

	private InternalInterface internalInterface;

	@Inject
	public ChargingActivityEngine(ChargingInfrastructure chargingInfrastructure, ElectricFleet electricFleet, EvConfigGroup evCfg, ChargingStrategy.Factory strategyFactory, ActivityEngine delegateEngine) {
		this.chargingInfrastructure = chargingInfrastructure;
		this.electricFleet = electricFleet;
		this.chargersAtLinks = ChargingInfrastructureUtils.getChargersAtLinks(chargingInfrastructure);
		this.evCfg = evCfg;
		this.strategyFactory = strategyFactory;
		this.delegateEngine = delegateEngine;
	}

	@Override
	public void doSimStep(double time) {

		// the idea of the following is to delay the activity end time as long as agents are waiting in the
		// charger queue. This way we ensure that agents/vehicles have sufficient time for their charging
		// activity. The original code would use WithinDayAgentUtils.rescheduleActivityEnd for this. However,
		// we know that the agent should be in our engine. Therefore, we can directly reschedule the activity
		// end
		if (evCfg.isEnforceChargingInteractionDuration()) {
			for (var agentId : agentsInChargerQueue) {
				// SAFETY: We know that we have a plan agent because we test for it in handleActivity.
				var agent = personsCharging.get(agentId).agent;
				checkReplanningConditions(agent);

				// this magically sets the activity end time to now + actDur. Apparently, agents have access to the
				// sim timer internally...
				WithinDayAgentUtils.resetCaches(agent);
				rescheduleActivityEnd(agent);
			}
		}
		delegateEngine.doSimStep(time);
	}

	private static void checkReplanningConditions(MobsimAgent agent) {
		Preconditions.checkArgument(agent instanceof PlanAgent, "agent must be a plan agent");
		var pa = (PlanAgent) agent;
		Preconditions.checkArgument(pa.getCurrentPlanElement() instanceof Activity, "agent must have an activity as current plan element");
	}

	@Override
	public boolean handleActivity(MobsimAgent agent) {

		// this assumes that we only handle PlanAgents. I decided that this is a feasable assumption, as the old implementation contained
		// replanning code to delay the activity end times which only works on PlanAgents. If this assumption is too restrictive, because
		// we have agents charging vehicles without a backing plan, this engine could also listen for PersonLeavesVehicleEvents and infer
		// the vehicle id from those events. janek Mar' 2026
		if (agent instanceof PlanAgent pa) {
			var currentElement = pa.getCurrentPlanElement();
			var prevElement = pa.getPreviousPlanElement();

			if (currentElement instanceof Activity a && prevElement instanceof Leg l) {
				if (a.getType().endsWith(CHARGING_INTERACTION)) {
					return handleActivity(agent, a, l);
				}
			}
		}
		return false;
	}

	private boolean handleActivity(MobsimAgent ma, Activity currentAct, Leg prevLeg) {
		var route = prevLeg.getRoute();
		if (route instanceof HasVehicleId hvid) {

			var vehicleId = hvid.getVehicleId();

			if (electricFleet.hasVehicle(vehicleId)) {
				var ev = electricFleet.getVehicle(vehicleId);
				var charger = getSuitableCharger(currentAct.getLinkId(), ev);
				var strategy = strategyFactory.createStrategy(charger.getSpecification(), ev);
				var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
				charger.getLogic().addVehicle(ev, strategy, this, now);
				var chargingActivity = new ChargingActivity(charger.getId(), vehicleId, ma);
				personsCharging.put(ma.getId(), chargingActivity);
				vehiclesAtCharger.put(vehicleId, ma.getId());
				// we setup all the charging stuff first. The activity engine might decide that it
				// wants to pass the agent to arrangeNextAgentState immediately. Since we have setup
				// everything before passing the agent to the delegateEngine, we can revert all the
				// bookkeeping in handleActivityEnd, as if the agent would regularly wake up from its activity
				var wasAccepted = delegateEngine.handleActivity(ma);
				if (!wasAccepted) {
					throw new RuntimeException("VehicleChargingHandler expects the default activity engine to accept agents. ");
				}
				return true;
			}
		}
		return false;
	}


	private void endChargingActivity(MobsimAgent agent) {
		var chargingActivity = personsCharging.remove(agent.getId());
		if (chargingActivity == null) return;

		// vehiclesAtCharger is removed in notifyChargingEnded when charging completes naturally,
		// or here when the agent departs while charging is still in progress.
		var wasStillCharging = vehiclesAtCharger.remove(chargingActivity.vehicleId) != null;
		if (wasStillCharging) {
			var charger = chargingInfrastructure.getChargers().get(chargingActivity.chargerId);
			var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
			charger.getLogic().removeVehicle(electricFleet.getVehicle(chargingActivity.vehicleId), now);
		}
	}

	private Charger getSuitableCharger(Id<Link> linkId, ElectricVehicle ev) {
		return chargersAtLinks.get(linkId).stream()
			.filter(c -> ev.getChargerTypes().contains(c.getChargerType()))
			.findAny()
			.orElseThrow();
	}

	@Override
	public void rescheduleActivityEnd(MobsimAgent agent) {
		// ActivityEngineDefaultImpl happily reschedules and registers agents that it does not manage.
		// Therefore, only delegate requests for agents which we actually have delegated to our default engine.
		if (this.personsCharging.containsKey(agent.getId())) {
			delegateEngine.rescheduleActivityEnd(agent);
		}
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		delegateEngine.setInternalInterface(new InternalInterfaceDelegate());
	}

	@Override
	public void notifyVehicleQueued(ElectricVehicle ev, double now) {
		var driver = vehiclesAtCharger.get(ev.getId());
		this.agentsInChargerQueue.add(driver);
	}

	@Override
	public void notifyVehicleQuitChargerQueue(ElectricVehicle ev, double now) {
		if (evCfg.isEnforceChargingInteractionDuration()) {
			//this could actually happen when combining with edrt/etaxi/evrp
			throw new RuntimeException("should currently not happen, as this event is only triggered in case the agent quits the charger queue without charging afterwards, " +
				" and this should not happen with fixed charging activity duration.\n" +
				"If you run evrp together with conventional (preplanned) EV, please refer to VSP.");
		} else {
			var driver = vehiclesAtCharger.get(ev.getId());
			agentsInChargerQueue.remove(driver);
		}
	}

	@Override
	public void notifyChargingStarted(ElectricVehicle ev, double now) {
		var driver = vehiclesAtCharger.get(ev.getId());
		agentsInChargerQueue.remove(driver);
	}

	@Override
	public void notifyChargingEnded(ElectricVehicle ev, double now) {
		// Charging completed naturally (strategy satisfied). Remove vehicle from tracking so that
		// endChargingActivity() does not attempt to call removeVehicle() on an already-finished vehicle.
		vehiclesAtCharger.remove(ev.getId());
	}


	private class InternalInterfaceDelegate implements InternalInterface {
		@Override
		public void arrangeNextAgentState(MobsimAgent agent) {
			// intercept when agents wake up.
			endChargingActivity(agent);
			internalInterface.arrangeNextAgentState(agent);
		}

		@Override
		public void registerAdditionalAgentOnLink(MobsimAgent agent) {
			internalInterface.registerAdditionalAgentOnLink(agent);
		}

		@Override
		public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
			return internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
		}

		@Override
		public Netsim getMobsim() {
			return internalInterface.getMobsim();
		}

		@Override
		public Collection<? extends DepartureHandler> getDepartureHandlers() {
			return internalInterface.getDepartureHandlers();
		}
	}

	private static class ChargingActivity {
		private final Id<Charger> chargerId;
		private final Id<Vehicle> vehicleId;
		private final MobsimAgent agent;

		private ChargingActivity(Id<Charger> chargerId, Id<Vehicle> vehicleId, MobsimAgent agent) {
			this.chargerId = chargerId;
			this.vehicleId = vehicleId;
			this.agent = agent;
		}
	}
}
