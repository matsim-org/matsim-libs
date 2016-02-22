package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.api.population.EmptyTime;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions.HasBasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions.SinglePlannerAgent;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public abstract class PassivePlannerDriverAgent implements MobsimDriverAgent, HasBasePerson, PlanAgent {

	//Constants
	private final static Logger log = Logger.getLogger(TransitAgent.class);
	
	//Attributes
	private final BasePerson basePerson;
	protected final Netsim simulation;
	private final PassivePlannerManager passivePlannerManager;
	protected SinglePlannerAgent planner;
	private MobsimVehicle vehicle;
	protected boolean isAborted = false;
	private double activityEndTime;
	private int currentLinkIdIndex;
	
	//Constructors
	public PassivePlannerDriverAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager) {
		this.basePerson = basePerson;
		this.simulation = simulation;
		this.passivePlannerManager = passivePlannerManager;
		if (basePerson.getSelectedPlan().getPlanElements().size() > 0) {
			Activity firstAct = (Activity) basePerson.getSelectedPlan().getPlanElements().get(0);				
			if(basePerson.getSelectedPlan().getPlanElements().size()==1)
				firstAct.setEndTime(Double.POSITIVE_INFINITY);
			activityEndTime = firstAct.getEndTime();
		}
	}

	//Methods
	public boolean isPlanned() {
		return !(getCurrentPlanElement() instanceof EmptyTime);
	}
	@Override
	public State getState() {
		if(isAborted)
			return State.ABORT;
		else
			return getCurrentPlanElement() instanceof Leg?State.LEG:State.ACTIVITY;
	}
	@Override
	public double getActivityEndTime() {
		if(getState() == State.ACTIVITY)
			return activityEndTime;
		return Time.UNDEFINED_TIME;
	}
	@Override
	public void endActivityAndComputeNextState(double now) {
		Activity prevAct = (Activity)getCurrentPlanElement();
		simulation.getEventsManager().processEvent(new ActivityEndEvent(now, getId(), prevAct.getLinkId(), prevAct.getFacilityId(), prevAct.getType()));
		planner.incrementPlanElementIndex();
		if(planner.getPlanElementIndex()>=basePerson.getSelectedPlan().getPlanElements().size()) {
			log.error("Agent "+getId()+" has not more elements after the activity "+prevAct.getType()+" at "+now+" seconds.");
			setStateToAbort(now);
			return;
		}
		if(getCurrentPlanElement() instanceof EmptyTime)
			passivePlannerManager.addPlanner(planner, prevAct.getFacilityId(), ((Activity)getNextPlanElement()).getFacilityId(), now, this.getId());
		advancePlan(now);
	}
	
	@Override
	public void endLegAndComputeNextState(double now) {
		Leg prevLeg = (Leg)getCurrentPlanElement();
		this.simulation.getEventsManager().processEvent(new PersonArrivalEvent(now, getId(), getDestinationLinkId(), prevLeg.getMode()));
		planner.incrementPlanElementIndex();
		if(planner.getPlanElementIndex()>=basePerson.getSelectedPlan().getPlanElements().size()) {
			log.error("Agent "+getId()+" has not more elements after the leg "+prevLeg.getMode()+" at "+now+" seconds.");
			setStateToAbort(now);
			return;
		}
		advancePlan(now);
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
		this.simulation.getEventsManager().processEvent(new ActivityStartEvent(now, this.getId(), act.getLinkId(), act.getFacilityId(), act.getType()));
		activityEndTime = calculateDepartureTime(act, now);
	}
	public void initializeLastActivity(Activity act, double now) {
		activityEndTime = calculateDepartureTime(act, now);
	}
	private void initializeLeg(Leg leg, double now) {
		Route route = leg.getRoute();
		if (route == null) {
			log.error("The agent " + getId() + " has no route in its leg.  Setting agent state to ABORT (but continuing the mobsim).");
			setStateToAbort(now);
		}
		else
			this.currentLinkIdIndex = 0;
	}
	private double calculateDepartureTime(Activity act, double now) {
		if(act.equals(basePerson.getSelectedPlan().getPlanElements().get(basePerson.getSelectedPlan().getPlanElements().size()-1))) {
			if(act.getEndTime()!=Time.UNDEFINED_TIME && act.getEndTime()!=Double.POSITIVE_INFINITY)
				log.error("last activity has end time < infty; setting it to infty");
			return Double.POSITIVE_INFINITY ;
		}
		else {
			double departure;
			if(act.getMaximumDuration() != Time.UNDEFINED_TIME && act.getEndTime() == Time.UNDEFINED_TIME) {
				departure = Math.min(act.getEndTime(), now + act.getMaximumDuration());
			} else if(act.getMaximumDuration() != Time.UNDEFINED_TIME) {
				departure = now + act.getMaximumDuration();
			} else {
				departure = act.getEndTime();
			}
			return departure;
		}
	}
	public PlanElement getPreviousPlanElement() {
		return basePerson.getSelectedPlan().getPlanElements().get(planner.getPlanElementIndex()-1);
	}
	@Override
	public PlanElement getCurrentPlanElement() {
		return basePerson.getSelectedPlan().getPlanElements().get(planner.getPlanElementIndex());
	}
	@Override
	public PlanElement getNextPlanElement() {
		return basePerson.getSelectedPlan().getPlanElements().get(planner.getPlanElementIndex()+1);
	}
	@Override
	public void setStateToAbort(double now) {
		isAborted = true;
	}
	@Override
	public Double getExpectedTravelTime() {
		return ((Leg)getCurrentPlanElement()).getTravelTime();
	}

    @Override
    public Double getExpectedTravelDistance() {
        return ((Leg)getCurrentPlanElement()).getRoute().getDistance();
    }

    @Override
	public String getMode() {
		return ((Leg)getCurrentPlanElement()).getMode();
	}
	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		
	}
	@Override
	public Id<Link> getCurrentLinkId() {
		if(getCurrentPlanElement() instanceof Activity)
			return ((Activity)getCurrentPlanElement()).getLinkId();
		else {
			Leg leg = (Leg)getCurrentPlanElement();
			if(leg.getRoute() instanceof NetworkRoute)
				if(currentLinkIdIndex==0)
					return ((NetworkRoute)leg.getRoute()).getStartLinkId();
				else if(currentLinkIdIndex==((NetworkRoute)leg.getRoute()).getLinkIds().size()+1)
					return ((NetworkRoute)leg.getRoute()).getEndLinkId();
				else if(currentLinkIdIndex>0 && currentLinkIdIndex<=((NetworkRoute)leg.getRoute()).getLinkIds().size())
					return ((NetworkRoute)leg.getRoute()).getLinkIds().get(currentLinkIdIndex-1);
				else
					return null;
			else 
				return leg.getRoute().getStartLinkId();
		}
	}
	@Override
	public Id<Link> getDestinationLinkId() {
		return ((Leg)getCurrentPlanElement()).getRoute().getEndLinkId();
	}
	@Override
	public Id<Person> getId() {
		return basePerson.getId();
	}
	@Override
	public Id<Link> chooseNextLinkId() {
		Leg leg = (Leg)getCurrentPlanElement();
		if(leg.getRoute() instanceof NetworkRoute) {
			NetworkRoute route = (NetworkRoute)leg.getRoute();
			if(route.getLinkIds().size()==0)
				return null;
			else if(currentLinkIdIndex<route.getLinkIds().size())
				return route.getLinkIds().get(currentLinkIdIndex);
			else if(currentLinkIdIndex==route.getLinkIds().size())
				return route.getEndLinkId();
		}
		else
			log.error("Agent "+getId()+" is a driver without NetworkRoute.");
		return null;
	}
	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		Leg leg = (Leg)getCurrentPlanElement();
		if(leg.getRoute() instanceof NetworkRoute) {
			NetworkRoute route = (NetworkRoute)leg.getRoute();
			if(currentLinkIdIndex<=route.getLinkIds().size()-1 && route.getLinkIds().get(currentLinkIdIndex).equals(newLinkId))
				currentLinkIdIndex ++;
			else if(route.getEndLinkId().equals(newLinkId))
				currentLinkIdIndex ++;
			else
				log.error("Agent "+getId()+" is moving to a non expected link.");
		}
		else
			log.error("Agent "+getId()+" is a driver without NetworkRoute.");
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
	public Id<Vehicle> getPlannedVehicleId() {
		if(((Leg)getCurrentPlanElement()).getRoute() instanceof NetworkRoute)
			if(((NetworkRoute)((Leg)getCurrentPlanElement()).getRoute()).getVehicleId() != null)
				return ((NetworkRoute)((Leg)getCurrentPlanElement()).getRoute()).getVehicleId();
			else
				return Id.create(basePerson.getId(), Vehicle.class); // we still assume the vehicleId is the agentId if no vehicleId is given.
		else
			log.error("Agent "+getId()+" is a driver without NetworkRoute.");
		return null;
	}
	@Override
	public BasePerson getBasePerson() {
		return basePerson;
	}
	public double getWeight() {
		return 1/simulation.getScenario().getConfig().qsim().getFlowCapFactor();
	}
	public void advanceToNextActivity(double now, double penalty) {
		this.simulation.getEventsManager().processEvent(new PersonDepartureEvent(now, getId(), ((Activity)getPreviousPlanElement()).getLinkId(), "empty"));
		this.simulation.getEventsManager().processEvent(new TeleportationArrivalEvent(now+penalty, basePerson.getId(), 4000*24));
		this.simulation.getEventsManager().processEvent(new PersonArrivalEvent(now+penalty, getId(), ((Activity)getNextPlanElement()).getLinkId(), "empty"));
		initializeActivity((Activity) getNextPlanElement(), now);
	}

	@Override
	public Plan getCurrentPlan() {
		return basePerson.getSelectedPlan();
	}
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// The following is the old condition: Being at the end of the plan means you arrive anyways, no matter if you are on the right or wrong link.
		// kai, nov'14
		if ( this.chooseNextLinkId()==null ) {
			return true ;
		} else {
			return false ;
		}
	}
	
	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}



}
