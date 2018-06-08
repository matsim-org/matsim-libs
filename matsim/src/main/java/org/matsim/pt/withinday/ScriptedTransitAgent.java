package org.matsim.pt.withinday;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.TransitAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class ScriptedTransitAgent implements MobsimDriverPassengerAgent, PlanAgent, HasPerson, HasModifiablePlan{

	private static final Logger log = Logger.getLogger(ScriptedTransitAgent.class);

	private BasicPlanAgentImpl basicAgentDelegate ;
	private PlanBasedDriverAgentImpl driverAgentDelegate ;
	private TransitAgentImpl transitAgentDelegate ;
	
	private MobsimTimer timer;
	private Scenario scenario;
	private WithinDayTransitEngine disruptionEngine;
	private ScriptedTransitBehavior behavior;

	public static ScriptedTransitAgent createTransitAgent(Person p, Netsim simulation, WithinDayTransitEngine engine) {
		ScriptedTransitAgent agent = new ScriptedTransitAgent(p, simulation, engine);
		return agent;
	}

	private ScriptedTransitAgent(final Person p, final Netsim simulation, final WithinDayTransitEngine engine) {
		basicAgentDelegate = new BasicPlanAgentImpl( p.getSelectedPlan(), simulation.getScenario(), simulation.getEventsManager(), 
				simulation.getSimTimer() ) ;
		driverAgentDelegate = new PlanBasedDriverAgentImpl( basicAgentDelegate ) ;
		transitAgentDelegate = new TransitAgentImpl( basicAgentDelegate );
		
		timer = simulation.getSimTimer();
		scenario = simulation.getScenario();
		disruptionEngine = engine;
	}

	@Override
	public final void endLegAndComputeNextState(double now) {
		String id = basicAgentDelegate.getId().toString();
		String cur = basicAgentDelegate.getCurrentPlanElement().toString();
		String priorState = basicAgentDelegate.getState().toString() + "("+cur+")";
		basicAgentDelegate.endLegAndComputeNextState(now);
		String post = basicAgentDelegate.getCurrentPlanElement().toString();
		String posteriorState = basicAgentDelegate.getState().toString() + "("+post+")";
		log.info("Agent "+id+" end leg: "+priorState+" -> "+posteriorState);
	}
	@Override
	public final void setStateToAbort(double now) {
		basicAgentDelegate.setStateToAbort(now);
	}
	@Override
	public final void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		basicAgentDelegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}
	@Override
	public final void endActivityAndComputeNextState(double now) {
		String id = basicAgentDelegate.getId().toString();
		String cur = basicAgentDelegate.getCurrentPlanElement().toString();
		String priorState = basicAgentDelegate.getState().toString() + "("+cur+")";
		basicAgentDelegate.endActivityAndComputeNextState(now);
		String post = basicAgentDelegate.getCurrentPlanElement().toString();
		String posteriorState = basicAgentDelegate.getState().toString() + "("+post+")";
		log.info("Agent "+id+" end activity: "+priorState+" -> "+posteriorState);
	}
	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		return basicAgentDelegate.getPlannedVehicleId();
	}
	@Override
	public final String getMode() {
		return basicAgentDelegate.getMode();
	}
	@Override
	public String toString() {
		return basicAgentDelegate.toString();
	}
	@Override
	public final Double getExpectedTravelTime() {
		return basicAgentDelegate.getExpectedTravelTime();
	}
	@Override
	public final Double getExpectedTravelDistance() {
		return basicAgentDelegate.getExpectedTravelDistance();
	}	
	@Override
	public final PlanElement getCurrentPlanElement() {
		return basicAgentDelegate.getCurrentPlanElement();
	}
	@Override
	public final PlanElement getNextPlanElement() {
		return basicAgentDelegate.getNextPlanElement();
	}
	@Override
	public final Plan getCurrentPlan() {
		return basicAgentDelegate.getCurrentPlan();
	}
	@Override
	public final Id<Person> getId() {
		return basicAgentDelegate.getId();
	}
	@Override
	public final Person getPerson() {
		return basicAgentDelegate.getPerson();
	}
	@Override
	public final MobsimVehicle getVehicle() {
		return basicAgentDelegate.getVehicle();
	}
	@Override
	public final void setVehicle(MobsimVehicle vehicle) {
		basicAgentDelegate.setVehicle(vehicle);
	}
	@Override
	public final Id<Link> getCurrentLinkId() {
		return basicAgentDelegate.getCurrentLinkId();
	}
	@Override
	public final Id<Link> getDestinationLinkId() {
		return basicAgentDelegate.getDestinationLinkId();
	}
	@Override
	public final double getActivityEndTime() {
		return basicAgentDelegate.getActivityEndTime();
	}
	@Override
	public final State getState() {
		return basicAgentDelegate.getState();
	}
	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		driverAgentDelegate.notifyMoveOverNode(newLinkId);
	}
	@Override
	public final Id<Link> chooseNextLinkId() {
		return driverAgentDelegate.chooseNextLinkId();
	}
	@Override
	public final boolean isWantingToArriveOnCurrentLink() {
		return driverAgentDelegate.isWantingToArriveOnCurrentLink();
	}
	@Override
	public final boolean getExitAtStop(TransitStopFacility stop) {
		return transitAgentDelegate.getExitAtStop(stop);
	}
	@Override
	public final boolean getEnterTransitRoute(TransitLine line, TransitRoute transitRoute, List<TransitRouteStop> stopsToCome,
			TransitVehicle transitVehicle) {
		
		// Step 1: if this line or route is currently disrupted, return false
		if (!disruptionEngine.isAvailable(line, transitRoute)) {
			return false;
		}
		
		// Step 2: if this line or route is according to plan, return true (?)
		boolean answer = transitAgentDelegate.getEnterTransitRoute(line, transitRoute, stopsToCome, transitVehicle);
		if (answer) {
			return true;
		}
		
		// Step 3: if the stopsToCome do not contain the planned goal, do nothing
		boolean isCandidate;
		PlanElement cur = basicAgentDelegate.getCurrentPlanElement();
		Leg leg;
		Id<TransitLine> plannedLineId;
		Id<TransitRoute> plannedRouteId;
		if (cur instanceof Leg) {
			leg = (Leg) cur;
			Route route = leg.getRoute();
			if (route instanceof ExperimentalTransitRoute) {
				ExperimentalTransitRoute etr = (ExperimentalTransitRoute) route;
				plannedLineId = etr.getLineId();
				plannedRouteId = etr.getRouteId();
				isCandidate = stopsToCome.stream()
						                 .anyMatch(trs -> trs.getStopFacility().getId().equals(etr.getEgressStopId()));
			}
			else {
				throw new IllegalStateException("Agent waiting for public transport does not have a ExperimentalTransitRoute as its route");
			}
		}
		else {
			throw new IllegalStateException("Agent waiting for the public transport does not have a Leg as the current PlanElement");
		}

		if (!isCandidate) {
			return false;
		}
		
		// Step 4: if present, delegate to the ScriptedTransitBehavior
		if (behavior != null) {
			double time = timer.getTimeOfDay();
			TransitRoute plannedRoute = scenario.getTransitSchedule().getTransitLines().get(plannedLineId).getRoutes().get(plannedRouteId);
			return behavior.enterVehicle(basicAgentDelegate.getPerson(), time, line, transitRoute, stopsToCome, leg, plannedRoute);
		}
		
		//String id = transitAgentDelegate.getId().toString();
		//log.info("Person "+id+" getEnterTransitRoute "+line+"/"+transitRoute+" : "+answer);
		return answer;
	}
	@Override
	public final double getWeight() {
		return transitAgentDelegate.getWeight();
	}
	@Override
	public final Id<TransitStopFacility> getDesiredAccessStopId() {
		return transitAgentDelegate.getDesiredAccessStopId();
	}
	@Override
	public final Id<TransitStopFacility> getDesiredDestinationStopId() {
		return transitAgentDelegate.getDesiredDestinationStopId();
	}

	@Override
	public final PlanElement getPreviousPlanElement() {
		return this.basicAgentDelegate.getPreviousPlanElement();
	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		return this.basicAgentDelegate.getCurrentFacility();
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		return this.basicAgentDelegate.getDestinationFacility();
	}

	@Override
	public Plan getModifiablePlan() {
		return this.basicAgentDelegate.getModifiablePlan();
	}

	@Override
	public void resetCaches() {
		this.basicAgentDelegate.resetCaches();
		this.driverAgentDelegate.resetCaches();
	}

	@Override
	public int getCurrentLinkIndex() {
		return this.basicAgentDelegate.getCurrentLinkIndex();
	}

	
}
