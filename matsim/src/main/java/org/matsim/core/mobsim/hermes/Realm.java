/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.hermes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.EventArray;
import org.matsim.core.utils.collections.IntArrayMap;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayDeque;
import java.util.ArrayList;

class Realm {
	private final ScenarioImporter si;
    // Global array of links.
    // Note: the id of the link is its index in the array.
    private final HLink[] links;
    // Internal realm links on hold until a specific timestamp (in seconds).
    // Internal means that the source and destination realm of are the same.
    private final ArrayList<ArrayDeque<HLink>> delayedLinksByWakeupTime;
    // Agents on hold until a specific timestamp (in seconds).
    private final ArrayList<ArrayDeque<Agent>> delayedAgentsByWakeupTime;
    // Agents waiting in pt stations. Should be used as follows:
    // agent_stops.get(curr station id).get(line id) -> queue of agents
    private final IdMap<TransitStopFacility, IntArrayMap<ArrayDeque<Agent>>> agent_stops;
    // stop ids for each route: int[] stop_ids = route_stops_by_route_no[route_no]
    protected int[][] route_stops_by_route_no;
    // line id of a particular route
    private final int[] line_of_route;
    // queue of sorted events by time
    private EventArray sortedEvents;
    // MATSim event manager.
    private final EventsManager eventsManager;
    // Current timestamp
    private int secs;
    Logger log = LogManager.getLogger(Realm.class);

    public Realm(ScenarioImporter scenario, EventsManager eventsManager) {
        this.si = scenario;
        this.links = scenario.hermesLinks;
        // The plus one is necessary because we peek into the next slot on each tick.
        this.delayedLinksByWakeupTime = new ArrayList<>();
        this.delayedAgentsByWakeupTime = new ArrayList<>();
        this.agent_stops = scenario.agentStops;
        this.route_stops_by_route_no = scenario.routeStopsByRouteNo;
        this.line_of_route = scenario.lineOfRoute;
        this.sortedEvents = new EventArray();
        this.eventsManager = eventsManager;

        // the last position is to store events that will not happen...
        for (int i = 0; i <= HermesConfigGroup.SIM_STEPS + 1; i++) {
            delayedLinksByWakeupTime.add(new ArrayDeque<>());
            delayedAgentsByWakeupTime.add(new ArrayDeque<>());
        }
    }

    public void log(int time, String s) {
        if (HermesConfigGroup.DEBUG_REALMS) {
            log.debug(String.format("Hermes [ time = %d ] %s", time, s));
        }
    }

    private void addDelayedAgent(Agent agent, int until) {
        if (HermesConfigGroup.DEBUG_REALMS) log(secs, String.format("agent %d delayed until %d", agent.id, until));
        delayedAgentsByWakeupTime.get(Math.min(until, HermesConfigGroup.SIM_STEPS + 1)).add(agent);
    }

    private void addDelayedLink(HLink link, int until) {
        if (HermesConfigGroup.DEBUG_REALMS)
            log(secs, String.format("link %d delayed until %d size %d peek agent %d", link.id(), until, link.queue().size(), link.queue().peek().id));
        delayedLinksByWakeupTime.get(Math.min(until, HermesConfigGroup.SIM_STEPS + 1)).add(link);
    }

    private void advanceAgentandSetEventTime(Agent agent) {
        advanceAgent(agent);
        // set time in agent's event.
        setEventTime(agent, Agent.getPlanEvent(agent.currPlan()), secs, false);
    }

    private void advanceAgent(Agent agent) {
        if (HermesConfigGroup.DEBUG_REALMS) {
            long centry = agent.currPlan();
            log(secs, String.format("agent %d finished %s (prev plan index is %d)", agent.id, Agent.toString(centry), agent.planIndex));
        }
        agent.planIndex++;
        if (HermesConfigGroup.DEBUG_REALMS) {
            long nentry = agent.currPlan();
            log(secs, String.format("agent %d starting %s (new plan index is %d)", agent.id, Agent.toString(nentry), agent.planIndex));
        }
    }

