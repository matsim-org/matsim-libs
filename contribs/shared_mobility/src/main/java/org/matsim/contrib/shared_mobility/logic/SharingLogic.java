package org.matsim.contrib.shared_mobility.logic;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.shared_mobility.routing.InteractionPoint;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;
import org.matsim.contrib.shared_mobility.service.VehicleInteractionPoint;
import org.matsim.contrib.shared_mobility.service.events.SharingReservingEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingDropoffEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingFailedDropoffEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingFailedPickupEvent;
import org.matsim.contrib.shared_mobility.service.events.SharingPickupEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Verify;

public class SharingLogic {
	private static final Logger LOG = LogManager.getLogger(SharingLogic.class);
	private final IdMap<Person, SharingVehicle> activeVehicles = new IdMap<>(Person.class);

	private final RoutingModule accessEgressRoutingModule;
	private final RoutingModule mainModeRoutingModule;

	private final Network network;
	private final PopulationFactory populationFactory;
	private final EventsManager eventsManager;
	private final TimeInterpretation timeInterpretation;

	private final SharingService service;

	public SharingLogic(SharingService service, RoutingModule accessEgressRoutingModule,
						RoutingModule mainModeRoutingModule, Scenario scenario, EventsManager eventsManager,
						TimeInterpretation timeInterpretation) {
		this.service = service;
		this.eventsManager = eventsManager;

		this.accessEgressRoutingModule = accessEgressRoutingModule;
		this.mainModeRoutingModule = mainModeRoutingModule;

		this.network = scenario.getNetwork();
		this.populationFactory = scenario.getPopulation().getFactory();
		this.timeInterpretation = timeInterpretation;
	}

