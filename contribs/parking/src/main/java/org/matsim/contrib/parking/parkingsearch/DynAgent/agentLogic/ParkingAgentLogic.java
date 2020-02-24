/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.contrib.dynagent.IdleDynActivity;
import org.matsim.contrib.dynagent.StaticPassengerDynLeg;
import org.matsim.contrib.parking.parkingsearch.DynAgent.ParkingDynLeg;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.vehicles.Vehicle;


/**
 * @author jbischoff
 *
 */
public class ParkingAgentLogic implements DynAgentLogic {

	public enum LastParkActionState  {
			
			// we have the following cases of ending dynacts:
			NONCARTRIP,	// non-car trip arrival: start Activity
			CARTRIP, // car-trip arrival: add park-car activity 
			PARKACTIVITY, // park-car activity: get next PlanElement & add walk leg to activity location
			WALKFROMPARK ,// walk-leg to act: start next PlanElement Activity
			ACTIVITY, // ordinary activity: get next Leg, if car: go to car, otherwise add ordinary leg by other mode
			WALKTOPARK, // walk-leg to car: add unpark activity
			UNPARKACTIVITY // unpark activity: find the way to the next route & start leg
	}
	protected LastParkActionState lastParkActionState;
	protected DynAgent agent;
	protected Iterator<PlanElement> planElemIter;
	protected Plan plan;
	protected PlanElement currentPlanElement;
	protected ParkingSearchManager parkingManager;
	private RoutingModule walkRouter;
	private Network network;
	protected ParkingRouter parkingRouter;
	protected MobsimTimer timer;
	protected EventsManager events;
	protected ParkingSearchLogic parkingLogic;
	protected VehicleTeleportationLogic teleportationLogic;
	protected boolean isinitialLocation = true;
	protected Id<Vehicle> currentlyAssignedVehicleId = null;
	protected String stageInteractionType = null;
	private ParkingSearchConfigGroup configGroup;
	private static final Logger log = Logger.getLogger(ParkingAgentLogic.class);

	/**
	 * @param plan
	 *            (always starts with Activity)
	 */
	public ParkingAgentLogic(Plan plan, ParkingSearchManager parkingManager, RoutingModule walkRouter,  Network network, 
			ParkingRouter parkingRouter, EventsManager events, ParkingSearchLogic parkingLogic, MobsimTimer timer, 
			VehicleTeleportationLogic teleportationLogic, ParkingSearchConfigGroup configGroup) {
		planElemIter = plan.getPlanElements().iterator();
		this.plan = plan;
		this.parkingManager = parkingManager;
		this.walkRouter = walkRouter;
		this.network = network;
		this.parkingRouter = parkingRouter;
		this.timer = timer;
		this.events = events;
		this.parkingLogic = parkingLogic;
		this.teleportationLogic = teleportationLogic;
		this.configGroup = configGroup;
		
		
	}

	@Override
	public DynActivity computeInitialActivity(DynAgent adapterAgent) {
		this.agent = adapterAgent;
		this.lastParkActionState = LastParkActionState.ACTIVITY;
		this.currentPlanElement = planElemIter.next();
		Activity act = (Activity) currentPlanElement;
		//TODO: assume something different regarding initial parking location

		return new IdleDynActivity(act.getType(), act.getEndTime());
	}

	@Override
	public DynAgent getDynAgent() {
		return agent;
	}

