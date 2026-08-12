package org.matsim.contrib.pseudosimulation.mobsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

final class PSimPlanExecutor implements PSimExecutionCoordinator.Worker {

    private static final double MIN_ACT_DURATION = 1.0;
    private static final double MIN_LEG_DURATION = 0.0;

    private final double endTime;
    private final TravelTime carLinkTravelTimes;
    private final TransitEmulator transitEmulator;
    private final Set<String> transitModes;
    private final Runnable completion;
    private Collection<Plan> plans;
    private EventsManager eventManager;
    private Network network;

    PSimPlanExecutor(double endTime, TravelTime carLinkTravelTimes, TransitEmulator transitEmulator,
            Set<String> transitModes, Runnable completion) {
        this.endTime = endTime;
        this.carLinkTravelTimes = carLinkTravelTimes;
        this.transitEmulator = transitEmulator;
        this.transitModes = transitModes;
        this.completion = completion;
    }

    @Override
    public void initialize(Collection<Plan> plans, Network network, EventsManager eventManager) {
        this.plans = plans;
        this.network = network;
        this.eventManager = eventManager;
    }

    @Override
    public void run() {
        for (Plan plan : plans) {
            execute(plan);
        }
        // Intentionally not in a finally block: legacy worker failures leave PSim waiting.
        completion.run();
    }

    private void execute(Plan plan) {
        Id<Person> personId = plan.getPerson().getId();
        Id<Vehicle> personVehicleId = Id.createVehicleId(personId.toString());
        List<PlanElement> elements = plan.getPlanElements();
        List<Event> eventQueue = new ArrayList<>(Math.max(10, elements.size() * 3));
        double previousEndTime = 0;

        for (int index = 0; index < elements.size(); index += 2) {
            Activity activity = (Activity) elements.get(index);
            double activityEndTime = Math.max(previousEndTime + MIN_ACT_DURATION, activity.getEndTime().orElse(0));
            if (index > 0) {
                Leg previousLeg = (Leg) elements.get(index - 1);
                Double travelTime = createLegEvents(previousLeg, previousEndTime, personId, personVehicleId, eventQueue);
                if (travelTime == null) {
                    continue;
                }

                travelTime = Math.max(MIN_LEG_DURATION, travelTime);
                double arrivalTime = travelTime + previousEndTime;
                activityEndTime = Math.max(arrivalTime + MIN_ACT_DURATION, activityEndTime);
                eventQueue.add(new PersonArrivalEvent(arrivalTime, personId, activity.getLinkId(), previousLeg.getMode()));
                eventQueue.add(new ActivityStartEvent(arrivalTime, personId, activity.getLinkId(),
                        activity.getFacilityId(), activity.getType(), null));
            }

            if (index < elements.size() - 1) {
                Leg nextLeg = (Leg) elements.get(index + 1);
                eventQueue.add(new ActivityEndEvent(activityEndTime, personId, activity.getLinkId(),
                        activity.getFacilityId(), activity.getType(), null));
                eventQueue.add(new PersonDepartureEvent(activityEndTime, personId, activity.getLinkId(),
                        nextLeg.getMode(), TripStructureUtils.getRoutingMode(nextLeg)));
            }
            previousEndTime = activityEndTime;
        }

        publishEvents(eventQueue, personId);
    }

    private Double createLegEvents(Leg leg, double departureTime, Id<Person> personId, Id<Vehicle> vehicleId,
            List<Event> eventQueue) {
        if (leg.getMode().equals(TransportMode.car)) {
            try {
                eventQueue.add(new PersonEntersVehicleEvent(departureTime, personId, vehicleId));
                eventQueue.add(new VehicleEntersTrafficEvent(departureTime, personId, leg.getRoute().getStartLinkId(),
                        vehicleId, TransportMode.car, 1.0));
                double travelTime = calculateRouteTravelTime((NetworkRoute) leg.getRoute(), departureTime,
                        eventQueue, vehicleId);
                eventQueue.add(new VehicleLeavesTrafficEvent(departureTime + travelTime, personId,
                        leg.getRoute().getEndLinkId(), vehicleId, TransportMode.car, 1.0));
                eventQueue.add(new PersonLeavesVehicleEvent(departureTime + travelTime, personId, vehicleId));
                return travelTime;
            } catch (NullPointerException exception) {
                LogManager.getLogger(getClass()).error("No route for car leg. Continuing with next leg");
                return null;
            }
        }
        if (transitModes.contains(leg.getMode())) {
            TransitEmulator.Trip trip = transitEmulator.findTrip(leg, departureTime);
            if (trip == null) {
                return 0.0;
            }
            Id<Vehicle> transitVehicleId = trip.vehicleId();
            if (transitVehicleId == null) {
                transitVehicleId = Id.create("dummy", Vehicle.class);
            }
            eventQueue.add(new PersonEntersVehicleEvent(trip.accessTime_s(), personId, transitVehicleId));
            eventQueue.add(new PersonLeavesVehicleEvent(trip.egressTime_s(), personId, transitVehicleId));
            return trip.egressTime_s() - departureTime;
        }

        Route route = leg.getRoute();
        if (route == null) {
            LogManager.getLogger(getClass()).error("No route for this leg. Continuing with next leg");
            return null;
        }
        double travelTime = route.getTravelTime().orElse(0);
        eventQueue.add(new TeleportationArrivalEvent(departureTime + travelTime, personId, route.getDistance(),
                leg.getMode()));
        return travelTime;
    }

    private void publishEvents(List<Event> eventQueue, Id<Person> personId) {
        for (Event event : eventQueue) {
            if (event.getTime() > endTime) {
                eventManager.processEvent(new PersonStuckEvent(endTime, personId, null, null));
                break;
            }
            eventManager.processEvent(event);
        }
    }

    private double calculateRouteTravelTime(NetworkRoute route, double startTime, List<Event> eventQueue,
            Id<Vehicle> vehicleId) {
        double travelTime = 0;
        if (route.getStartLinkId() != route.getEndLinkId()) {
            Id<Link> startLink = route.getStartLinkId();
            double linkEnterTime = startTime + 1;
            eventQueue.add(new LinkLeaveEvent(linkEnterTime, vehicleId, startLink));
            double linkLeaveTime = linkEnterTime;
            for (Id<Link> routeLinkId : route.getLinkIds()) {
                linkEnterTime = linkLeaveTime;
                eventQueue.add(new LinkEnterEvent(linkEnterTime, vehicleId, routeLinkId));
                double linkTime = carLinkTravelTimes.getLinkTravelTime(network.getLinks().get(routeLinkId),
                        linkEnterTime, null, null);
                travelTime += Math.max(linkTime, 1.0);
                linkLeaveTime = Math.max(linkEnterTime + 1, linkEnterTime + linkTime);
                eventQueue.add(new LinkLeaveEvent(linkLeaveTime, vehicleId, routeLinkId));
            }
            travelTime = linkLeaveTime - startTime;
        }
        eventQueue.add(new LinkEnterEvent(startTime + travelTime, vehicleId, route.getEndLinkId()));
        return travelTime + carLinkTravelTimes.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()),
                travelTime + startTime, null, null);
    }
}