	public boolean tryBookVehicle(double now, MobsimAgent agent) {
		double currentTime=now;
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		int bookingActivityIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		Verify.verify(plan.getPlanElements().get(bookingActivityIndex) instanceof Activity);
		Activity bookingActivity = (Activity) plan.getPlanElements().get(bookingActivityIndex);
		Verify.verify(bookingActivity.getType().equals(SharingUtils.BOOKING_ACTIVITY));

		// Find closest vehicle and hope it is at the current station / link
		Optional<VehicleInteractionPoint> closestVehicleInteraction = service.findClosestVehicle(agent);

		if (closestVehicleInteraction.isPresent()) {
			Id<Link> vehicleLinkId = closestVehicleInteraction.get().getLinkId();

			// We need to get to the closest vehicle after this activity ...

			// Maybe we want to throw event that the vehicle was found
			// eventsManager.processEvent(new SharingFailedPickupEvent(now, service.getId(),
			// agent.getId(), vehicleLinkId,
			// service.getStationId(vehicleLinkId)));

			// Remove everything until the dropoff activity
			int dropoffActivityIndex = findDropoffActivityIndex(bookingActivityIndex, plan);
			Activity dropoffActivity = (Activity) plan.getPlanElements().get(dropoffActivityIndex);

			// router ensures that pickup and dropoff locations are different
			// however we need to ensure this here as well. Otherwise we are
			// getting 0 seconds travel times, which is problematic
			// If the pickup location is the same as dropoff location we return false
			// and consider agent stuck

			if (dropoffActivity.getLinkId().equals(vehicleLinkId))
				return false;

			plan.getPlanElements().subList(bookingActivityIndex + 1, dropoffActivityIndex).clear();

			// Create new plan elements
			List<PlanElement> updatedElements = new LinkedList<>();

			// 1) Leg to pickup activity
			List<? extends PlanElement> accessElements = routeAccessEgressStage(bookingActivity.getLinkId(),
					vehicleLinkId, now, agent);
			updatedElements.addAll(accessElements);
			now = timeInterpretation.decideOnElementsEndTime(accessElements, now).seconds();

			// 2) Pickup activity
			Activity updatedPickupActivity = createPickupActivity(now, closestVehicleInteraction.get());
			updatedElements.add(updatedPickupActivity);
			now = timeInterpretation.decideOnElementEndTime(updatedPickupActivity, now).seconds();

			// 3) Leg to dropoff activity
			List<? extends PlanElement> mainElements = routeMainStage(vehicleLinkId, dropoffActivity.getLinkId(), now,
					agent);
			updatedElements.addAll(mainElements);

			// Insert new plan elements
			plan.getPlanElements().addAll(bookingActivityIndex + 1, updatedElements);
			setVehicle(plan,mainElements, closestVehicleInteraction.get().getVehicle());

			service.reserveVehicle(agent,closestVehicleInteraction.get().getVehicle());
			eventsManager.processEvent(new SharingReservingEvent(currentTime, service.getId(),
					agent.getId(), bookingActivity.getLinkId(), closestVehicleInteraction.get().getVehicle().getId(),
					closestVehicleInteraction.get().getStationId(), dropoffActivity.getLinkId()));

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Agent tries to pick up a vehicle.
	 *
	 * If it returns false, agent needs to abort!
	 *
	 * @param agent
	 */
	public boolean tryPickupVehicle(double now, MobsimAgent agent) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		int pickupActivityIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		Verify.verify(plan.getPlanElements().get(pickupActivityIndex) instanceof Activity);
		Activity pickupActivity = (Activity) plan.getPlanElements().get(pickupActivityIndex);
		Verify.verify(pickupActivity.getType().equals(SharingUtils.PICKUP_ACTIVITY));

		Optional<SharingVehicle> reservedVehicle = service.getReservedVehicle(agent);

		Optional<VehicleInteractionPoint> selectedVehicleInteraction;

		if(reservedVehicle.isPresent())
		{
			selectedVehicleInteraction = Optional.of(VehicleInteractionPoint.of(reservedVehicle.get()));
		} else {
			// Find the closest vehicle and hope it is at the current station / link
			selectedVehicleInteraction = service.findClosestVehicle(agent);
		}

		if (selectedVehicleInteraction.isPresent()) {
			Id<Link> vehicleLinkId = selectedVehicleInteraction.get().getLinkId();

			if (vehicleLinkId.equals(pickupActivity.getLinkId())) {
				// We are at the current location. We can pick up the vehicle.
				service.pickupVehicle(selectedVehicleInteraction.get().getVehicle(), agent);
				activeVehicles.put(agent.getId(), selectedVehicleInteraction.get().getVehicle());

				eventsManager.processEvent(new SharingPickupEvent(now, service.getId(), agent.getId(), vehicleLinkId,
						selectedVehicleInteraction.get().getVehicle().getId(),
						selectedVehicleInteraction.get().getStationId()));
			} else {
				// The closest vehicle is not here, we need to get there after this activity ...

				eventsManager.processEvent(new SharingFailedPickupEvent(now, service.getId(), agent.getId(),
						vehicleLinkId, selectedVehicleInteraction.get().getStationId()));

				// router ensures that pickup and dropoff locations are different
				// however we need to ensure this here as well. Otherwise we are
				// getting 0 seconds travel times, which is problematic
				// If the pickup location is the same as dropoff location we return false
				// and consider agent stuck

				// Remove everything until the dropoff activity
				int dropoffActivityIndex = findDropoffActivityIndex(pickupActivityIndex, plan);
				Activity dropoffActivity = (Activity) plan.getPlanElements().get(dropoffActivityIndex);

				if (dropoffActivity.getLinkId().equals(vehicleLinkId)) {
					service.releaseReservation(agent); // Release if pickup fails
					return false;
				}

				plan.getPlanElements().subList(pickupActivityIndex + 1, dropoffActivityIndex).clear();

				// Create new plan elements
				List<PlanElement> updatedElements = new LinkedList<>();

				// 1) Leg to pickup activity
				List<? extends PlanElement> accessElements = routeAccessEgressStage(pickupActivity.getLinkId(),
						vehicleLinkId, now, agent);
				updatedElements.addAll(accessElements);
				now = timeInterpretation.decideOnElementsEndTime(accessElements, now).seconds();

				// 2) Pickup activity
				Activity updatedPickupActivity = createPickupActivity(now, selectedVehicleInteraction.get());
				updatedElements.add(updatedPickupActivity);
				now = timeInterpretation.decideOnElementEndTime(updatedPickupActivity, now).seconds();

				// 3) Leg to dropoff activity
				List<? extends PlanElement> mainElements = routeMainStage(vehicleLinkId, dropoffActivity.getLinkId(),
						now, agent);
				updatedElements.addAll(mainElements);
				setVehicle(plan, mainElements, selectedVehicleInteraction.get().getVehicle());

				// Insert new plan elements
				plan.getPlanElements().addAll(pickupActivityIndex + 1, updatedElements);
			}

			return true;
		} else {
			service.releaseReservation(agent); // Release if pickup fails
			return false;
		}
	}

	/**
	 * Set the planned vehicle into the NetworkRoute
	 * @param plan
	 * @param planElements
	 * @param vehicle
	 */
	private void setVehicle(Plan plan, List<? extends PlanElement> planElements, SharingVehicle vehicle)
	{
		Set<Id<Vehicle>> vehicleIds = new HashSet<>();

		for (PlanElement pe : planElements)
		{

			if(vehicleIds.size()>1)
			{
				throw new IllegalStateException("Tried to modify more than one vehicleId. Current code supports only non-vehicular access and egress modes");
			}

			if (pe instanceof Leg)
			{
				Leg currentLeg =  (Leg) pe;
				Route route =  currentLeg.getRoute();

				if(route instanceof NetworkRoute)
				{
					Id<Vehicle> assignedId = Id.createVehicleId(vehicle.getId().toString());
					NetworkRoute networkRoute = (NetworkRoute) currentLeg.getRoute();
					vehicleIds.add(networkRoute.getVehicleId());
					networkRoute.setVehicleId(assignedId);
				}

			}
		}




	}

	/**
	 * Agent tries to drop off a vehicle.
	 *
	 * If it returns false, agent needs to abort!
	 *
	 * @param agent
	 */
	public void tryDropoffVehicle(double now, MobsimAgent agent) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		int dropoffActivityIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		Verify.verify(plan.getPlanElements().get(dropoffActivityIndex) instanceof Activity);
		Activity dropoffActivity = (Activity) plan.getPlanElements().get(dropoffActivityIndex);
		Verify.verify(dropoffActivity.getType().equals(SharingUtils.DROPOFF_ACTIVITY));

		SharingVehicle vehicle = activeVehicles.get(agent.getId());
		Verify.verifyNotNull(vehicle);

		// Find the closest place to drop off the vehicle and hope we're already there ...
		InteractionPoint closestDropoffInteraction = service.findClosestDropoffLocation(vehicle, agent);

		if (closestDropoffInteraction.getLinkId().equals(dropoffActivity.getLinkId())) {
			// We're at the right spot. Drop the vehicle here.
			service.dropoffVehicle(vehicle, agent);
			activeVehicles.remove(agent.getId());

			eventsManager.processEvent(new SharingDropoffEvent(now, service.getId(), agent.getId(),
					closestDropoffInteraction.getLinkId(), vehicle.getId(), closestDropoffInteraction.getStationId()));
			service.releaseReservation(agent); // Release the reservation after successful drop off
		} else {
			// We cannot drop the vehicle here, so let's try the proposed place

			eventsManager.processEvent(new SharingFailedDropoffEvent(now, service.getId(), agent.getId(),
					closestDropoffInteraction.getLinkId(), vehicle.getId(), closestDropoffInteraction.getStationId()));

			// Remove everything until the end of the trip
			int destinationActivityIndex = findNextOrdinaryActivityIndex(dropoffActivityIndex, plan);
			Activity destinationActivity = (Activity) plan.getPlanElements().get(destinationActivityIndex);

			plan.getPlanElements().subList(dropoffActivityIndex + 1, destinationActivityIndex).clear();

			// Create new plan elements
			List<PlanElement> updatedElements = new LinkedList<>();

			// 1) Leg to new dropoff activity
			List<? extends PlanElement> mainElements = routeMainStage(dropoffActivity.getLinkId(),
					closestDropoffInteraction.getLinkId(), now, agent);
			updatedElements.addAll(mainElements);
			now = timeInterpretation.decideOnElementsEndTime(mainElements, now).seconds();

			setVehicle(plan,mainElements,vehicle);

			// 2) Dropoff activity
			Activity updatedPickupActivity = createDropoffActivity(now, closestDropoffInteraction);
			updatedElements.add(updatedPickupActivity);
			now = timeInterpretation.decideOnElementEndTime(updatedPickupActivity, now).seconds();

			// 3) Leg to destination
			List<? extends PlanElement> accessElements = routeAccessEgressStage(closestDropoffInteraction.getLinkId(),
					destinationActivity.getLinkId(), now, agent);
			updatedElements.addAll(accessElements);

			// Insert new plan elements
			plan.getPlanElements().addAll(dropoffActivityIndex + 1, updatedElements);
		}
	}