	@Override
	public DynAction computeNextAction(DynAction oldAction, double now) {
		// we have the following cases of ending dynacts:
		// non-car trip arrival: start Activity
		// car-trip arrival: add park-car activity 
		// park-car activity: get next PlanElement & add walk leg to activity location
		// walk-leg to act: start next PlanElement Activity
		// ordinary activity: get next Leg, if car: go to car, otherwise add ordinary leg by other mode
		// walk-leg to car: add unpark activity
		// unpark activity: find the way to the next route & start leg
		switch (lastParkActionState){
		case ACTIVITY:
			return nextStateAfterActivity(oldAction, now);
				
		case CARTRIP:
			return nextStateAfterCarTrip(oldAction,now);
			
		case NONCARTRIP:
			return nextStateAfterNonCarTrip(oldAction,now);
			
		case PARKACTIVITY:
			return nextStateAfterParkActivity(oldAction,now);
		
		case UNPARKACTIVITY:
			return nextStateAfterUnParkActivity(oldAction,now);
			
		case WALKFROMPARK:
			return nextStateAfterWalkFromPark(oldAction,now);
			
		case WALKTOPARK:
			return nextStateAfterWalkToPark(oldAction,now);
		
		}
		throw new RuntimeException("unreachable code");

	}

	protected DynAction nextStateAfterUnParkActivity(DynAction oldAction, double now) {
		// we have unparked, now we need to get going by car again.
		
		Leg currentPlannedLeg = (Leg) currentPlanElement;
		Route plannedRoute = currentPlannedLeg.getRoute();
		NetworkRoute actualRoute = this.parkingRouter.getRouteFromParkingToDestination(plannedRoute.getEndLinkId(), now, agent.getCurrentLinkId());
		if ((this.parkingManager.unParkVehicleHere(currentlyAssignedVehicleId, agent.getCurrentLinkId(), now))||(isinitialLocation)){
			this.lastParkActionState = LastParkActionState.CARTRIP;
			isinitialLocation = false;
			Leg currentLeg = (Leg) this.currentPlanElement;
			//this could be Car, Carsharing, Motorcylce, or whatever else mode we have, so we want our leg to reflect this.
			return new ParkingDynLeg(currentLeg.getMode(), actualRoute, parkingLogic, parkingManager, currentlyAssignedVehicleId, timer, events);
		
		}
		else throw new RuntimeException("parking location mismatch");
		
	}

	protected DynAction nextStateAfterWalkToPark(DynAction oldAction, double now) {
		//walk2park is complete, we can unpark.
		this.lastParkActionState = LastParkActionState.UNPARKACTIVITY;
		return new IdleDynActivity(this.stageInteractionType, now + configGroup.getUnparkduration());
	}

	protected DynAction nextStateAfterWalkFromPark(DynAction oldAction, double now) {
		//walkleg complete, time to get the next activity from the plan Elements and start it, this is basically the same as arriving on any other mode
		return nextStateAfterNonCarTrip(oldAction, now);
	}

	protected DynAction nextStateAfterParkActivity(DynAction oldAction, double now) {
		// add a walk leg after parking
		Leg currentPlannedLeg = (Leg) currentPlanElement;
		Facility fromFacility = new LinkWrapperFacility (network.getLinks().get(agent.getCurrentLinkId()));
		Facility toFacility = new LinkWrapperFacility (network.getLinks().get(currentPlannedLeg.getRoute().getEndLinkId()));
		List<? extends PlanElement> walkTrip = walkRouter.calcRoute(fromFacility, toFacility, now, plan.getPerson());
		if (walkTrip.size() != 1 || ! (walkTrip.get(0) instanceof Leg)) {
			String message = "walkRouter returned something else than a single Leg, e.g. it routes walk on the network with non_network_walk to access the network. Not implemented in parking yet!";
			log.error(message);
			throw new RuntimeException(message);
		}
		Leg walkLeg = (Leg) walkTrip.get(0);
		this.lastParkActionState = LastParkActionState.WALKFROMPARK;
		this.stageInteractionType = null;
		return new StaticPassengerDynLeg(walkLeg.getRoute(), walkLeg.getMode());
	}

