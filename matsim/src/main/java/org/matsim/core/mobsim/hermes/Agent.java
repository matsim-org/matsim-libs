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

import java.util.ArrayList;
import java.util.Arrays;
import org.matsim.core.events.EventArray;

public class Agent {

	public static class PlanArray {
		long[] array;
		int size;

		public PlanArray() {
			this.array = new long[32];
		}

		public void add(long element) {
			if (size == array.length) {
				array = Arrays.copyOf(array, array.length * 2);
			}
			array[size++] = element;
		}

		public int size() {
			return size;
		}
		public long get(int index) {
			return array[index];
		}

		public void clear() {
			for (int i = 0; i < size; i++) {
				array[i] = 0;
			}
			size = 0;
		}
	}

    // Types of plan headers.
    // agent sleeps for some time.
    public static final int SleepForType   =  0;
    // agent sleeps until a specific time of the day
    public static final int SleepUntilType =  1;
    // agent goes through some link
    public static final int LinkType       =  2;
    // non-vehicle agent enters a vehicle agent
    public static final int AccessType     =  3;
    // non-vehicle agent leaves a vehicle agent
    public static final int EgressType     =  4;
    // vehicle agent arrives at a PT stop
    public static final int StopArriveType =  5;
    // vehicle agent leaves at a PT stop
    public static final int StopDepartType =  6;
    // non-vehicle agent waits for vehicle agent at PT stop
    public static final int WaitType       =  7;
    // vehicle agent waits at the PT stop until it can leave the stop
    public static final int StopDelayType  =  8;

    // Id of the link (index for World.agents).
    protected final int id;

    // Array of plan elements. A plan element has the following structure:
    // <4 bit header><60 bit payload>
    // Possible headers (binary format) and corresponding payload:
    // <0000> SleepForType    | 4 bits unused | 16 bit event id  | 8 bits unused   | 32 bit sleep for a number of second
    // <0001> SleepUntilType  | 4 bits unused | 16 bit event id  | 8 bits unused   | 32 bit speep until a specific time
    // <0010> LinkType        | 4 bits PCEcategory | 16 bit event id  | 32 bit link id  | 8 bit velocity
    // <0111> WaitType        | 4 bits unused | 16 bit event id  | 8 bits unused   | 16 bit route id | 16 station id
    // <0011> AccessType      | 4 bits unused | 16 bit event id  | 8 bits unused   | 16 bit route id | 16 station id
    // <0100> EgressType      | 4 bits unused | 16 bit event id  | 8 bits unused   | 16 bit route id | 16 station id
    // <0101> StopArriveType  | 4 bits unused | 16 bit event id  | 8 station idx   | 16 bit route id | 16 station id
    // <1000> StopDelayType   | 20 departure sec                 | 8 station idx   | 16 bit route id | 16 station id
    // <0110> StopDepartType  | 4 bits unused | 16 bit event id  | 8 station idx   | 16 bit route id | 16 station id
    protected final PlanArray plan; // TODO - use a byte buffer instead of a long[]...

    protected final EventArray events;

    // Current position in plan. Using this index in the plan will yield what
    // the agent is doing currently. Note that we trigger the corresponding
    // events when the plan entry is activated.
    protected int planIndex;

    protected int eventsIndex;

     // Timestamp of when the agent will be ready to exit link.
    protected int linkFinishTime;

    // Number of passagers that this agent can take (zero for personal vehicles)
    private int capacity;

    // Number of passegers that are currently being transported.
    private int passengersInside;

    // Array of passagers on this vehicle.
    private ArrayList<ArrayList<Agent>> passengersByStop;

    public Agent(int id, int capacity, PlanArray plan, EventArray events) {
        this.id = id;
        this.plan = plan;
        this.events = events;
        this.capacity = capacity;
        this.passengersByStop = new ArrayList<>(HermesConfigGroup.MAX_STOP_IDX + 1);
        for (int i = 0; i < HermesConfigGroup.MAX_STOP_IDX + 1; i++) {
            this.passengersByStop.add(new ArrayList<>());
        }
    }

    public static long preparePlanEventEntry(long type, long element) {
        long planEntry = (type << 60) | element;
        if (HermesConfigGroup.DEBUG_EVENTS) {
            validatePlanEntry(planEntry);
        }
        return planEntry;
    }

    public int id() { return this.id; }
    public int linkFinishTime() { return this.linkFinishTime; }
    public int planIndex() { return this.planIndex; }
    public PlanArray plan() { return this.plan; }
    public EventArray events() { return this.events; }
    public long currPlan() { return this.plan.get(planIndex); }
    public long prevPlan() { return this.plan.get(planIndex - 1); }
    public boolean finished() { return planIndex >= (plan.size() - 1); }