	private Activity createPickupActivity(double now, InteractionPoint interaction) {
		Activity activity = populationFactory.createActivityFromLinkId(SharingUtils.PICKUP_ACTIVITY,
				interaction.getLinkId());
		activity.setStartTime(now);
		activity.setMaximumDuration(SharingUtils.INTERACTION_DURATION);
		SharingUtils.setServiceId(activity, service.getId());

		if (interaction.isStation()) {
			SharingUtils.setStationId(activity, interaction.getStationId().get());
		}

		return activity;
	}

	private Activity createDropoffActivity(double now, InteractionPoint interaction) {
		Activity activity = populationFactory.createActivityFromLinkId(SharingUtils.DROPOFF_ACTIVITY,
				interaction.getLinkId());
		activity.setStartTime(now);
		activity.setMaximumDuration(SharingUtils.INTERACTION_DURATION);
		SharingUtils.setServiceId(activity, service.getId());

		if (interaction.isStation()) {
			SharingUtils.setStationId(activity, interaction.getStationId().get());
		}

		return activity;
	}

	private List<? extends PlanElement> routeAccessEgressStage(Id<Link> originId, Id<Link> destinationId,
															   double departureTime, MobsimAgent agent) {
		Facility originFacility = new LinkWrapperFacility(network.getLinks().get(originId));
		Facility destinationFacility = new LinkWrapperFacility(network.getLinks().get(destinationId));

		return accessEgressRoutingModule.calcRoute(DefaultRoutingRequest.of(originFacility, destinationFacility,
				departureTime, ((PlanAgent) agent).getCurrentPlan().getPerson(), getCurrentTripAttributes(agent)));
	}

