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

/**
 * 
 */
package playground.jbischoff.ffcs.DynAgent.agentLogic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynLeg;
import org.matsim.contrib.dynagent.StaticDynActivity;
import org.matsim.contrib.dynagent.StaticPassengerDynLeg;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.ParkingAgentLogic;
import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.ParkingAgentLogic.LastParkActionState;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.vehicles.Vehicle;

import playground.jbischoff.ffcs.FFCSConfigGroup;
import playground.jbischoff.ffcs.FFCSUtils;
import playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CarsharingParkingAgentLogic extends ParkingAgentLogic {

	private FreefloatingCarsharingManager ffcmanager;
	FFCSConfigGroup ffcsconfig;
	/**
	 * @param plan
	 * @param parkingManager
	 * @param walkLegFactory
	 * @param parkingRouter
	 * @param events
	 * @param parkingLogic
	 * @param timer
	 * @param teleportationLogic
	 * @param FreefloatingCarsharingManager
	 * @param FFCSConfigGroup
	 */
	public CarsharingParkingAgentLogic(Plan plan, ParkingSearchManager parkingManager, WalkLegFactory walkLegFactory,
			ParkingRouter parkingRouter, EventsManager events, ParkingSearchLogic parkingLogic, MobsimTimer timer,
			VehicleTeleportationLogic teleportationLogic, FreefloatingCarsharingManager ffcmanager, FFCSConfigGroup ffcsconfig) {
		super(plan, parkingManager, walkLegFactory, parkingRouter, events, parkingLogic, timer, teleportationLogic);
		this.ffcmanager = ffcmanager;
		this.ffcsconfig = ffcsconfig;
	}
	
	@Override
	protected DynAction nextStateAfterCarTrip(DynAction oldAction, double now) {
		
		// car trip is complete, we have found a parking space (not part of the logic), block it and start to park
		if (this.parkingManager.parkVehicleHere(this.currentlyAssignedVehicleId, agent.getCurrentLinkId(), now)){
		DynLeg oldLeg = (DynLeg) oldAction;
		if (oldLeg.getMode().equals(FFCSUtils.FREEFLOATINGMODE)){
			this.ffcmanager.endRental(agent.getCurrentLinkId(), agent.getId(), this.currentlyAssignedVehicleId, now);
			}
		this.lastParkActionState = LastParkActionState.PARKACTIVITY;
		this.currentlyAssignedVehicleId = null;
		this.parkingLogic.reset();
		return new StaticDynActivity(this.stageInteractionType,now + ParkingUtils.PARKDURATION);}
		else throw new RuntimeException ("No parking possible");
	}
	
	@Override
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
			
			Id<Link> telePortedParkLink = this.teleportationLogic.getVehicleLocation(agent.getCurrentLinkId(), vehicleId, parkLink, now);
			Leg walkleg = walkLegFactory.createWalkLeg(agent.getCurrentLinkId(), telePortedParkLink, now, TransportMode.access_walk);
			this.lastParkActionState = LastParkActionState.WALKTOPARK;
			this.currentlyAssignedVehicleId = vehicleId;
			this.stageInteractionType = ParkingUtils.PARKACTIVITYTYPE;
			return new StaticPassengerDynLeg(walkleg.getRoute(), walkleg.getMode());
		}
		else if (currentLeg.getMode().equals(FFCSUtils.FREEFLOATINGMODE)){
			Tuple<Id<Link>,Id<Vehicle>> vehicleLocationLink = ffcmanager.findAndReserveFreefloatingVehicleForLeg(currentLeg, agent.getId(), now);
			if (vehicleLocationLink == null){
				currentLeg.setMode(TransportMode.pt);
				this.lastParkActionState = LastParkActionState.NONCARTRIP;
				if (ffcsconfig.getPunishmentForModeSwitch()!=0.0){
					events.processEvent(new PersonMoneyEvent(now, this.agent.getId(), ffcsconfig.getPunishmentForModeSwitch()));
				}
				return new StaticPassengerDynLeg(currentLeg.getRoute(), currentLeg.getMode());
				
			}

			Leg walkleg = walkLegFactory.createWalkLeg(agent.getCurrentLinkId(), vehicleLocationLink.getFirst(), now, TransportMode.access_walk);
			this.currentlyAssignedVehicleId = vehicleLocationLink.getSecond();
			this.lastParkActionState = LastParkActionState.WALKTOPARK;
			this.stageInteractionType = FFCSUtils.FREEFLOATINGPARKACTIVITYTYPE;
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
