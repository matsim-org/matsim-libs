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

package playground.jbischoff.parking.DynAgent.agentLogic;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.contrib.dynagent.StaticDynActivity;
import org.matsim.contrib.dynagent.StaticPassengerDynLeg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.vehicles.Vehicle;

import playground.jbischoff.parking.ParkingUtils;
import playground.jbischoff.parking.DynAgent.ParkingDynLeg;
import playground.jbischoff.parking.choice.ParkingChoiceLogic;
import playground.jbischoff.parking.manager.ParkingManager;
import playground.jbischoff.parking.manager.WalkLegFactory;
import playground.jbischoff.parking.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import playground.jbischoff.parking.routing.ParkingRouter;

/**
 * @author jbischoff
 *
 */
public class ParkingAgentLogic implements DynAgentLogic {

	enum LastParkActionState  {
			
			// we have the following cases of ending dynacts:
			NONCARTRIP,	// non-car trip arrival: start Activity
			CARTRIP, // car-trip arrival: add park-car activity 
			PARKACTIVITY, // park-car activity: get next PlanElement & add walk leg to activity location
			WALKFROMPARK ,// walk-leg to act: start next PlanElement Activity
			ACTIVITY, // ordinary activity: get next Leg, if car: go to car, otherwise add ordinary leg by other mode
			WALKTOPARK, // walk-leg to car: add unpark activity
			UNPARKACTIVITY // unpark activity: find the way to the next route & start leg
	}
	private LastParkActionState lastParkActionState;
	private DynAgent agent;
	private Iterator<PlanElement> planElemIter;
	private PlanElement currentPlanElement;
	private ParkingManager parkingManager;
	private WalkLegFactory walkLegFactory;
	private ParkingRouter parkingRouter;
	private MobsimTimer timer;
	private EventsManager events;
	private ParkingChoiceLogic parkingLogic;
	private VehicleTeleportationLogic teleportationLogic;
	private boolean isinitialLocation = true;
	/**
	 * @param plan
	 *            (always starts with Activity)
	 */
	public ParkingAgentLogic(Plan plan, ParkingManager parkingManager, WalkLegFactory walkLegFactory, ParkingRouter parkingRouter, EventsManager events, ParkingChoiceLogic parkingLogic, MobsimTimer timer, VehicleTeleportationLogic teleportationLogic) {
		planElemIter = plan.getPlanElements().iterator();
		this.parkingManager = parkingManager;
		this.walkLegFactory = walkLegFactory;
		this.parkingRouter = parkingRouter;
		this.timer = timer;
		this.events = events;
		this.parkingLogic = parkingLogic;
		this.teleportationLogic = teleportationLogic;
		
		
	}

	@Override
	public DynActivity computeInitialActivity(DynAgent adapterAgent) {
		this.agent = adapterAgent;
		this.lastParkActionState = LastParkActionState.ACTIVITY;
		this.currentPlanElement = planElemIter.next();
		Activity act = (Activity) currentPlanElement;
		//TODO: assume something different regarding initial parking location
		
		return new StaticDynActivity(act.getType(), act.getEndTime());
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

	private DynAction nextStateAfterUnParkActivity(DynAction oldAction, double now) {
		// we have unparked, now we need to get going by car again.
		
		Leg currentPlannedLeg = (Leg) currentPlanElement;
		NetworkRoute plannedRoute = (NetworkRoute) currentPlannedLeg.getRoute();
		NetworkRoute actualRoute = this.parkingRouter.getRouteFromParkingToDestination(plannedRoute, now, agent.getCurrentLinkId());
		Id<Vehicle> vehicleId = Id.create(this.agent.getId(), Vehicle.class);
		
		if ((this.parkingManager.unParkVehicleHere(vehicleId, agent.getCurrentLinkId(), now))||(isinitialLocation)){
			this.lastParkActionState = LastParkActionState.CARTRIP;
			isinitialLocation = false;
			return new ParkingDynLeg(TransportMode.car, actualRoute, parkingLogic, parkingManager, vehicleId, timer, events);
		
		}
		else throw new RuntimeException("parking location mismatch");
		
	}

	private DynAction nextStateAfterWalkToPark(DynAction oldAction, double now) {
		//walk2park is complete, we can unpark.
		this.lastParkActionState = LastParkActionState.UNPARKACTIVITY;
		return new StaticDynActivity(ParkingUtils.UNPARKACTIVITYTYPE, now + ParkingUtils.UNPARKDURATION);
	}

	private DynAction nextStateAfterWalkFromPark(DynAction oldAction, double now) {
		//walkleg complete, time to get the next activity from the plan Elements and start it, this is basically the same as arriving on any other mode
		return nextStateAfterNonCarTrip(oldAction, now);
	}

	private DynAction nextStateAfterParkActivity(DynAction oldAction, double now) {
		// add a walk leg after parking
		Leg currentPlannedLeg = (Leg) currentPlanElement;
		Id<Link> walkDestination = currentPlannedLeg.getRoute().getEndLinkId();
		Leg walkLeg = walkLegFactory.createWalkLeg(agent.getCurrentLinkId(), walkDestination, now, TransportMode.egress_walk);
		this.lastParkActionState = LastParkActionState.WALKFROMPARK;
		return new StaticPassengerDynLeg(walkLeg.getRoute(), walkLeg.getMode());
	}

	private DynAction nextStateAfterNonCarTrip(DynAction oldAction, double now) {
		// switch back to activity
		this.currentPlanElement = planElemIter.next();
		Activity nextPlannedActivity = (Activity) this.currentPlanElement;
		this.lastParkActionState = LastParkActionState.ACTIVITY;
		double endTime =nextPlannedActivity.getEndTime() ; 
		if (endTime == Time.UNDEFINED_TIME){
			endTime = Double.POSITIVE_INFINITY;
		}
			 
		return new StaticDynActivity(nextPlannedActivity.getType(),endTime);
		
	}

	private DynAction nextStateAfterCarTrip(DynAction oldAction, double now) {
		// car trip is complete, we have found a parking space (not part of the logic), block it and start to park
		if (this.parkingManager.parkVehicleHere(Id.create(this.agent.getId(), Vehicle.class), agent.getCurrentLinkId(), now)){
		this.lastParkActionState = LastParkActionState.PARKACTIVITY;
		return new StaticDynActivity(ParkingUtils.PARKACTIVITYTYPE,now + ParkingUtils.PARKDURATION);}
		else throw new RuntimeException ("No parking possible");
	}

	private DynAction nextStateAfterActivity(DynAction oldAction, double now) {
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
			
			Id<Link> telePortedParkLink = this.teleportationLogic.getVehicleLocation(agent.getCurrentLinkId(), vehicleId, parkLink, now);
			Leg walkleg = walkLegFactory.createWalkLeg(agent.getCurrentLinkId(), telePortedParkLink, now, TransportMode.access_walk);
			this.lastParkActionState = LastParkActionState.WALKTOPARK;
			return new StaticPassengerDynLeg(walkleg.getRoute(), walkleg.getMode());
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