	private List<? extends PlanElement> routeMainStage(Id<Link> originId, Id<Link> destinationId, double departureTime,
													   MobsimAgent agent) {
		Facility originFacility = new LinkWrapperFacility(network.getLinks().get(originId));
		Facility destinationFacility = new LinkWrapperFacility(network.getLinks().get(destinationId));

		return mainModeRoutingModule.calcRoute(DefaultRoutingRequest.of(originFacility, destinationFacility,
				departureTime, ((PlanAgent) agent).getCurrentPlan().getPerson(), getCurrentTripAttributes(agent)));
	}

	private int findNextOrdinaryActivityIndex(int currentIndex, Plan plan) {
		List<PlanElement> elements = plan.getPlanElements();

		for (int i = currentIndex + 1; i < elements.size(); i++) {
			PlanElement element = elements.get(i);

			if (element instanceof Activity) {
				Activity activity = (Activity) element;

				if (!TripStructureUtils.isStageActivityType(activity.getType())) {
					return i;
				}
			}
		}

		throw new IllegalStateException("No ordinary activity found");
	}

	private int findDropoffActivityIndex(int pickupActivityIndex, Plan plan) {
		List<PlanElement> elements = plan.getPlanElements();

		for (int i = pickupActivityIndex + 1; i < elements.size(); i++) {
			PlanElement element = elements.get(i);

			if (element instanceof Activity) {
				Activity activity = (Activity) element;

				if (activity.getType().equals(SharingUtils.DROPOFF_ACTIVITY)) {
					return i;
				}
			}
		}

		throw new IllegalStateException("No dropoff activity found");
	}

	private Attributes getCurrentTripAttributes(MobsimAgent agent) {
		int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		Plan plan = ((PlanAgent) agent).getCurrentPlan();

		while (currentIndex >= 0) {
			PlanElement element = plan.getPlanElements().get(currentIndex);

			if (element instanceof Activity) {
				Activity activity = (Activity) element;

				if (!TripStructureUtils.isStageActivityType(activity.getType())) {
					return activity.getAttributes();
				}
			}

			currentIndex--;
		}

		throw new IllegalStateException("Cannot find origin activity for current trip");
	}
}