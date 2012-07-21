package playground.sergioo.passivePlanning.core.mobsim.passivePlanning.agents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.events.TravelEventImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;

import playground.sergioo.passivePlanning.api.population.BasePerson;
import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.definitions.HasBasePerson;
import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.definitions.SinglePlannerAgent;
import playground.sergioo.passivePlanning.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerAgent implements MobsimDriverAgent, HasBasePerson  {

	//Constants
	private final static Logger log = Logger.getLogger(TransitAgent.class);
	
	//Attributes
	private final BasePerson basePerson;
	private final Netsim simulation;
	private final PassivePlannerManager passivePlannerManager;
	protected SinglePlannerAgent planner;
	private MobsimVehicle vehicle;
	protected State state;
	private double activityEndTime;
	private int currentPlanElementIndex = 0;
	private int currentLinkIdIndex;
	
	//Constructors
	public PassivePlannerAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager) {
		this.basePerson = basePerson;
		this.simulation = simulation;
		this.passivePlannerManager = passivePlannerManager;
	}

	//Methods
	@Override
	public State getState() {
		return state;
	}
	@Override
	public double getActivityEndTime() {
		if(state == State.ACTIVITY)
			return activityEndTime;
		return Time.UNDEFINED_TIME;
	}
	@Override
	public void endActivityAndComputeNextState(double now) {
		if(state == State.ACTIVITY) {
			Activity prevAct = (Activity)getCurrentPlanElement();
			simulation.getEventsManager().processEvent(simulation.getEventsManager().getFactory().createActivityEndEvent(now, getId(), prevAct.getLinkId(), prevAct.getFacilityId(), prevAct.getType()));
			currentPlanElementIndex++;
			if(currentPlanElementIndex>=basePerson.getSelectedPlan().getPlanElements().size()) {
				log.error("Agent "+getId()+" has not more elements after the activity "+prevAct.getType()+" at "+now+" seconds.");
				abort(now);
				return;
			}
			advancePlan(now);
		}
	}
	
	@Override
	public void endLegAndComputeNextState(double now) {
		if(state == State.LEG) {
			Leg prevLeg = (Leg)getCurrentPlanElement();
			this.simulation.getEventsManager().processEvent(this.simulation.getEventsManager().getFactory().createAgentArrivalEvent(now, getId(), getDestinationLinkId(), prevLeg.getMode()));
			currentPlanElementIndex++;
			if(currentPlanElementIndex>=basePerson.getSelectedPlan().getPlanElements().size()) {
				log.error("Agent "+getId()+" has not more elements after the leg "+prevLeg.getMode()+" at "+now+" seconds.");
				abort(now);
				return;
			}
			advancePlan(now);
		}
	}
	private void advancePlan(double now) {
		PlanElement pe = this.getCurrentPlanElement() ;
		if (pe instanceof Activity)
			initializeActivity((Activity) pe, now);
		else if (pe instanceof Leg)
			initializeLeg((Leg) pe, now);
		else
			throw new RuntimeException("Unknown PlanElement of type: " + pe.getClass().getName());
	}
	private void initializeActivity(Activity act, double now) {
		this.state = MobsimAgent.State.ACTIVITY;
		this.simulation.getEventsManager().processEvent(this.simulation.getEventsManager().getFactory().createActivityStartEvent(now, this.getId(),  act.getLinkId(), act.getFacilityId(), act.getType()));
		activityEndTime = calculateDepartureTime(act, now, "");
	}
	private void initializeLeg(Leg leg, double now) {
		this.state = MobsimAgent.State.LEG ;			
		Route route = leg.getRoute();
		if (route == null) {
			log.error("The agent " + getId() + " has no route in its leg.  Setting agent state to ABORT (but continuing the mobsim).");
			abort(now);
		}
		else
			this.currentLinkIdIndex = 0;
	}
	private double calculateDepartureTime(Activity act, double now, String activityDurationInterpretation) {
		return 0;
	}
	protected PlanElement getCurrentPlanElement() {
		return basePerson.getSelectedPlan().getPlanElements().get(currentPlanElementIndex);
	}
	@Override
	public void abort(double now) {
		state = State.ABORT;
	}
	@Override
	public Double getExpectedTravelTime() {
		if(state == State.LEG)
			return ((Leg)getCurrentPlanElement()).getTravelTime();
		return null;
	}
	@Override
	public String getMode() {
		if(state == State.LEG)
			return ((Leg)getCurrentPlanElement()).getMode();
		return null;
	}
	@Override
	public void notifyTeleportToLink(Id linkId) {
		if(state == State.LEG)
			simulation.getEventsManager().processEvent(new TravelEventImpl(simulation.getSimTimer().getTimeOfDay(), getId(), ((Leg)getCurrentPlanElement()).getRoute().getDistance()));
	}
	@Override
	public Id getCurrentLinkId() {
		if(state == State.LEG) {
			Leg leg = (Leg)getCurrentPlanElement();
			if(leg.getRoute() instanceof NetworkRoute)
				return ((NetworkRoute)leg.getRoute()).getLinkIds().get(currentLinkIdIndex);
			else
				log.error("Agent "+getId()+" is a driver without NetworkRoute.");
		}
		return null;
	}
	@Override
	public Id getDestinationLinkId() {
		if(state == State.LEG)
			return ((Leg)getCurrentPlanElement()).getRoute().getEndLinkId();
		return null;
	}
	@Override
	public Id getId() {
		return basePerson.getId();
	}
	@Override
	public Id chooseNextLinkId() {
		if(state == State.LEG) {
			Leg leg = (Leg)getCurrentPlanElement();
			if(leg.getRoute() instanceof NetworkRoute) {
				NetworkRoute route = (NetworkRoute)leg.getRoute();
				if(currentLinkIdIndex<route.getLinkIds().size())
					return route.getLinkIds().get(currentLinkIdIndex);
				else if(currentLinkIdIndex==route.getLinkIds().size())
					return route.getEndLinkId();
			}
			else
				log.error("Agent "+getId()+" is a driver without NetworkRoute.");
		}
		return null;
	}
	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		if(state == State.LEG) {
			Leg leg = (Leg)getCurrentPlanElement();
			if(leg.getRoute() instanceof NetworkRoute) {
				NetworkRoute route = (NetworkRoute)leg.getRoute();
				if(route.getLinkIds().get(currentLinkIdIndex+1).equals(newLinkId))
					currentLinkIdIndex++;
				else
					log.error("Agent "+getId()+" is moving to a non expected link.");
			}
			else
				log.error("Agent "+getId()+" is a driver without NetworkRoute.");
		}
	}
	@Override
	public void setVehicle(MobsimVehicle vehicle) {
		this.vehicle = vehicle;
	}
	@Override
	public MobsimVehicle getVehicle() {
		return vehicle;
	}
	@Override
	public Id getPlannedVehicleId() {
		if(state == State.LEG)
			if(((Leg)getCurrentPlanElement()).getRoute() instanceof NetworkRoute)
				if(((NetworkRoute)((Leg)getCurrentPlanElement()).getRoute()).getVehicleId() != null)
					return ((NetworkRoute)((Leg)getCurrentPlanElement()).getRoute()).getVehicleId();
				else
					return basePerson.getId(); // we still assume the vehicleId is the agentId if no vehicleId is given.
			else
				log.error("Agent "+getId()+" is a driver without NetworkRoute.");
		return null;
	}
	@Override
	public BasePerson getBasePerson() {
		return basePerson;
	}
	protected double getWeight() {
		return 1/simulation.getScenario().getConfig().getQSimConfigGroup().getFlowCapFactor();
	}

}
