/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.pt;

import java.util.Iterator;
import java.util.ListIterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

/**
 * @author michaz
 */
public class TransitDriverAgentImpl extends AbstractTransitDriverAgent {

	private final EventsManager eventsManager;

	private static class PlanBuilder {

		final Plan plan = PopulationUtils.createPlan();

		static final String activityType = PtConstants.TRANSIT_ACTIVITY_TYPE;

		public void addTrip(NetworkRoute networkRoute, String transportMode) {
			Activity lastActivity;
			if (!plan.getPlanElements().isEmpty()) {
				lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
				assert lastActivity.getLinkId().equals(networkRoute.getStartLinkId());
			} else {
				lastActivity = PopulationUtils.createActivityFromLinkId(activityType, networkRoute.getStartLinkId());
				plan.addActivity(lastActivity);
			}
			Leg leg = PopulationUtils.createLeg(transportMode);
			leg.setRoute(networkRoute);
			plan.addLeg(leg);
			Activity activity = PopulationUtils.createActivityFromLinkId(activityType, networkRoute.getEndLinkId());
			plan.addActivity(activity);
		}

		public Plan build() {
			return plan;
		}

	}

	private final Umlauf umlauf;
	private final Iterator<UmlaufStueckI> iUmlaufStueck;
	private final ListIterator<PlanElement> iPlanElement;
	private NetworkRoute carRoute;
	private double departureTime;
	private PlanElement currentPlanElement;
	private TransitLine transitLine;
	private TransitRoute transitRoute;
	private Departure departure;
	private Scenario scenario;
	
	public TransitDriverAgentImpl(Umlauf umlauf,
			String transportMode,
			TransitStopAgentTracker thisAgentTracker, InternalInterface internalInterface) {
		super(internalInterface, thisAgentTracker);
		this.umlauf = umlauf;
		this.eventsManager = internalInterface.getMobsim().getEventsManager();
		this.scenario = internalInterface.getMobsim().getScenario() ;
		// (yy AbstractTransitDriverAgent already keeps both of them. kai, dec'15)
		this.iUmlaufStueck = this.umlauf.getUmlaufStuecke().iterator();
		Person driverPerson = PopulationUtils.getFactory().createPerson(Id.create("pt_" + umlauf.getId(), Person.class)); // we use the non-wrapped route for efficiency, but the leg has to return the wrapped one.
		PlanBuilder planBuilder = new PlanBuilder();
		for (UmlaufStueckI umlaufStueck : umlauf.getUmlaufStuecke()) {
			NetworkRoute carRoute2 = umlaufStueck.getCarRoute();
			Gbl.assertNotNull(carRoute2);
			planBuilder.addTrip(getWrappedCarRoute(carRoute2), transportMode);
		}
		Plan plan = planBuilder.build();
		driverPerson.addPlan(plan);
		driverPerson.setSelectedPlan(plan);
		setDriver(driverPerson);
		iPlanElement = plan.getPlanElements().listIterator();
		this.currentPlanElement = iPlanElement.next();
		setNextLeg();
	}

	@Override
	public void endActivityAndComputeNextState(final double now) {
		this.currentPlanElement = iPlanElement.next();
		sendTransitDriverStartsEvent(now);	
		
//		this.sim.arrangeAgentDeparture(this);
		this.state = MobsimAgent.State.LEG ;
//		this.sim.reInsertAgentIntoMobsim(this) ;

	}

	@Override
	public void endLegAndComputeNextState(final double now) {
		eventsManager.processEvent(
				new PersonArrivalEvent(now, this.getId(), this.getDestinationLinkId(), this.getCurrentLeg().getMode()));
		this.currentPlanElement = iPlanElement.next();
		if (this.iUmlaufStueck.hasNext()) {
			setNextLeg();
			if (this.departureTime < now) {
				this.departureTime = now;
			}

//			this.sim.arrangeActivityStart(this);
			this.state = MobsimAgent.State.ACTIVITY ;
//			this.sim.reInsertAgentIntoMobsim(this) ;


		} else {
			// inserting an activity with end time infinity.  one can debate if this is a hack:
			// * in general, a MobsimAgent can construct its path through the day on the fly
			// * in this particular instance, the agent pretends to have a plan
			// kai, mar'12
			
			this.state = MobsimAgent.State.ACTIVITY ;
			this.departureTime = Double.POSITIVE_INFINITY ;
			
		}
	}