    protected boolean processAgentLink(Agent agent, long planentry, int currLinkId) {
        int linkid = Agent.getLinkPlanEntry(planentry);
        double velocity = Agent.getVelocityPlanEntry(planentry);
        HLink next = links[linkid];
        int prev_finishtime = agent.linkFinishTime;
        // this ensures that if no velocity is provided for the vehicle, we use the link
        velocity = velocity == 0 ? next.velocity() : velocity;
        // the max(1, ...) ensures that a link hop takes at least on step.
        int traveltime = (HermesConfigGroup.LINK_ADVANCE_DELAY + (int) Math.round(Math.max(1, next.length() / Math.min(velocity, next.velocity()))));
        agent.linkFinishTime = secs + traveltime;
        float storageCapacityPCU = agent.getStorageCapacityPCUE();
        if (next.push(agent,secs,storageCapacityPCU)) {
            advanceAgentandSetEventTime(agent);
            // If the agent we just added is the head, add to delayed links
            if (currLinkId != next.id() && next.queue().peek() == agent) {
                addDelayedLink(next, Math.max(agent.linkFinishTime, secs + 1));
            }
            return true;
        } else {
            agent.linkFinishTime = prev_finishtime;
            return false;
        }
    }

    protected boolean processAgentSleepFor(Agent agent, long planentry) {
        int sleep = Agent.getSleepPlanEntry(planentry);
        return processAgentSleepUntil(agent, secs + Math.max(1, sleep));
    }

    protected boolean processAgentSleepUntil(Agent agent, long planentry) {
        int sleep = Agent.getSleepPlanEntry(planentry);
        addDelayedAgent(agent, Math.max(sleep, secs + 1));
        updateCapacities(agent);
        advanceAgentandSetEventTime(agent);
        return true;
    }

    private void updateCapacities(Agent agent) {
        if (agent.isTransitVehicle()) {
            return;
            //assures PT vehicles never update their PCUEs, as only they have a capacity > 0
            //check is not strictly necessary in current code, adding it just in case
        }
        if (agent.plan.size < agent.planIndex + 3) {
            return;
        }
        if (Agent.getPlanHeader(agent.plan.get(agent.planIndex + 2)) == Agent.LinkType) {
            int category = Agent.getLinkPCEEntry(agent.nextPlan());
            agent.setStorageCapacityPCUE(si.getStorageCapacityPCE(category));
            agent.setFlowCapacityPCUE(si.getFlowCapacityPCE(category));
        }
    }

    protected boolean processAgentWait(Agent agent, long planentry) {
        advanceAgentandSetEventTime(agent);
        int routeNo = Agent.getRoutePlanEntry(planentry);
        int accessStop = Agent.getStopPlanEntry(planentry);
        // Note: getNextStop needs to be called after advanceAgent.
        int lineid = line_of_route[routeNo];

        try {
          agent_stops.get(accessStop)
            .get(lineid)
            .add(agent);
        } catch (NullPointerException npe) {
        	log.error(String.format("Hermes NPE agent=%d routeNo=%d accessStop=%d lineid=%d", agent.id, routeNo, accessStop, lineid), npe);
        }
        return true;
    }

    protected boolean processAgentStopArrive(Agent agent, long planentry) {
        addDelayedAgent(agent, secs + 1);
        advanceAgentandSetEventTime(agent);
        // Although we want the agent to be processed in the next tick, we
        // return true to remove the vehicle from the link that it is currently.
        return true;
    }

    protected boolean processAgentStopDelay(Agent agent, long planentry) {
        int stopid = Agent.getStopPlanEntry(planentry);
        int departure = Agent.getDeparture(planentry);

        // consume stop delay
        addDelayedAgent(agent, Math.max(secs + 1, departure));
        advanceAgent(agent);

        // drop agents
        for (Agent out : agent.egress(stopid)) {
            addDelayedAgent(out, secs + 1);
            // consume access, activate egress
            advanceAgentandSetEventTime(out);
            // set driver in agent's event
            setEventVehicle(out, Agent.getPlanEvent(out.currPlan()), agent.id);
        }

        // True is returned as the agent is already in the delayed list.
        return true;
    }

