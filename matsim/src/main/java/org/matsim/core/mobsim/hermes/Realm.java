package org.matsim.core.mobsim.hermes;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.EventArray;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;

public class Realm {
	private final ScenarioImporter si;
    // Global array of links.
    // Note: the id of the link is its index in the array.
    private final Link[] links;
    // Internal realm links on hold until a specific timestamp (in seconds).
    // Internal means that the source and destination realm of are the same.
    private final ArrayList<ArrayDeque<Link>> delayedLinksByWakeupTime;
    // Agents on hold until a specific timestamp (in seconds).
    private final ArrayList<ArrayDeque<Agent>> delayedAgentsByWakeupTime;
    // Agents waiting in pt stations. Should be used as follows:
    // nqsim_stops.get(curr station id).get(line id).get(dst station id) -> queue of agents
    private final ArrayList<ArrayList<Map<Integer, ArrayDeque<Agent>>>> agent_stops;
    // stop ids per route id
    private final ArrayList<ArrayList<Integer>> stops_in_route;
    // line id of a particular route
    private final int[] line_of_route;
    // queue of sorted events by time
    private EventArray sorted_events;
    // MATSim event manager.
    private final ParallelEventsManager eventsManager;
    // Current timestamp
    private int secs;

    Logger log = Logger.getLogger(Realm.class);

    public Realm(ScenarioImporter scenario, EventsManager eventsManager) throws Exception {
    	this.si = scenario;
        this.links = scenario.hermes_links;
        // The plus one is necessary because we peek into the next slot on each tick.
        this.delayedLinksByWakeupTime = new ArrayList<>();
        this.delayedAgentsByWakeupTime = new ArrayList<>();
        this.agent_stops = scenario.agent_stops;
        this.stops_in_route = scenario.route_stops_by_index;
        this.line_of_route = scenario.line_of_route;
        this.sorted_events = new EventArray();
        this.eventsManager = (ParallelEventsManager)eventsManager;

	// the last position is to store events that will not happen...
        for (int i = 0; i <= HermesConfigGroup.SIM_STEPS + 1; i++) {
            delayedLinksByWakeupTime.add(new ArrayDeque<>(Math.max(16,this.links.length/4)));
            delayedAgentsByWakeupTime.add(new ArrayDeque<>(Math.max(512,this.si.agent_persons/1000)));
        }
    }

    public void log(int time, String s) {
        if (HermesConfigGroup.DEBUG_REALMS) {
            log.debug(String.format("ETHZ [ time = %d ] %s", time, s));
        }
    }

    private void add_delayed_agent(Agent agent, int until) {
        if (HermesConfigGroup.DEBUG_REALMS) log(secs, String.format("agent %d delayed until %d", agent.id, until));
        delayedAgentsByWakeupTime.get(Math.min(until, HermesConfigGroup.SIM_STEPS + 1)).add(agent);
    }

    private void add_delayed_link(Link link, int until) {
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
        long centry = agent.currPlan();
        if (HermesConfigGroup.DEBUG_REALMS)
            log(secs, String.format("agent %d finished %s (prev plan index is %d)", agent.id, Agent.toString(centry), agent.planIndex));
        agent.planIndex++;
        long nentry = agent.currPlan();
        if (HermesConfigGroup.DEBUG_REALMS)
            log(secs, String.format("agent %d starting %s (new plan index is %d)", agent.id, Agent.toString(nentry), agent.planIndex));
    }

