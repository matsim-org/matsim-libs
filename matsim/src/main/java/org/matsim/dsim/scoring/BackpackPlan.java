package org.matsim.dsim.scoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import static org.matsim.core.scoring.EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME;
import static org.matsim.core.scoring.EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME;


public class BackpackPlan {

	private static final Logger log = LogManager.getLogger(BackpackPlan.class);
	private final Plan experiencedPlan = PopulationUtils.createPlan();
//	private final Network network;
//	private final TransitSchedule transitSchedule;

	private Activity currentActivity;
	private PendingLeg currentLeg;
	private PendingVehicleLeg currentVehicleLeg;
	private PendingPtLeg currentPtLeg;

	Plan experiencedPlan() {
		return experiencedPlan;
	}

	public BackpackPlan() {
//		this.network = network;
//		this.transitSchedule = transitSchedule;
	}

	void startPtPart(Id<TransitLine> line, Id<TransitRoute> route) {
		if (currentPtLeg == null)
			currentPtLeg = new PendingPtLeg();
		currentPtLeg.startPart(line, route);
	}

	void handleEvent(ActivityStartEvent e) {
		if (currentActivity != null) throw new IllegalStateException("Agent already performs an activity.");

		currentActivity = createActivity(
			e.getActType(), e.getLinkId(), e.getFacilityId(), e.getCoord()
		);
		currentActivity.setStartTime(e.getTime());
	}

	void handleEvent(ActivityEndEvent e) {
		// overnight activities don't have an activity start event. Therefore, create the activity here.
		if (currentActivity == null) {
			currentActivity = createActivity(
				e.getActType(), e.getLinkId(), e.getFacilityId(), e.getCoord()
			);
		}
		currentActivity.setEndTime(e.getTime());
		experiencedPlan.addActivity(currentActivity);
		currentActivity = null;
	}

	void handleEvent(PersonDepartureEvent e) {
		if (currentLeg != null) throw new IllegalStateException("Agent already performs a leg.");

		currentLeg = new PendingLeg(e.getLegMode(), e.getRoutingMode(), e.getTime());
		currentLeg.addLink(e.getLinkId());
	}

	/**
	 * This takes the arrival event as well as the network and the transit schedule. This has the advantage that the object does not store these
	 * objects as the backpack plan is passed around when agents switch from one to another partition.
	 */
	void handleEvent(PersonArrivalEvent e, Network network, TransitSchedule transitSchedule) {

		// TODO this lacks pt

		if (currentLeg == null) throw new IllegalStateException("Agent arrives but never started");

		currentLeg.arrivalTime(e.getTime());

		var route = finishRoute(e, network, transitSchedule);
		var leg = PopulationUtils.createLeg(currentLeg.legMode());
		leg.setDepartureTime(currentLeg.departureTime());
		leg.setRoutingMode(currentLeg.routingMode());
		leg.setTravelTime(currentLeg.travelTime());
		leg.setRoute(route);

		if (currentVehicleLeg != null) {
			leg.getAttributes().putAttribute(ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, currentVehicleLeg.enterVehicleTime());
			leg.getAttributes().putAttribute(VEHICLE_ID_ATTRIBUTE_NAME, currentVehicleLeg.vehicleId());
		}

		experiencedPlan.addLeg(leg);
		currentLeg = null;
		currentVehicleLeg = null;
		currentPtLeg = null;
	}

