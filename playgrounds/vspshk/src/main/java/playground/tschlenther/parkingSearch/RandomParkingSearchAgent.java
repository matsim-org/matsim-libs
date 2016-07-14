/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.tschlenther.parkingSearch;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel/schlenther
 *
 */
final class RandomParkingSearchAgent implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);
	
	private BasicPlanAgentImpl basicAgentDelegate ;
	private PlanBasedDriverAgentImpl driverAgentDelegate ;
	
	private boolean searchMode = true ;

	public RandomParkingSearchAgent(final Plan plan1, final Netsim simulation) {
		basicAgentDelegate = new BasicPlanAgentImpl(plan1, simulation.getScenario(), simulation.getEventsManager(), 
				simulation.getSimTimer() ) ;
		driverAgentDelegate = new PlanBasedDriverAgentImpl(basicAgentDelegate) ;
		
		// deliberately does NOT keep a back pointer to the whole Netsim; this should also be removed in the constructor call.
	}
	
	public RandomParkingSearchAgent(final Plan plan1, Scenario scen, MobsimTimer timer, EventsManager manager) {
		basicAgentDelegate = new BasicPlanAgentImpl(plan1, scen, manager,timer) ;
		driverAgentDelegate = new PlanBasedDriverAgentImpl(basicAgentDelegate) ;		
	}

	@Override
	public final void endLegAndComputeNextState(double now) {
		basicAgentDelegate.endLegAndComputeNextState(now);
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
		basicAgentDelegate.endActivityAndComputeNextState(now);
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
	public final Double getExpectedTravelTime() {
		return basicAgentDelegate.getExpectedTravelTime();
	}

    @Override
    public final Double getExpectedTravelDistance() {
        return basicAgentDelegate.getExpectedTravelDistance();
    }

    @Override
	public String toString() {
		return basicAgentDelegate.toString();
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
		if ( !this.searchMode ) {
			return driverAgentDelegate.chooseNextLinkId();
		} else {
			Link currentLink = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get( this.getCurrentLinkId() ) ;
			Node toNode = currentLink.getToNode() ;
			int idx = MatsimRandom.getRandom().nextInt( toNode.getOutLinks().size() ) ;
			for ( Link outLink : toNode.getOutLinks().values() ) {
				if ( idx==0 ) {
					return outLink.getId() ;
				}
				idx-- ;
			}
			throw new RuntimeException("should not happen" ) ;
		}
	}

	@Override
	public final boolean isWantingToArriveOnCurrentLink() {
		if ( !this.searchMode ) {
			return driverAgentDelegate.isWantingToArriveOnCurrentLink();
		} else {
			return false;
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

	@Override
	public PlanElement getPreviousPlanElement() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}
	
}