    protected boolean processAgentStopDepart(Agent agent, long planentry) {
        int routeNo = Agent.getRoutePlanEntry(planentry);
        int stopid = Agent.getStopPlanEntry(planentry);
        int lineid = line_of_route[routeNo];
        ArrayDeque<Agent> waiting_agents = agent_stops.get(stopid).get(lineid);

        // take agents
        if (waiting_agents != null) {
            ArrayList<Agent> removed = new ArrayList<>();
            for (Agent in : waiting_agents) {
                try {
                    int egressStop = in.getNextStopPlanEntry();
                    if (agent.willServeStop(egressStop)) {
                        if (agent.access(egressStop, in)) {
                            removed.add(in);
                            // consume wait in stop, activate access
                            advanceAgentandSetEventTime(in);
                            // set driver in agent's event
                            setEventVehicle(in, Agent.getPlanEvent(in.currPlan()), agent.id);
                        } else {
                            // agent could not enter, likely the vehicle is full
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            waiting_agents.removeAll(removed);
        }
        advanceAgentandSetEventTime(agent);
        // False is returned to force this agent to be processed in the next tick.
        // This will mean that the vehicle will be processed in the next tick.
        return false;
    }

    protected boolean processAgent(Agent agent, int currLinkId) {
        // Peek the next plan element and try to execute it.
        long planentry = agent.plan.get(agent.planIndex + 1);
        int type = Agent.getPlanHeader(planentry);
        switch (type) {
            case Agent.LinkType:        return processAgentLink(agent, planentry, currLinkId);
            case Agent.SleepForType:    return processAgentSleepFor(agent, planentry);
            case Agent.SleepUntilType:  return processAgentSleepUntil(agent, planentry);
            case Agent.StopArriveType:  return processAgentStopArrive(agent, planentry);
            case Agent.StopDelayType:   return processAgentStopDelay(agent, planentry);
            case Agent.StopDepartType:  return processAgentStopDepart(agent, planentry);
            case Agent.WaitType:        return processAgentWait(agent, planentry);
            case Agent.AccessType:      // The access event is consumed in the stop.
            case Agent.EgressType:      // The egress event is consumed in the stop.
            default:
                throw new RuntimeException(String.format(
                        "unknown plan element type %d, agent %d plan index %d",
                        type, agent.id, agent.planIndex + 1));
        }
    }

    protected int processAgentActivities(Agent agent) {
        boolean finished = agent.finished();
        // if finished, install times on last event.
        if (finished) {
            setEventTime(agent, agent.events().size() - 1, secs, true);
        }
        // -1 is used in the processAgent because the agent is not in a link currently.
        if (!finished && !processAgent(agent, -1)) {
            addDelayedAgent(agent, secs + 1);
            return 0;
        }
        return 1;
    }

    protected int processLinks(HLink link) {
        int routed = 0;
        Agent agent = link.queue().peek();
        while (agent.linkFinishTime <= secs && link.flow(secs, agent.getFlowCapacityPCUE())) {
            boolean finished = agent.finished();
            // if finished, install times on last event.
            if (finished) {
                setEventTime(agent, agent.events().size() - 1, secs, true);
            }
            if (finished || processAgent(agent, link.id())) {
                float storageCapacityPCE = agent.getStorageCapacityPCUE();
                link.pop(storageCapacityPCE);
                routed += 1;
                if ((agent = link.queue().peek()) == null) {
                    break;
                }
            } else {
                break;
            }
        }
        // If there is at least one agent in the link that could not be processed
        // In addition we check if this agent was not added in this tick.
        if (agent != null) {
            addDelayedLink(link, Math.max(agent.linkFinishTime, secs + 1));
        }
        return routed;
    }

    public void run() throws Exception {
        int routed = 0;
        Agent agent;
        HLink link;

        while (secs != HermesConfigGroup.SIM_STEPS) {
            if (secs % 3600 == 0) {
                log.info("Hermes running at " + Time.writeTime(secs));
            }
            while ((agent = delayedAgentsByWakeupTime.get(secs).poll()) != null) {
                if (HermesConfigGroup.DEBUG_REALMS) {
                    log(secs, String.format("Processing agent %d", agent.id));
                }
                routed += processAgentActivities(agent);

            }
            delayedAgentsByWakeupTime.set(secs, null);
            if (si.isDeterministicPt()) {
                for (Event e : si.getDeterministicPtEvents().get(secs)) {
                    sortedEvents.add(e);
                }
                si.getDeterministicPtEvents().get(secs).clear();
            }

            while ((link = delayedLinksByWakeupTime.get(secs).poll()) != null) {
                if (HermesConfigGroup.DEBUG_REALMS) {
                    log(secs, String.format("Processing link %d", link.id()));
                }
                routed += processLinks(link);
            }
            delayedLinksByWakeupTime.set(secs, null);
            if (HermesConfigGroup.DEBUG_REALMS && routed > 0) {
                log(secs, String.format("Processed %d agents", routed));
            }
            if (HermesConfigGroup.CONCURRENT_EVENT_PROCESSING && secs % 3600 == 0 && sortedEvents.size() > 0) {
                eventsManager.processEvents(sortedEvents);
                sortedEvents = new EventArray();
            }

            routed = 0;
            secs += 1;
        }
    }

    public void setEventTime(Agent agent, int agentId, int time, boolean lastEvent) {
        if (agentId != 0) {
            EventArray agentEvents = agent.events();
            Event event = agentEvents.get(agentId);

            for (; agent.eventsIndex <= agentId; agent.eventsIndex++) {
                agentEvents.get(agent.eventsIndex).setTime(time);
                if (HermesConfigGroup.DEBUG_REALMS)
                    log(secs, String.format("agent %d setEventTime (eventsIndex=%d) %s", agent.id, agent.eventsIndex, agentEvents.get(agent.eventsIndex).toString()));
                sortedEvents.add(agentEvents.get(agent.eventsIndex));
            }

            // Fix delay for PT events.
            if (event instanceof VehicleArrivesAtFacilityEvent vaafe) {
                vaafe.setDelay(vaafe.getTime() - vaafe.getDelay());

            } else if (event instanceof VehicleDepartsAtFacilityEvent vdafe) {
                vdafe.setDelay(vdafe.getTime() - vdafe.getDelay());
            }
            // This removes actend that is not issued by QSim.
            else if (lastEvent && event instanceof ActivityEndEvent) {
                sortedEvents.removeLast();
            }
        }
    }

    public void setEventVehicle(Agent agent, int eventId, int vehicleId) {
        if (eventId != 0) {
            Event event = agent.events().get(eventId);
            Id<Vehicle> vid = Id.get(si.matsim_id(vehicleId, true), Vehicle.class);
            if (event instanceof PersonEntersVehicleEvent) {
                ((PersonEntersVehicleEvent) event).setVehicleId(vid);
            } else if (event instanceof PersonLeavesVehicleEvent) {
                ((PersonLeavesVehicleEvent) event).setVehicleId(vid);
            } else {
                throw new RuntimeException(
                        String.format("vehicle id could not be set for event: %d", eventId));
            }
        }
    }

    ArrayList<ArrayDeque<HLink>> delayedLinks() { return this.delayedLinksByWakeupTime; }

    ArrayList<ArrayDeque<Agent>> delayedAgents() {
        return this.delayedAgentsByWakeupTime;
    }

    EventArray getSortedEvents() {
        return this.sortedEvents;
    }
}