	private Route finishRoute(PersonArrivalEvent pae, Network network, TransitSchedule transitSchedule) {

		if (currentPtLeg != null) {

			// chained routes are implemented as a linked list of transit routes. We have collected a plain list
			// of those transit routes. Iterate through this list backwards, create pt routes and place them into
			// the previous one.
			DefaultTransitPassengerRoute ptRoute = null;

			while (!currentPtLeg.getPtParts().isEmpty()) {
				var part = currentPtLeg.getPtParts().removeLast();

				var start = transitSchedule.getFacilities().get(part.getStartFacility());
				var end = transitSchedule.getFacilities().get(part.getEndFacility());
				var transitLine = transitSchedule.getTransitLines().get(part.getLine());
				var transitRoute = transitLine.getRoutes().get(part.getRoute());
				ptRoute = new DefaultTransitPassengerRoute(start, transitLine, transitRoute, end, ptRoute);
			}
			assert ptRoute != null;

			ptRoute.setBoardingTime(currentVehicleLeg.enterVehicleTime());
			ptRoute.setTravelTime(currentLeg.travelTime());
			ptRoute.setDistance(RouteUtils.calcDistance(ptRoute, transitSchedule, network));
			return ptRoute;
		}
		// The original code handles a special case, where a person enters and leaves a vehicle, but the vehicle does not
		// enter and leave traffic. In that case the code generates a generic route instead of a network route.
		// the code below creates a network route in that case but with the same attributes regarding distance (0.0) and
		// travel time. Let's see whether this breaks things somewhere else.
		if (currentVehicleLeg != null) {
			var route = currentVehicleLeg.route();
			var travelledDistance = RouteUtils.calcDistance(
				route, currentVehicleLeg.relativePositionOnDepartureLink(), currentVehicleLeg.relativePositionOnArrivalLink(), network);
			route.setDistance(travelledDistance);
			route.setTravelTime(currentLeg.travelTime());
			route.setVehicleId(currentVehicleLeg.vehicleId());
			return route;
		} else {
			// this was a teleported leg with a generic route. Add the arrival link
			currentLeg.addLink(pae.getLinkId());
			var route = currentLeg.route();
			route.setDistance(currentLeg.travelDistance());
			route.setTravelTime(currentLeg.travelTime());
			return route;
		}
	}

	void handleEvent(TeleportationArrivalEvent e) {
		currentLeg.travelDistance(e.getDistance());
	}

	void handleEvent(PersonEntersVehicleEvent e) {
		if (currentVehicleLeg != null) throw new IllegalStateException("Agent already performs vehicle leg");
		currentVehicleLeg = new PendingVehicleLeg(e.getTime(), e.getVehicleId(), currentLeg);
	}

	void handleEvent(PersonLeavesVehicleEvent e) {
		log.info("break");
	}

	void handleEvent(VehicleArrivesAtFacilityEvent e) {
		if (currentPtLeg != null) {
			currentPtLeg.endFacility(e.getFacilityId());
			//throw new IllegalStateException("Vehicle arrives at facility events should only be passed if the person has started a pt leg");
		}
	}

	void handleEvent(VehicleDepartsAtFacilityEvent e) {
		if (currentPtLeg != null && !currentPtLeg.hasStartFacility()) {
			currentPtLeg.startFacility(e.getFacilityId());
		}
	}

	void handleEvent(VehicleEntersTrafficEvent e) {
		// the original code also sets route information. However, usually, the link a vehicle enters traffic on
		// is the same as the departure link. Therfore omit this step for now and only set the relative position on
		// departure link for distance calculations later.
		currentVehicleLeg.relativePositionOnDepartureLink(e.getRelativePositionOnLink());
	}

	void handleEvent(VehicleLeavesTrafficEvent e) {
		currentVehicleLeg.relativePositionOnArrivalLink(e.getRelativePositionOnLink());
	}

	void handleEvent(LinkEnterEvent e) {
		currentVehicleLeg.addLink(e.getLinkId());
	}

	void handleEvent(AgentWaitingForPtEvent e) {
		log.info("waiting for pt");
	}

	void finish() {
		// overnight activities don't have an activity end event
		if (currentActivity != null) experiencedPlan.addActivity(currentActivity);
	}

	private static Activity createActivity(String type, Id<Link> linkId, Id<ActivityFacility> facilityId, Coord coord) {
		var result = PopulationUtils.createActivityFromLinkId(type, linkId);
		result.setFacilityId(facilityId);
		result.setCoord(coord);
		return result;
	}
}
