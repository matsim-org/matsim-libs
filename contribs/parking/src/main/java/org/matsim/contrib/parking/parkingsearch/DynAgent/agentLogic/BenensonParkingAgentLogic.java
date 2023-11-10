/**
 *
 */
package org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.parking.parkingsearch.DynAgent.BenensonDynLeg;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.RoutingModule;

/**
 * @author Work
 *
 */
public class BenensonParkingAgentLogic extends ParkingAgentLogic {

	/**
	 * @param plan
	 * @param parkingManager
	 * @param walkRouter
	 * @param parkingRouter
	 * @param events
	 * @param parkingLogic
	 * @param timer
	 * @param teleportationLogic
	 */


	public BenensonParkingAgentLogic(Plan plan, ParkingSearchManager parkingManager, RoutingModule walkRouter, Network network,
			ParkingRouter parkingRouter, EventsManager events, ParkingSearchLogic parkingLogic, MobsimTimer timer,
			VehicleTeleportationLogic teleportationLogic, ParkingSearchConfigGroup configGroup) {
		super(plan, parkingManager, walkRouter, network, parkingRouter, events, parkingLogic, timer, teleportationLogic, configGroup);
	}


	@Override
	protected DynAction nextStateAfterUnParkActivity(DynAction oldAction, double now) {
		// we have unparked, now we need to get going by car again.

		Leg currentPlannedLeg = (Leg) currentPlanElement;
		Route plannedRoute = currentPlannedLeg.getRoute();
		NetworkRoute actualRoute = this.parkingRouter.getRouteFromParkingToDestination(plannedRoute.getEndLinkId(), now, agent.getCurrentLinkId());
		if ((this.parkingManager.unParkVehicleHere(currentlyAssignedVehicleId, agent.getCurrentLinkId(), now))||(isInitialLocation)){
			this.lastParkActionState = LastParkActionState.CARTRIP;
			isInitialLocation = false;
			Leg currentLeg = (Leg) this.currentPlanElement;
			//this could be Car, Carsharing, Motorcylce, or whatever else mode we have, so we want our leg to reflect this.
			return new BenensonDynLeg(currentLeg.getMode(), actualRoute, parkingLogic, parkingManager, currentlyAssignedVehicleId, timer, events);
		}
		else throw new RuntimeException("parking location mismatch");

	}
}
