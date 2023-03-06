/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.mobsim.qsim.pt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine.TransitAgentTriesToTeleportException;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import ch.sbb.matsim.config.SBBTransitConfigGroup;

/**
 * @author cdobler
 */
public class SBBTransitEngineRunner implements Steppable, MobsimEngine {

    private static final Logger log = LogManager.getLogger(SBBTransitEngineRunner.class);

    private final SBBTransitConfigGroup config;
    private final TransitConfigGroup ptConfig;
    private final QSim qSim;
    private final ReplanningContext context;
    private final TransitStopAgentTracker agentTracker;
    private final TransitSchedule schedule;
    private final PriorityQueue<TransitEvent> eventQueue = new PriorityQueue<>();
    private final Map<TransitRoute, List<Link[]>> linksCache;
    private final PriorityQueue<LinkEvent> linkEventQueue;
    private InternalInterface internalInterface;
    private boolean createLinkEvents = false;

    @Inject
    public SBBTransitEngineRunner(final QSim qSim, final ReplanningContext context, final TransitStopAgentTracker agentTracker, final Map<TransitRoute, List<Link[]>> linksCache) {
        this.qSim = qSim;
        this.context = context;
        this.config = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), SBBTransitConfigGroup.GROUP_NAME, SBBTransitConfigGroup.class);
        this.ptConfig = qSim.getScenario().getConfig().transit();
        this.schedule = qSim.getScenario().getTransitSchedule();
//        this.agentTracker = new TransitStopAgentTracker(qSim.getEventsManager());
        this.agentTracker = agentTracker;
        this.linksCache = linksCache;
        if (this.config.getCreateLinkEventsInterval() > 0) {
            this.linkEventQueue = new PriorityQueue<>();
        } else {
            this.linkEventQueue = null;
        }
        checkSettings();
    }

    private void checkSettings() {
        if (this.config.getDeterministicServiceModes().isEmpty()) {
            log.warn("There are no modes registered for the deterministic transit simulation, so no transit vehicle will be handled by this engine.");
        }
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
        this.internalInterface = internalInterface;
    }