	protected DynAction nextStateAfterNonCarTrip(DynAction oldAction, double now) {
		// switch back to activity
		this.currentPlanElement = planElemIter.next();
		Activity nextPlannedActivity = (Activity) this.currentPlanElement;
		this.lastParkActionState = LastParkActionState.ACTIVITY;
		final double endTime;
        if (nextPlannedActivity.isEndTimeUndefined()) {
            if (Time.isUndefinedTime(nextPlannedActivity.getMaximumDuration())) {
                endTime = Double.POSITIVE_INFINITY;
                //last activity of a day
            } else {
                endTime = now + nextPlannedActivity.getMaximumDuration();
            }
		} else {
        	endTime = nextPlannedActivity.getEndTime();
		}
		return new IdleDynActivity(nextPlannedActivity.getType(), endTime);
		
	}

	protected DynAction nextStateAfterCarTrip(DynAction oldAction, double now) {
		// car trip is complete, we have found a parking space (not part of the logic), block it and start to park
		if (this.parkingManager.parkVehicleHere(Id.create(this.agent.getId(), Vehicle.class), agent.getCurrentLinkId(), now)){
		this.lastParkActionState = LastParkActionState.PARKACTIVITY;
		this.currentlyAssignedVehicleId = null;
		this.parkingLogic.reset();
			return new IdleDynActivity(this.stageInteractionType, now + configGroup.getParkduration());
		}
		else throw new RuntimeException ("No parking possible");
	}

	protected DynAction nextStateAfterActivity(DynAction oldAction, double now) {
		// we could either depart by car or not next
		if (planElemIter.hasNext()){
		this.currentPlanElement = planElemIter.next();
		Leg currentLeg = (Leg) currentPlanElement;
		if (currentLeg.getMode().equals(TransportMode.car)){
			Id<Vehicle> vehicleId = Id.create(this.agent.getId(), Vehicle.class);
			Id<Link> parkLink = this.parkingManager.getVehicleParkingLocation(vehicleId);
			
			if (parkLink == null){
				//this is the first activity of a day and our parking manager does not provide informations about initial stages. We suppose the car is parked where we are
				parkLink = agent.getCurrentLinkId();
			}

    		Facility fromFacility = new LinkWrapperFacility (network.getLinks().get(agent.getCurrentLinkId()));
            Id<Link> teleportedParkLink = this.teleportationLogic.getVehicleLocation(agent.getCurrentLinkId(), vehicleId, parkLink, now, currentLeg.getMode());
    		Facility toFacility = new LinkWrapperFacility (network.getLinks().get(teleportedParkLink));
    		List<? extends PlanElement> walkTrip = walkRouter.calcRoute(fromFacility, toFacility, now, plan.getPerson());
    		if (walkTrip.size() != 1 || ! (walkTrip.get(0) instanceof Leg)) {
    			String message = "walkRouter returned something else than a single Leg, e.g. it routes walk on the network with non_network_walk to access the network. Not implemented in parking yet!";
    			log.error(message);
    			throw new RuntimeException(message);
    		}
    		Leg walkLeg = (Leg) walkTrip.get(0);
			this.lastParkActionState = LastParkActionState.WALKTOPARK;
			this.currentlyAssignedVehicleId = vehicleId;
			this.stageInteractionType = ParkingUtils.PARKACTIVITYTYPE;
			return new StaticPassengerDynLeg(walkLeg.getRoute(), walkLeg.getMode());
		}
		else if (currentLeg.getMode().equals(TransportMode.pt)) {
			if (currentLeg.getRoute() instanceof ExperimentalTransitRoute){
				throw new IllegalStateException ("not yet implemented");
			}
			else {
				this.lastParkActionState = LastParkActionState.NONCARTRIP;
				return new StaticPassengerDynLeg(currentLeg.getRoute(), currentLeg.getMode());
			}
		//teleport or pt route	
		} 
		else {
		//teleport	
			this.lastParkActionState = LastParkActionState.NONCARTRIP;
			return new StaticPassengerDynLeg(currentLeg.getRoute(), currentLeg.getMode());
		}
		
	}else throw new RuntimeException("no more leg to follow but activity is ending\nLastPlanElement: "+currentPlanElement.toString()+"\n Agent "+this.agent.getId()+"\nTime: "+Time.writeTime(now));
	}

}