    public int capacity() {
        return this.capacity;
    }

    public boolean isVehicle() {
        return this.passengersByStop == null;
    }

    public ArrayList<Agent> egress(int stopidx) {
        ArrayList<Agent> ret = passengersByStop.get(stopidx);
        passengersInside -= ret.size();
        passengersByStop.set(stopidx, new ArrayList<>());
        return ret;
    }

    public boolean access(int stopidx, Agent agent) {
        if (passengersInside == capacity) {
            return false;
        } else {
            passengersByStop.get(stopidx).add(agent);
            passengersInside++;
            return true;
        }
    }

    public int getNextStopPlanEntry() {
        // TODO - install assert checking if the next entry is an egress?
        // +2 is used to peek where the agent wants to leave the vehicle.
        // +1 is the access plan element which was not yet consumed.
        return getStopPlanEntry(plan.get(planIndex + 2));
    }

    public static int getPlanHeader         (long plan) { return (int)((plan >> 60) & 0x000000000000000FL); }
    public static int getPlanEvent          (long plan) { return (int)((plan >> 40) & 0x000000000000FFFFL); }
    public static int getDeparture          (long plan) { return (int)((plan >> 40) & 0x00000000000FFFFFL); }
    public static int getLinkPlanEntry      (long plan) {return (int) ((plan >> 8) & 0x00000000FFFFFFFFL); }
    public static int getLinkPCEEntry       (long plan) {
        return (int) ((plan >> 56) & 0x000000000000000FL);
    }
    public static int getVelocityPlanEntry  (long plan) {return (int) (plan & 0x00000000000000FFL); }
    public static int getRoutePlanEntry     (long plan) { return (int)((plan >> 16) & 0x000000000000FFFFL); }
    public static int getStopPlanEntry      (long plan) { return (int)( plan        & 0x000000000000FFFFL); }
    public static int getStopIndexPlanEntry (long plan) { return (int)( plan >> 32  & 0x00000000000000FFL); }
    public static int getSleepPlanEntry     (long plan) { return (int)( plan        & 0x00000000FFFFFFFFL); }

    private static void validatePlanEntry(long planEntry) {
        int event = Agent.getPlanEvent(planEntry);
        int type = Agent.getPlanHeader(planEntry);
        switch (type) {
            case Agent.LinkType:
            case Agent.SleepForType:
            case Agent.SleepUntilType:
            case Agent.AccessType:
            case Agent.StopArriveType:
            case Agent.StopDelayType:
            case Agent.StopDepartType:
            case Agent.EgressType:
            case Agent.WaitType:
                break; // TODO - add more verification for each field!
            default:
                throw new RuntimeException("planEntry does not validate " + planEntry);
        }
    }

    public static long preparePlanEventEntry(long type, long eventid, long element) {
        if (eventid > HermesConfigGroup.MAX_EVENTS_AGENT) {
            throw new RuntimeException(String.format("eventid above limit: %d", eventid));
        }
        return preparePlanEventEntry(type, (eventid << 40) | element);
    }

    private static long prepareLinkEntryElement(long linkid, long velocity, long pcecategory) {
        if (linkid > HermesConfigGroup.MAX_LINK_ID) {
            throw new RuntimeException("exceeded maximum number of links");
        }

        // Checking for velocities that are too high.
        velocity = Math.min(velocity, HermesConfigGroup.MAX_VEHICLE_VELOCITY);

        // Checking for velocities that are too low.
        velocity = velocity < 0 ? HermesConfigGroup.MAX_VEHICLE_VELOCITY : velocity;
        return (pcecategory << 56) | (linkid << 8) | velocity;
    }

    public static long prepareStopDelay(long type, long departure, long element) {
        return preparePlanEventEntry(type, (departure << 40) | element);
    }

    private static long prepareRouteStopEntry(long routeid, long stopid, long stopidx) {
        if (stopid > HermesConfigGroup.MAX_STOP_ROUTE_ID) {
            throw new RuntimeException(String.format("stopid above limit: %d", stopid));
        }
        if (routeid > HermesConfigGroup.MAX_STOP_ROUTE_ID) {
            throw new RuntimeException(String.format("routeid above limit: %d", routeid));
        }
        if (stopidx > HermesConfigGroup.MAX_STOP_IDX) {
            throw new RuntimeException(String.format("station index above limit: %d", stopidx));
        }
        return (stopidx << 32) | (routeid << 16) | stopid;
    }