    protected boolean processAgentLink(Agent agent, long planentry, int currLinkId) {
        int linkid = Agent.getLinkPlanEntry(planentry);
        int velocity = Agent.getVelocityPlanEntry(planentry);
        Link next = links[linkid];
        int prev_finishtime = agent.linkFinishTime;
        // this ensures that if no velocity is provided for the vehicle, we use the link
        velocity = velocity == 0 ? next.velocity() : velocity;
        // the max(1, ...) ensures that a link hop takes at least on step.
        int traveltime = HermesConfigGroup.LINK_ADVANCE_DELAY + Math.max(1, next.length() / Math.min(velocity, next.velocity()));
        agent.linkFinishTime = secs + traveltime;

        if (next.push(agent)) {
            advanceAgentandSetEventTime(agent);
            // If the agent we just added is the head, add to delayed links
            if (currLinkId != next.id() && next.queue().peek() == agent) {
                add_delayed_link(next, Math.max(agent.linkFinishTime, secs + 1));
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
        add_delayed_agent(agent, Math.max(sleep, secs + 1));
        advanceAgentandSetEventTime(agent);
        return true;
    }

    protected boolean processAgentWait(Agent agent, long planentry) {
        advanceAgentandSetEventTime(agent);
        int routeid = Agent.getRoutePlanEntry(planentry);
        int accessStop = Agent.getStopPlanEntry(planentry);
        // Note: getNextStop needs to be called after advanveAgent.
        int egressStop = agent.getNextStopPlanEntry();
        int lineid = line_of_route[routeid];

        ArrayList<Map<Integer, ArrayDeque<Agent>>> list;
        Map<Integer, ArrayDeque<Agent>> list2;
        ArrayDeque<Agent> list3;
        try {
        	list = agent_stops.get(accessStop);
            list2 = list.get(lineid);
            list3 = list2.get(egressStop);
            list3.add(agent);
        } catch (NullPointerException npe) {
        	System.out.println(String.format("ETHZ NPE agent=%d routeid=%d accessStop=%d lineid=%d egressStop=%d", agent.id, routeid, accessStop, egressStop, lineid));
        }
        return true;
    }

    protected boolean processAgentStopArrive(Agent agent, long planentry) {
        add_delayed_agent(agent, secs + 1);
        advanceAgentandSetEventTime(agent);
        // Although we want the agent to be processed in the next tick, we
        // return true to remove the vehicle from the link that it is currently.
        return true;
    }

    protected boolean processAgentStopDelay(Agent agent, long planentry) {
        int routeid = Agent.getRoutePlanEntry(planentry);
        int stopid = Agent.getStopPlanEntry(planentry);
        int stopidx = Agent.getStopIndexPlanEntry(planentry);
        int lineid = line_of_route[routeid];
        int departure = Agent.getDeparture(planentry);
        ArrayList<Integer> next_stops = stops_in_route.get(routeid);
        Map<Integer, ArrayDeque<Agent>> agents_next_stops =
            agent_stops.get(stopid).get(lineid);

        // consume stop delay
        add_delayed_agent(agent, Math.max(secs + 1, departure));
        advanceAgent(agent);

        // drop agents
        for (Agent out : agent.egress(stopidx)) {
            add_delayed_agent(out, secs + 1);
            // consume access, activate egress
            advanceAgentandSetEventTime(out);
            // set driver in agent's event
            setEventVehicle(out, Agent.getPlanEvent(out.currPlan()), agent.id);
        }

        // take agents
        for (int idx = stopidx; idx < next_stops.size(); idx++) {
            ArrayDeque<Agent> in_agents = agents_next_stops.get(next_stops.get(idx));

            if (in_agents == null) {
                continue;
            }

            ArrayList<Agent> removed = new ArrayList<>();
            for (Agent in : in_agents) {
                if (!agent.access(idx, in)) {
                    break;
                }
                removed.add(in);
                // consume wait in stop, activate access
                advanceAgentandSetEventTime(in);
                // set driver in agent's event
                setEventVehicle(in, Agent.getPlanEvent(in.currPlan()), agent.id);
            }
            in_agents.removeAll(removed);
        }

        // True is returned as the agent is already in the delayed list.
        return true;
    }

    protected boolean processAgentStopDepart(Agent agent, long planentry) {
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
                    "unknow plan element type %d, agent %d plan index %d",
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
            add_delayed_agent(agent, secs + 1);
            return 0;
        }
        return 1;
    }

    protected int processLinks(Link link) {
        int routed = 0;
        Agent agent = link.queue().peek();
        int curr_flow = link.flow(secs);

        while (agent.linkFinishTime <= secs && curr_flow > 0) {
            boolean finished = agent.finished();
            // if finished, install times on last event.
            if (finished) {
                setEventTime(agent, agent.events().size() - 1, secs, true);
            }
            if (finished || processAgent(agent, link.id())) {
                link.pop();
                curr_flow -= 1;
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
            add_delayed_link(link, Math.max(agent.linkFinishTime, secs + 1));
        }
        return routed;
    }

    public void run() throws Exception {
    	int routed = 0;
        Agent agent = null;
        Link link = null;

        while (secs != HermesConfigGroup.SIM_STEPS) {
            if (secs % 3600 == 0){
                log.info("Hermes running at " + Time.writeTime(secs));
            }
            while ((agent = delayedAgentsByWakeupTime.get(secs).poll()) != null) {
                if (HermesConfigGroup.DEBUG_REALMS) log(secs, String.format("Processing agent %d", agent.id));
                routed += processAgentActivities(agent);

            }
            delayedAgentsByWakeupTime.set(secs,null);

            while ((link = delayedLinksByWakeupTime.get(secs).poll()) != null) {
                if (HermesConfigGroup.DEBUG_REALMS) log(secs, String.format("Processing link %d", link.id()));
                routed += processLinks(link);
            }
            delayedLinksByWakeupTime.set(secs,null);
            if (HermesConfigGroup.DEBUG_REALMS && routed > 0) log(secs, String.format("Processed %d agents", routed));

            if (HermesConfigGroup.CONCURRENT_EVENT_PROCESSING && secs % 3600 == 0 && sorted_events.size() > 0) {
                eventsManager.processEvents(sorted_events);
                sorted_events = new EventArray();
            }

            routed = 0;
            secs += 1;
        }
    }

    public void setEventTime(Agent agent, int eventid, int time, boolean lastevent) {
        // TODO - is this check necessary? -> I am trying to remove all instances where it is zero...
        if (eventid != 0) {
        	EventArray agentevents = agent.events();
            Event event = agentevents.get(eventid);

            for (; agent.eventsIndex <= eventid; agent.eventsIndex++) {
            	agentevents.get(agent.eventsIndex).setTime(time);
                if (HermesConfigGroup.DEBUG_REALMS)
                    log(secs, String.format("agent %d setEventTime (eventsIndex=%d) %s", agent.id, agent.eventsIndex, agentevents.get(agent.eventsIndex).toString()));
                sorted_events.add(agentevents.get(agent.eventsIndex));
            }

            // Fix delay for PT events.
            if (event instanceof VehicleArrivesAtFacilityEvent) {
			    VehicleArrivesAtFacilityEvent vaafe = (VehicleArrivesAtFacilityEvent) event;
			    vaafe.setDelay(vaafe.getTime() - vaafe.getDelay());
		    }
            else if (event instanceof VehicleDepartsAtFacilityEvent) {
			    VehicleDepartsAtFacilityEvent vdafe = (VehicleDepartsAtFacilityEvent) event;
			    vdafe.setDelay(vdafe.getTime() - vdafe.getDelay());
		    }
		    // This removes actend that is not issued by QSim.
		    else if (lastevent && event instanceof ActivityEndEvent) {
		        sorted_events.removeLast();
		    }
        }
    }

    public void setEventVehicle(Agent agent, int eventid, int vehicleid) {
        if (eventid != 0) {
            Event event = agent.events().get(eventid);
            Id<Vehicle> vid = Id.get(si.matsim_id(vehicleid,  true), Vehicle.class);
            if (event instanceof PersonEntersVehicleEvent) {
                ((PersonEntersVehicleEvent)event).setVehicleId(vid);
            } else if (event instanceof PersonLeavesVehicleEvent) {
                ((PersonLeavesVehicleEvent)event).setVehicleId(vid);
            } else {
                throw new RuntimeException(
                    String.format("vehicle id could not be set for event: %d", eventid));
            }
        }
    }

    public int time() { return this.secs; }
    public Link[] links() { return this.links; }
    public ArrayList<ArrayDeque<Link>> delayedLinks() { return this.delayedLinksByWakeupTime; }
    public ArrayList<ArrayDeque<Agent>> delayedAgents() { return this.delayedAgentsByWakeupTime; }
    public EventArray getSortedEvents() { return this.sorted_events; }
}