	private void setNextLeg() {
		UmlaufStueckI umlaufStueck = this.iUmlaufStueck.next();
		if (umlaufStueck.isFahrt()) {
			setLeg(umlaufStueck.getLine(), umlaufStueck.getRoute(), umlaufStueck.getDeparture());
		} else {
			setWenden(umlaufStueck.getCarRoute());
		}
		init();
	}

	private void setWenden(NetworkRoute carRoute) {
		this.transitLine = null;
		this.transitRoute = null;
		this.departure = null;
		this.carRoute = carRoute;
	}

	private void setLeg(final TransitLine line, final TransitRoute route, final Departure departure) {
		this.transitLine = line;
		this.transitRoute = route;
		this.departure = departure;
		this.departureTime = departure.getDepartureTime();
		this.carRoute = route.getRoute();
	}

	@Override
	Leg getCurrentLeg() {
		return (Leg) this.currentPlanElement;
	}
	
	@Override
	public OptionalTime getExpectedTravelTime() {
		return ((Leg) this.currentPlanElement).getTravelTime();
	}

    @Override
    public Double getExpectedTravelDistance() {
        return ((Leg) this.currentPlanElement).getRoute().getDistance();
    }

    @Override
	public String getMode() {
		return ((Leg)this.currentPlanElement).getMode();
	}
	
	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		Route route = ((Leg)this.currentPlanElement).getRoute() ;
		return ((NetworkRoute)route).getVehicleId() ; 
	}

//	@Override
//	public Activity getCurrentActivity() {
//		return (Activity) this.currentPlanElement;
//	}

	@Override
	public PlanElement getCurrentPlanElement() {
		return this.currentPlanElement; 
	}

	@Override
	public PlanElement getNextPlanElement() {
		if (iPlanElement.hasNext()) {
			PlanElement next = iPlanElement.next(); // peek at the next element, but...
			iPlanElement.previous(); // ...rewind iterator by one step
			return next;
		} else {
			return null ;
		}
	}
	@Override
	public PlanElement getPreviousPlanElement() {
		if (iPlanElement.hasPrevious()) {
			PlanElement prev = iPlanElement.previous(); // peek at the element, but...
			iPlanElement.next(); // ...rewind iterator by one step
			return prev;
		} else {
			return null ;
		}
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return getCurrentLeg().getRoute().getEndLinkId();
	}

	@Override
	public NetworkRoute getCarRoute() {
		return this.carRoute;
	}

	@Override
	public TransitLine getTransitLine() {
		return this.transitLine;
	}

	@Override
	public TransitRoute getTransitRoute() {
		return this.transitRoute;
	}

	@Override
	public Departure getDeparture() {
		return this.departure;
	}

	@Override
	public double getActivityEndTime() {
		return this.departureTime;
	}
	
	@Override
	public Plan getCurrentPlan() {
		return PopulationUtils.unmodifiablePlan(this.getPerson().getSelectedPlan());
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
	public Facility getCurrentFacility() {
		PlanElement pe = this.getCurrentPlanElement() ;
		Activity activity ;
		if ( pe instanceof Activity ) {
			activity = (Activity) pe;
		} else if ( pe instanceof Leg ) {
			activity = (Activity) this.getPreviousPlanElement() ;
		} else {
			throw new RuntimeException("unexpected type of PlanElement") ;
		}
		return  FacilitiesUtils.toFacility( activity, this.scenario.getActivityFacilities() );

		// the above assumes alternating acts/legs.  I start having the suspicion that we should revoke our decision to give that up.
		// If not, one will have to use TripUtils to find the preceeding activity ... but things get more difficult.  Preferably, the
		// facility should then sit in the leg (since there it is used for routing).  kai, dec'15
	}

	@Override
	public Facility getDestinationFacility() {
		PlanElement pe = this.getCurrentPlanElement() ;
		if ( pe instanceof Leg ) {
			Activity activity = (Activity)this.getNextPlanElement() ;
			return  FacilitiesUtils.toFacility( activity, this.scenario.getActivityFacilities() );
		} else if ( pe instanceof Activity ) {
			return null ;
		}
		throw new RuntimeException("unexpected type of PlanElement") ;
	}

	
	

}
