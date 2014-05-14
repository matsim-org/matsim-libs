/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PSim.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

/**
 *
 */
package playground.mzilske.populationsize;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author illenberger
 *         <p>
 *         Original car mode code and multi-threading by Johannes Illenberger
 *         for his social network research.
 * @author fouriep
 *         <p>
 *         Extended to produce link events, making it compatible with road
 *         pricing.
 * @author fouriep, sergioo
 *         <P>
 *         Extended for transit simulation.
 *
 */
public class PSim implements Mobsim {

    private final TravelTime travelTimes;
    Scenario scenario;
    EventsManager eventManager;

    private final static double MIN_ACT_DURATION = 1.0;

    private final static double MIN_LEG_DURATION = 0.0;

    private SimThread[] threads;

    private Future<?>[] futures;

    private final ExecutorService executor;
    private double beelineWalkSpeed;

    public PSim(Scenario sc, EventsManager eventsManager, TravelTime travelTime) {
        this.scenario = sc;
        this.eventManager = eventsManager;
        this.travelTimes = travelTime;
        int numThreads = Integer.parseInt(sc.getConfig().getParam("global", "numberOfThreads"));
        executor = Executors.newFixedThreadPool(numThreads);
        threads = new SimThread[numThreads];
        for (int i = 0; i < numThreads; i++)
            threads[i] = new SimThread();

        futures = new Future[numThreads];

        PlansCalcRouteConfigGroup pcrConfig = sc.getConfig().plansCalcRoute();
        this.beelineWalkSpeed = pcrConfig.getTeleportedModeSpeeds().get(TransportMode.walk)
                / pcrConfig.getBeelineDistanceFactor();
    }

    public void run() {

        Collection<Plan> plans = new LinkedHashSet<Plan>();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            plans.add(person.getSelectedPlan());
        }

        Logger.getLogger(this.getClass()).error("Executing " + plans.size() + " plans in mental simulation.");

        Network network = scenario.getNetwork();