    public void reset() {
        plan.clear();
        events.clear();
        planIndex = 0;
        eventsIndex = 0;
        linkFinishTime = 0;
        if (capacity > 0) {
            passengersInside = 0;
            for (int i = 0; i < HermesConfigGroup.MAX_STOP_IDX + 1; i++) {
                this.passengersByStop.get(i).clear();
            }
        }
    }

    public static long prepareLinkEntry(int eventid, int linkid, int velocity, int pcecategory) {
        long l = preparePlanEventEntry(LinkType, eventid, prepareLinkEntryElement(linkid, velocity, pcecategory));
        return l;
    }

    public static long prepareSleepForEntry(int eventid, int element) {
        return preparePlanEventEntry(SleepForType, eventid, element);
    }

    public static long prepareSleepUntilEntry(int eventid, int element) {
        return preparePlanEventEntry(SleepUntilType, eventid, element);
    }

    public static long prepareAccessEntry(int eventid, int routeid, int stopid) {
        return preparePlanEventEntry(AccessType, eventid, prepareRouteStopEntry(routeid, stopid, 0));
    }

    public static long prepareEgressEntry(int eventid, int routeid, int stopid) {
        return preparePlanEventEntry(EgressType, eventid, prepareRouteStopEntry(routeid, stopid, 0));
    }

    public static long prepareWaitEntry(int eventid, int routeid, int stopid) {
        return preparePlanEventEntry(WaitType, eventid, prepareRouteStopEntry(routeid, stopid, 0));
    }

    public static long prepareStopArrivalEntry(int eventid, int routeid, int stopid, int stopidx) {
        return preparePlanEventEntry(StopArriveType, eventid, prepareRouteStopEntry(routeid, stopid, stopidx));
    }

    public static long prepareStopDelayEntry(int departure, int routeid, int stopid, int stopidx) {
        return prepareStopDelay(StopDelayType, departure, prepareRouteStopEntry(routeid, stopid, stopidx));
    }

    public static long prepareStopDepartureEntry(int eventid, int routeid, int stopid, int stopidx) {
        return preparePlanEventEntry(StopDepartType, eventid, prepareRouteStopEntry(routeid, stopid, stopidx));
    }

    public static String toString(long planEntry) {
        int type = Agent.getPlanHeader(planEntry);
        switch (type) {
            case Agent.LinkType:
                return String.format("type=link; event=%d; link=%d; vel=%d",
            		getPlanEvent(planEntry), getLinkPlanEntry(planEntry), getVelocityPlanEntry(planEntry));
            case Agent.SleepForType:
                return String.format("type=sleepfor; event=%d; sleep=%d",
            		getPlanEvent(planEntry), getSleepPlanEntry(planEntry));
            case Agent.SleepUntilType:
                return String.format("type=sleepuntil; event=%d; sleep=%d",
            		getPlanEvent(planEntry), getSleepPlanEntry(planEntry));
            case Agent.AccessType:
                return String.format("type=access; event=%d; route=%d stopid=%d stopidx=%d",
            		getPlanEvent(planEntry), getRoutePlanEntry(planEntry), getStopPlanEntry(planEntry), getStopIndexPlanEntry(planEntry));
            case Agent.StopArriveType:
                return String.format("type=stoparrive; event=%d; route=%d stopid=%d stopidx=%d",
            		getPlanEvent(planEntry), getRoutePlanEntry(planEntry), getStopPlanEntry(planEntry), getStopIndexPlanEntry(planEntry));
            case Agent.StopDelayType:
                return String.format("type=stopdelay; departure=%d; route=%d stopid=%d stopidx=%d",
            		getDeparture(planEntry), getRoutePlanEntry(planEntry), getStopPlanEntry(planEntry), getStopIndexPlanEntry(planEntry));
            case Agent.StopDepartType:
                return String.format("type=stopdepart; event=%d; route=%d stopid=%d stopidx=%d",
            		getPlanEvent(planEntry), getRoutePlanEntry(planEntry), getStopPlanEntry(planEntry), getStopIndexPlanEntry(planEntry));
            case Agent.EgressType:
                return String.format("type=egress; event=%d; route=%d stopid=%d stopidx=%d",
            		getPlanEvent(planEntry), getRoutePlanEntry(planEntry), getStopPlanEntry(planEntry), getStopIndexPlanEntry(planEntry));
            case Agent.WaitType:
                return String.format("type=wait; event=%d; route=%d stopid=%d stopidx=%d",
            		getPlanEvent(planEntry), getRoutePlanEntry(planEntry), getStopPlanEntry(planEntry), getStopIndexPlanEntry(planEntry));
            default:
                return String.format("unknow plan type %d", type);
        }

    }

}
