/**
 * 
 */
package org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.parking.parkingsearch.DynAgent.DistanceMemoryDynLeg;
import org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic.ParkingAgentLogic;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * @author Work
 *
 */
public class MemoryBasedParkingAgentLogic extends ParkingAgentLogic {

	/**
	 * @param plan
	 * @param parkingManager
	 * @param walkLegFactory
	 * @param parkingRouter
	 * @param events
	 * @param parkingLogic
	 * @param timer
	 * @param teleportationLogic
	 */
	public MemoryBasedParkingAgentLogic(Plan plan, ParkingSearchManager parkingManager, WalkLegFactory walkLegFactory,
			ParkingRouter parkingRouter, EventsManager events, ParkingSearchLogic parkingLogic, MobsimTimer timer,
			VehicleTeleportationLogic teleportationLogic, ParkingSearchConfigGroup configGroup) {
		super(plan, parkingManager, walkLegFactory, parkingRouter, events, parkingLogic, timer, teleportationLogic, configGroup);
		// TODO Auto-generated constructor stub
	}

	
	@Override
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
			return new DistanceMemoryDynLeg(currentLeg.getMode(), actualRoute, parkingLogic, parkingManager, currentlyAssignedVehicleId, timer, events);
		}
		else throw new RuntimeException("parking location mismatch");
		
	}
}