		/*
		 * split collection in approx even segments
		 */
        int n = Math.min(plans.size(), threads.length);
        List<Plan>[] segments = split(plans, n);
		/*
		 * submit tasks
		 */
        for (int i = 0; i < segments.length; i++) {
            threads[i].init(segments[i], network, eventManager);
            futures[i] = executor.submit(threads[i]);
        }
		/*
		 * wait for threads
		 */
        for (int i = 0; i < segments.length; i++) {
            try {
                futures[i].get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static <T> List<T>[] split(Collection<T> set, int n) {
        if(set.size() >= n) {
            @SuppressWarnings("unchecked")
            List<T>[] arrays = new List[n];
            int minSegmentSize = (int) Math.floor(set.size()/(double)n);

            int start = 0;
            int stop = minSegmentSize;

            Iterator<T> it = set.iterator();

            for(int i = 0; i < n - 1; i++) {
                int segmentSize = stop - start;
                List<T> segment = new ArrayList<T>(segmentSize);
                for(int k = 0; k < segmentSize; k++) {
                    segment.add(it.next());
                }
                arrays[i] = segment;
                start = stop;
                stop += segmentSize;
            }

            int segmentSize = set.size() - start;
            List<T> segment = new ArrayList<T>(segmentSize);
            for(int k = 0; k < segmentSize; k++) {
                segment.add(it.next());
            }
            arrays[n - 1] = segment;

            return arrays;
        } else {
            throw new IllegalArgumentException("n must not be smaller set size!");
        }
    }

    public class SimThread implements Callable {

        private Collection<Plan> plans;

        private EventsManager eventManager;

        private Network network;

        public void init(Collection<Plan> plans, Network network, EventsManager eventManager) {
            this.plans = plans;
            this.network = network;
            this.eventManager = eventManager;
        }

        @Override
        public Boolean call() {
            for (Plan plan : plans) {
                Id personId = plan.getPerson().getId();
                List<PlanElement> elements = plan.getPlanElements();

                double prevEndTime = 0;
                for (int idx = 0; idx < elements.size(); idx += 2) {
                    Activity act = (Activity) elements.get(idx);
                    /*
                     * Make sure that the activity does not end before the
					 * previous activity.
					 */
                    double actEndTime = Math.max(prevEndTime + MIN_ACT_DURATION, act.getEndTime());

                    if (idx > 0) {
						/*
						 * If this is not the first activity, then there must
						 * exist a leg before.
						 */

                        Leg prevLeg = (Leg) elements.get(idx - 1);
                        Activity prevAct = (Activity) elements.get(idx - 2);
                        double travelTime;
                        if (prevLeg.getMode().equals(TransportMode.car)) {
                            eventManager.processEvent(new PersonEntersVehicleEvent(prevEndTime, personId, personId));
                            NetworkRoute croute = (NetworkRoute) prevLeg.getRoute();
                            travelTime = calcRouteTravelTime(croute, prevEndTime, travelTimes, network, personId);
                            eventManager.processEvent(new PersonLeavesVehicleEvent(prevEndTime + travelTime, personId, personId));
                        } else if (prevLeg.getMode().equals(TransportMode.transit_walk)) {
                            TransitWalkTimeAndDistance tnd = new TransitWalkTimeAndDistance(act.getCoord(), prevAct.getCoord());
                            travelTime = tnd.time;
                            eventManager.processEvent(new TeleportationArrivalEvent(prevEndTime + tnd.time, personId, tnd.distance));
                        } else {
                            GenericRoute route = (GenericRoute) prevLeg.getRoute();
                            travelTime = route.getTravelTime();
                        }

                        travelTime = Math.max(MIN_LEG_DURATION, travelTime);
                        double arrivalTime = travelTime + prevEndTime;

						/*
						 * Make sure that the activity does not end before the
						 * agent arrives.
						 */
                        actEndTime = Math.max(arrivalTime + MIN_ACT_DURATION, actEndTime);
						/*
						 * If act end time is not specified...
						 */
                        if (Double.isInfinite(actEndTime)) {
                            throw new RuntimeException("I think this is discuraged.");
                        }
						/*
						 * Send arrival and activity start events.
						 */
                        PersonArrivalEvent arrivalEvent = new PersonArrivalEvent(arrivalTime, personId, act.getLinkId(),
                                prevLeg.getMode());
                        eventManager.processEvent(arrivalEvent);
                        ActivityStartEvent startEvent = new ActivityStartEvent(arrivalTime, personId, act.getLinkId(),
                                act.getFacilityId(), act.getType());
                        eventManager.processEvent(startEvent);
                    }

                    if (idx < elements.size() - 1) {
						/*
						 * This is not the last activity, send activity end and
						 * departure events.
						 */
                        Leg nextLeg = (Leg) elements.get(idx + 1);
                        ActivityEndEvent endEvent = new ActivityEndEvent(actEndTime, personId, act.getLinkId(),
                                act.getFacilityId(), act.getType());
                        eventManager.processEvent(endEvent);
                        PersonDepartureEvent departureEvent = new PersonDepartureEvent(actEndTime, personId,
                                act.getLinkId(), nextLeg.getMode());

                        eventManager.processEvent(departureEvent);
                    }

                    prevEndTime = actEndTime;
                }
            }

            return true;

        }


        private double calcRouteTravelTime(NetworkRoute route, double startTime, TravelTime travelTime, Network network, Id agentId) {
            if (agentId.toString().equals("141"))
                System.out.println();
            double tt = 0;
            if (route.getStartLinkId() != route.getEndLinkId()) {
                Id startLink = route.getStartLinkId();
                double linkEnterTime = startTime;
                Wait2LinkEvent wait2Link = new Wait2LinkEvent(linkEnterTime, agentId, startLink, agentId);
                LinkEnterEvent linkEnterEvent;
                LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(++linkEnterTime, agentId, startLink, agentId);
                eventManager.processEvent(wait2Link);
                eventManager.processEvent(linkLeaveEvent);
                double linkLeaveTime = linkEnterTime;
                List<Id> ids = route.getLinkIds();
                for (Id link : ids) {
                    linkEnterTime = linkLeaveTime;
                    linkEnterEvent = new LinkEnterEvent(linkEnterTime, agentId, link, agentId);
                    eventManager.processEvent(linkEnterEvent);

                    double linkTime = travelTime.getLinkTravelTime(network.getLinks().get(link), linkEnterTime, null,
                            null);
                    tt += Math.max(linkTime, 1.0);

                    linkLeaveTime = Math.max(linkEnterTime + 1, linkEnterTime + linkTime);
                    linkLeaveEvent = new LinkLeaveEvent(linkLeaveTime, agentId, link, agentId);
                    eventManager.processEvent(linkLeaveEvent);
                }
                tt = linkLeaveTime - startTime;
            }
            LinkEnterEvent linkEnterEvent = new LinkEnterEvent(startTime + tt, agentId, route.getEndLinkId(), agentId);
            eventManager.processEvent(linkEnterEvent);
            return tt
                    + travelTime.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()), tt + startTime, null,
                    null);
        }

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        executor.shutdown();
    }

    class TransitWalkTimeAndDistance {
        double time;
        double distance;

        public TransitWalkTimeAndDistance(Coord startCoord, Coord endCoord) {
            distance = CoordUtils.calcDistance(startCoord, endCoord);
            time = distance / beelineWalkSpeed;
        }

    }
}