//    @Override
    public boolean handleDeparture(double time, MobsimAgent agent, Id<Link> linkId) {
        String mode = agent.getMode();
        if (this.ptConfig.getTransitModes().contains(mode)) {
            handlePassengerDeparture(agent, linkId);
            return true;
        }
        if (this.config.getDeterministicServiceModes().contains(mode)) {
            handleDeterministicDriverDeparture(agent, time);
            return true;
        }
        return false;
    }

    @Override
    public void onPrepareSim() {
        // not much to do, all pre-processing is done in insertAgentsIntoMobsim
        if (this.context != null) {
            int iteration = this.context.getIteration();
            int createEventsInterval = this.config.getCreateLinkEventsInterval();
            final boolean writingEventsAtAll = createEventsInterval > 0;
            final boolean regularWriteEvents = writingEventsAtAll && iteration % createEventsInterval == 0;
            this.createLinkEvents = writingEventsAtAll && regularWriteEvents;
        }
    }

    @Override
    public void doSimStep(double time) {
        if (this.createLinkEvents) {
            LinkEvent linkEvent = this.linkEventQueue.peek();
            while (linkEvent != null && linkEvent.time <= time) {
                this.linkEventQueue.poll();
                this.qSim.getEventsManager().processEvent(new LinkLeaveEvent(time, linkEvent.vehicleId, linkEvent.fromLinkId));
                this.qSim.getEventsManager().processEvent(new LinkEnterEvent(time, linkEvent.vehicleId, linkEvent.toLinkId));
                linkEvent = this.linkEventQueue.peek();
            }
        }

        TransitEvent event = this.eventQueue.peek();
        while (event != null && event.time <= time) {
            handleTransitEvent(this.eventQueue.poll());
            event = this.eventQueue.peek();
        }
    }

    @Override
    public void afterSim() {
        // check that all agents have arrived, generate stuck events otherwise
        double now = this.qSim.getSimTimer().getTimeOfDay();
        for (Map.Entry<Id<TransitStopFacility>, List<PTPassengerAgent>> agentsAtStop : this.agentTracker.getAgentsAtStop().entrySet()) {
            TransitStopFacility stop = this.schedule.getFacilities().get(agentsAtStop.getKey());
            for (PTPassengerAgent agent : agentsAtStop.getValue()) {
                this.qSim.getEventsManager().processEvent(new PersonStuckEvent(now, agent.getId(), stop.getLinkId(), agent.getMode()));
                this.qSim.getAgentCounter().decLiving();
                this.qSim.getAgentCounter().incLost();
            }
        }

        // check for agents still in a vehicle
        TransitEvent event;
        while ((event = this.eventQueue.poll()) != null) {
            Id<Link> nextStopLinkId = event.context.nextStop.getStopFacility().getLinkId();
            for (PassengerAgent agent : event.context.driver.getVehicle().getPassengers()) {
                this.qSim.getEventsManager().processEvent(new PersonStuckEvent(now, agent.getId(), nextStopLinkId, agent.getMode()));
                this.qSim.getAgentCounter().decLiving();
                this.qSim.getAgentCounter().incLost();
            }
        }
    }

    private void handlePassengerDeparture(MobsimAgent agent, Id<Link> linkId) {
        PTPassengerAgent passenger = (PTPassengerAgent) agent;
        // this puts the agent into the transit stop.
        Id<TransitStopFacility> accessStopId = passenger.getDesiredAccessStopId();
        if (accessStopId == null) {
            // looks like this agent has a bad transit route, likely no
            // route could be calculated for it
            log.error("pt-agent doesn't know to what transit stop to go to. Removing agent from simulation. Agent " + passenger.getId().toString());
            this.qSim.getAgentCounter().decLiving();
            this.qSim.getAgentCounter().incLost();
            return;
        }
        TransitStopFacility stop = this.schedule.getFacilities().get(accessStopId);
        if (stop.getLinkId() == null || stop.getLinkId().equals(linkId)) {
            double now = this.qSim.getSimTimer().getTimeOfDay();
            this.agentTracker.addAgentToStop(now, passenger, stop.getId());
            this.internalInterface.registerAdditionalAgentOnLink(agent);
        } else {
            throw new TransitAgentTriesToTeleportException("Agent " + passenger.getId() + " tries to enter a transit stop at link " + stop.getLinkId() + " but really is at " + linkId + "!");
        }
    }

    private void handleDeterministicDriverDeparture(MobsimAgent agent, double now) {
        SBBTransitDriverAgent driver = (SBBTransitDriverAgent) agent;
        TransitRoute trRoute = driver.getTransitRoute();
        List<Link[]> links = this.createLinkEvents ? this.linksCache.computeIfAbsent(trRoute, r -> getLinksPerStopAlongRoute(r, this.qSim.getScenario().getNetwork())) : null;
        TransitContext context = new TransitContext(driver, links);
        this.qSim.getEventsManager().processEvent(new PersonEntersVehicleEvent(now, driver.getId(), driver.getVehicle().getId()));
        if (this.createLinkEvents) {
            Id<Link> linkId = driver.getCurrentLinkId();
            String mode = driver.getMode();
            this.qSim.getEventsManager().processEvent(new VehicleEntersTrafficEvent(now, driver.getId(), linkId, driver.getVehicle().getId(), mode, 1.0));
        }
        TransitEvent event = new TransitEvent(now, TransitEventType.ArrivalAtStop, context);
        this.eventQueue.add(event);
    }

    private void handleTransitEvent(TransitEvent event) {
        switch (event.type) {
            case ArrivalAtStop:
                handleArrivalAtStop(event);
                break;
            case PassengerExchange:
                handlePassengerExchange(event);
                break;
            case DepartureAtStop:
                handleDepartureAtStop(event);
                break;
            default:
                throw new RuntimeException("Unsupported TransitEvent type.");
        }
    }

    private void handleArrivalAtStop(TransitEvent event) {
        event.context.driver.arrive(event.context.nextStop, event.time);
        handlePassengerExchange(event);
    }

    private void handlePassengerExchange(TransitEvent event) {
        SBBTransitDriverAgent driver = event.context.driver;
        TransitRouteStop stop = event.context.nextStop;
        double stopTime = driver.handleTransitStop(stop.getStopFacility(), event.time);
        if (stopTime > 0) {
            TransitEvent depEvent = new TransitEvent(event.time + stopTime, TransitEventType.PassengerExchange, event.context);
            this.eventQueue.add(depEvent);
        } else {
            handleDepartureAtStop(new TransitEvent(event.time, TransitEventType.DepartureAtStop, event.context));
        }
    }

    private void handleDepartureAtStop(TransitEvent event) {
        SBBTransitDriverAgent driver = event.context.driver;
        TransitRouteStop stop = event.context.nextStop;
        driver.depart(stop.getStopFacility(), event.time);

        TransitRouteStop nextStop = event.context.advanceStop();
        if (nextStop != null) {
            double arrOffset = nextStop.getArrivalOffset().or(nextStop.getDepartureOffset()).seconds();
            double arrTime = driver.getDeparture().getDepartureTime() + arrOffset;
            if (arrTime < event.time) {
                // looks like we had a huge delay before.
                // MATSim does not allow to send events with an earlier time than the last time,
                // so we have to adapt a bit here.
                arrTime = event.time;
            }
            TransitEvent arrEvent = new TransitEvent(arrTime, TransitEventType.ArrivalAtStop, event.context);
            this.eventQueue.add(arrEvent);
            if (this.createLinkEvents) {
                precomputeLinkEvents(event.time, arrTime, event.context.linksToNextStop, driver.getVehicle(), driver);
            }
        } else {
            if (this.createLinkEvents) {
                Id<Link> linkId = driver.getDestinationLinkId();
                String mode = driver.getMode();
                this.qSim.getEventsManager().processEvent(new VehicleLeavesTrafficEvent(event.time, driver.getId(), linkId, driver.getVehicle().getId(), mode, 1.0));
            }
            this.qSim.getEventsManager().processEvent(new PersonLeavesVehicleEvent(event.time, driver.getId(), driver.getVehicle().getId()));
            driver.endLegAndComputeNextState(event.time);
            this.internalInterface.arrangeNextAgentState(driver);
        }
    }

    private void precomputeLinkEvents(double depTime, double arrTime, Link[] linksToNextStop, TransitVehicle vehicle, SBBTransitDriverAgent driver) {
        double travelTime = arrTime - depTime;
        double totalLength = 0.0;
        boolean isDepartureLink = true;
        boolean hasLinks = false;
        for (Link link : linksToNextStop) {
            if (isDepartureLink) {
                isDepartureLink = false;
            } else {
                totalLength += link.getLength();
                hasLinks = true;
            }
        }
        if (hasLinks) {
            double secondsPerMeter = totalLength > 0 ? travelTime / totalLength : 0;
            double travelledLength = 0;
            Link fromLink = null;
            for (Link toLink : linksToNextStop) {
                if (fromLink != null) {
                    double time = depTime + travelledLength * secondsPerMeter;
                    if (travelTime == 0) {
                        // create the events right now, so they stay in correct order before next arrival
                        this.qSim.getEventsManager().processEvent(new LinkLeaveEvent(time, vehicle.getId(), fromLink.getId()));
                        this.qSim.getEventsManager().processEvent(new LinkEnterEvent(time, vehicle.getId(), toLink.getId()));
                    } else {
                        this.linkEventQueue.add(new LinkEvent(time, fromLink.getId(), toLink.getId(), vehicle.getId()));
                    }
                    travelledLength += toLink.getLength();
                }
                fromLink = toLink;
            }
        }
    }

    /**
     * Returns for each TransitRouteStop the Links leading from that stop to the next one. The returned list has the same number of entries as the TransitRoute has stops. The list for the last stop
     * might contain some links, depending on the provided NetworkRoute. If the NetworkRoute contains links before the first stop, they will be ignored and not returned.
     * <p>
     * The first link of each link-list is the departure link, the last link in the list is the arrival link.
     *
     * @param trRoute TransitRoute for which to get the links
     * @return list containing the links leading from each stop to the next, ordered by the sequence of TransitRouteStops in the TransitRoute
     */
    private List<Link[]> getLinksPerStopAlongRoute(TransitRoute trRoute, Network network) {
        Iterator<TransitRouteStop> stopIter = trRoute.getStops().iterator();
        TransitRouteStop nextStop = stopIter.hasNext() ? stopIter.next() : null;
        Id<Link> nextStopLinkId = nextStop.getStopFacility().getLinkId();

        NetworkRoute netRoute = trRoute.getRoute();
        List<Id<Link>> allLinkIds = new ArrayList<>();
        allLinkIds.add(netRoute.getStartLinkId());
        allLinkIds.addAll(netRoute.getLinkIds());
        if (!netRoute.getStartLinkId().equals(netRoute.getEndLinkId()) || allLinkIds.size() > 1) {
            // either the start- and end link are different, or there are additional links in between
            allLinkIds.add(netRoute.getEndLinkId());
        }

        List<Link[]> result = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        TransitRouteStop lastStop = null;
        for (Id<Link> linkId : allLinkIds) {
            Link link = network.getLinks().get(linkId);
            links.add(link);
            boolean recheckLink = true;
            while (recheckLink) {
                recheckLink = false;
                if (linkId.equals(nextStopLinkId)) {
                    if (lastStop != null) {
                        int linkCount = links.size();
                        if (linkCount > 1) {
                            // if it's only 1 link, it means we're still on the same link as the previous stop, so ignore it.
                            result.add(links.toArray(new Link[linkCount]));
                        } else {
                            result.add(new Link[0]);
                        }
                    }
                    links.clear();
                    links.add(link); // add this link again for the linkLeaveEvent
                    lastStop = nextStop;
                    nextStop = stopIter.hasNext() ? stopIter.next() : null;
                    nextStopLinkId = nextStop == null ? null : nextStop.getStopFacility().getLinkId();
                    recheckLink = nextStopLinkId != null;
                }
            }
        }
        // add any potential links after the last stop
        result.add(links.toArray(new Link[links.size()]));
        return result;
    }

    private enum TransitEventType {ArrivalAtStop, PassengerExchange, DepartureAtStop}

    private static class TransitContext {

        SBBTransitDriverAgent driver;
        Iterator<TransitRouteStop> stopIter;
        TransitRouteStop nextStop;
        Iterator<Link[]> linksIter;
        Link[] linksToNextStop;

        TransitContext(SBBTransitDriverAgent driver, List<Link[]> links) {
            this.driver = driver;
            this.stopIter = driver.getTransitRoute().getStops().iterator();
            this.nextStop = this.stopIter.next();
            this.linksIter = links == null ? null : links.iterator();
            this.linksToNextStop = links == null ? null : new Link[0]; // the route to the first stop is empty by definition
        }

        private TransitRouteStop advanceStop() {
            if (this.stopIter.hasNext()) {
                this.nextStop = this.stopIter.next();
            } else {
                this.nextStop = null;
            }
            if (this.linksIter != null && this.linksIter.hasNext()) {
                this.linksToNextStop = this.linksIter.next();
            } else {
                this.linksToNextStop = null;
            }
            return this.nextStop;
        }
    }

    private static class TransitEvent implements Comparable<TransitEvent> {

        double time;
        TransitEventType type;
        TransitContext context;

        TransitEvent(double time, TransitEventType type, TransitContext context) {
            this.time = time;
            this.type = type;
            this.context = context;
        }

        @Override
        public int compareTo(TransitEvent o) {
            int result = Double.compare(this.time, o.time);
            if (result == 0) {
                if (this.type == o.type) {
                    result = this.context.driver.getId().compareTo(o.context.driver.getId());
                } else {
                    // arrivals should come before departures
                    result = this.type == TransitEventType.ArrivalAtStop ? -1 : +1;
                }
            }
            return result;
        }
    }

    private static class LinkEvent implements Comparable<LinkEvent> {

        double time;
        Id<Link> fromLinkId;
        Id<Link> toLinkId;
        Id<Vehicle> vehicleId;

        LinkEvent(double time, Id<Link> fromLinkId, Id<Link> toLinkId, Id<Vehicle> vehicleId) {
            this.time = time;
            this.fromLinkId = fromLinkId;
            this.toLinkId = toLinkId;
            this.vehicleId = vehicleId;
        }

        @Override
        public int compareTo(LinkEvent o) {
            int result = Double.compare(this.time, o.time);
            if (result == 0) {
                result = this.vehicleId.compareTo(o.vehicleId);
            }
            return result;
        }
    }

}
