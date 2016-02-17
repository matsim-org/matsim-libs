/**
 *
 */
package org.matsim.contrib.pseudosimulation.mobsim;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.contrib.pseudosimulation.util.CollectionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author illenberger
 *         <p/>
 *         Original car mode code and multi-threading by Johannes Illenberger
 *         for his social network research.
 * @author fouriep
 *         <p/>
 *         Extended to produce link events, making it compatible with road
 *         pricing.
 * @author fouriep, sergioo
 *         <p/>
 *         Extended for transit simulation.
 */
public class PSim implements Mobsim {

    private final Scenario scenario;
    private final EventsManager eventManager;

    private final static double MIN_ACT_DURATION = 1.0;

    private final static double MIN_LEG_DURATION = 0.0;

    private final SimThread[] threads;
    private final double beelineDistanceFactor;
    private TransitPerformance transitPerformance;
    private boolean isUseTransit;
    AtomicInteger numThreads;

    private final double walkSpeed;

    private final TravelTime carLinkTravelTimes;
    private WaitTime waitTimes;
    private StopStopTime stopStopTimes;
    private Map<Id<TransitLine>, TransitLine> transitLines;
    private Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities;
    private final Collection<Plan> plans;
    private final double endTime;

    public PSim(Scenario sc, EventsManager eventsManager, Collection<Plan> plans, TravelTime carLinkTravelTimes) {
        Logger.getLogger(getClass()).warn("Constructing PSim");
        this.scenario = sc;
        this.endTime = sc.getConfig().qsim().getEndTime();
        this.eventManager = eventsManager;
        int numThreads = Integer.parseInt(sc.getConfig().getParam("global", "numberOfThreads"));
        threads = new SimThread[numThreads];
        for (int i = 0; i < numThreads; i++)
            threads[i] = new SimThread();


        PlansCalcRouteConfigGroup pcrConfig = sc.getConfig().plansCalcRoute();
        this.beelineDistanceFactor = pcrConfig.getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
        this.walkSpeed = pcrConfig.getTeleportedModeSpeeds().get(TransportMode.walk) ;
        this.carLinkTravelTimes = carLinkTravelTimes;
        this.plans = plans;
    }

    public PSim(Scenario sc, EventsManager eventsManager, Collection<Plan> plans, TravelTime carLinkTravelTimes, WaitTime waitTimes, StopStopTime stopStopTimes, TransitPerformance transitPerformance) {
        this(sc, eventsManager, plans, carLinkTravelTimes);
        isUseTransit = true;
        this.waitTimes = waitTimes;
        this.stopStopTimes = stopStopTimes;
        this.transitPerformance = transitPerformance;

        transitLines = scenario.getTransitSchedule().getTransitLines();
        stopFacilities = scenario.getTransitSchedule().getFacilities();

    }

    @Override
    public void run() {

        Logger.getLogger(this.getClass()).error("Executing " + plans.size() + " plans in pseudosimulation.");

        Network network = scenario.getNetwork();

		/*
         * split collection in approx even segments
		 */
        int n = Math.min(plans.size(), threads.length);
        List<Plan>[] segments = CollectionUtils.split(plans, n);
		/*
		 * submit tasks
		 */
        numThreads = new AtomicInteger(threads.length);
        for (int i = 0; i < segments.length; i++) {
            threads[i].init(segments[i], network, eventManager);
            new Thread(threads[i]).start();
        }
		/*
		 * wait for threads
		 */
        while (numThreads.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class SimThread implements Runnable {

        private Collection<Plan> threadPlans;

        private EventsManager eventManager;

        private Network network;

        public void init(Collection<Plan> plans, Network network, EventsManager eventManager) {
            this.threadPlans = plans;
            this.network = network;
            this.eventManager = eventManager;
        }

        @Override
        public void run() {
            PLANS:
            for (Plan plan : threadPlans) {
                Queue<Event> eventQueue = new LinkedList<>();
                Id personId = plan.getPerson().getId();
                List<PlanElement> elements = plan.getPlanElements();

                double prevEndTime = 0;
                LEGS:
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
                        double travelTime = 0.0;
                        if (prevLeg.getMode().equals(TransportMode.car)) {
                            try {
                                eventQueue.add(new PersonEntersVehicleEvent(prevEndTime, personId, personId));
                                NetworkRoute croute = (NetworkRoute) prevLeg.getRoute();

                                travelTime = calcRouteTravelTime(croute, prevEndTime, carLinkTravelTimes, network, eventQueue, personId);

                                eventQueue.add(new PersonLeavesVehicleEvent(prevEndTime + travelTime, personId, personId));
                            } catch (NullPointerException ne) {
                                Logger.getLogger(this.getClass()).error("No route for car leg. Continuing with next leg");
                                continue;
                            }
                        } else if (prevLeg.getMode().equals(TransportMode.transit_walk)) {
                            TransitWalkTimeAndDistance tnd = new TransitWalkTimeAndDistance(act.getCoord(), prevAct.getCoord());
                            travelTime = tnd.time;
                            eventQueue.add(new TeleportationArrivalEvent(prevEndTime + tnd.time, personId, tnd.distance));
                        } else if (prevLeg.getMode().equals(TransportMode.pt)) {
                            if (isUseTransit) {
                                ExperimentalTransitRoute route = (ExperimentalTransitRoute) prevLeg.getRoute();
                                Id accessStopId = route.getAccessStopId();
                                Id egressStopId = route.getEgressStopId();
                                Id dummyVehicleId = Id.create("dummy", TransitVehicle.class);
                                TransitLine line = transitLines.get(route.getLineId());
                                TransitRoute transitRoute = line.getRoutes().get(route.getRouteId());
                                if (transitPerformance == null) {
                                    travelTime += waitTimes.getRouteStopWaitTime(line.getId(), transitRoute.getId(), accessStopId, prevEndTime);
                                    eventQueue.add(new PersonEntersVehicleEvent(prevEndTime + travelTime, personId, dummyVehicleId));
                                    travelTime += findTransitTravelTime(route, prevEndTime + travelTime);
                                    eventQueue.add(new PersonLeavesVehicleEvent(prevEndTime + travelTime, personId, dummyVehicleId));
                                } else {
                                    Tuple<Double, Double> routeTravelTime = transitPerformance.getRouteTravelTime(line.getId(), route.getRouteId(), accessStopId, egressStopId, prevEndTime);
                                    travelTime += routeTravelTime.getFirst();
                                    eventQueue.add(new PersonEntersVehicleEvent(prevEndTime + travelTime, personId, dummyVehicleId));
                                    travelTime += routeTravelTime.getSecond();
                                    eventQueue.add(new PersonLeavesVehicleEvent(prevEndTime + travelTime, personId, dummyVehicleId));
                                }
                            }
                        } else {
                            try {
                                Route route = prevLeg.getRoute();
                                travelTime = route.getTravelTime();
                                eventQueue.add(new TeleportationArrivalEvent(prevEndTime + travelTime, personId, Double.NaN));
                            } catch (NullPointerException e) {
                                Logger.getLogger(this.getClass()).error("No route for this leg. Continuing with next leg");
                                continue;
                            }
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
                            if(transitPerformance!=null){
                                //this guy is stuck, will be caught in events handling outside the loop
                                break;
                            }else
                                throw new RuntimeException("I think this is discuraged.");
                        }
						/*
						 * Send arrival and activity start events.
						 */
                        PersonArrivalEvent arrivalEvent = new PersonArrivalEvent(arrivalTime, personId, act.getLinkId(), prevLeg.getMode());
                        eventQueue.add(arrivalEvent);
                        ActivityStartEvent startEvent = new ActivityStartEvent(arrivalTime, personId, act.getLinkId(), act.getFacilityId(), act.getType());
                        eventQueue.add(startEvent);
                    }

                    if (idx < elements.size() - 1) {
						/*
						 * This is not the last activity, send activity end and
						 * departure events.
						 */
                        Leg nextLeg = (Leg) elements.get(idx + 1);
                        ActivityEndEvent endEvent = new ActivityEndEvent(actEndTime, personId, act.getLinkId(), act.getFacilityId(), act.getType());
                        eventQueue.add(endEvent);
                        PersonDepartureEvent departureEvent = new PersonDepartureEvent(actEndTime, personId, act.getLinkId(), nextLeg.getMode());

                        eventQueue.add(departureEvent);
                    }

                    prevEndTime = actEndTime;
                }
                for (Event event : eventQueue) {
                    if (event.getTime() > endTime) {
                        eventManager.processEvent(new PersonStuckEvent(endTime, personId, null, null));
                        break;
                    }
                    eventManager.processEvent(event);
                }
            }

            numThreads.decrementAndGet();
        }

        private double findTransitTravelTime(ExperimentalTransitRoute route, double prevEndTime) {
            double travelTime = 0;
            double prevStopTime = prevEndTime;
            TransitRouteImpl transitRoute = (TransitRouteImpl) transitLines.get(route.getLineId()).getRoutes().get(route.getRouteId());
            // cannot just get the indices of the two transitstops, because
            // sometimes routes visit the same stop facility more than once
            // and the transitroutestop is different for each time it stops at
            // the same facility
            TransitRouteStop orig = transitRoute.getStop(stopFacilities.get(route.getAccessStopId()));
            Id dest = route.getEgressStopId();
            int i = transitRoute.getStops().indexOf(orig);
            // int j = transitRoute.getStops().indexOf(dest);
            // if(i>=j){
            // throw new
            // RuntimeException(String.format("Cannot route from origin stop %s to destination stop %s on route %s.",
            // orig.getStopFacility().getId().toString()
            // ,dest.getStopFacility().getId().toString()
            // ,route.getRouteId().toString()
            // ));
            //
            // }
            boolean destinationFound = false;
            while (i < transitRoute.getStops().size() - 1) {
                Id fromId = transitRoute.getStops().get(i).getStopFacility().getId();
                TransitRouteStop toStop = transitRoute.getStops().get(i + 1);
                Id toId = toStop.getStopFacility().getId();
                travelTime += stopStopTimes.getStopStopTime(fromId, toId, prevStopTime);
                prevStopTime += travelTime;
                if (toStop.getStopFacility().getId().equals(dest)) {
                    destinationFound = true;
                    break;
                }
                if (toStop.getStopFacility().getId().equals(route.getAccessStopId())) {
                    //this is a repeating stop, for routes that loop on themselves more than once
                    travelTime = 0;
                    prevStopTime = prevEndTime;
                }
                i++;
            }
            if (destinationFound) {
                return travelTime;
            } else {

                return Double.NEGATIVE_INFINITY;
            }
        }

        private double calcRouteTravelTime(NetworkRoute route, double startTime, TravelTime travelTime, Network network, Queue<Event> eventQueue, Id agentId) {
            if (agentId.toString().equals("141"))
                System.out.println();
            double tt = 0;
            if (route.getStartLinkId() != route.getEndLinkId()) {
                Id<Link> startLink = route.getStartLinkId();
                double linkEnterTime = startTime;
                VehicleEntersTrafficEvent wait2Link = new VehicleEntersTrafficEvent(linkEnterTime, agentId, startLink, agentId, PtConstants.NETWORK_MODE, 1.0);
                LinkEnterEvent linkEnterEvent = null;
                LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(++linkEnterTime, agentId, startLink);
                eventQueue.add(wait2Link);
                eventQueue.add(linkLeaveEvent);
                double linkLeaveTime = linkEnterTime;
                List<Id<Link>> routeLinkIds = route.getLinkIds();
                for (Id<Link> routeLinkId : routeLinkIds) {
                    if (linkEnterTime > 1E16) {
                        int mmm = 0;
                    }
                    linkEnterTime = linkLeaveTime;
                    linkEnterEvent = new LinkEnterEvent(linkEnterTime, agentId, routeLinkId);
                    eventQueue.add(linkEnterEvent);

                    double linkTime = travelTime.getLinkTravelTime(network.getLinks().get(routeLinkId), linkEnterTime, null, null);
                    tt += Math.max(linkTime, 1.0);

                    linkLeaveTime = Math.max(linkEnterTime + 1, linkEnterTime + linkTime);
                    linkLeaveEvent = new LinkLeaveEvent(linkLeaveTime, agentId, routeLinkId);
                    eventQueue.add(linkLeaveEvent);
                }
                tt = linkLeaveTime - startTime;
            }
            LinkEnterEvent linkEnterEvent = new LinkEnterEvent(startTime + tt, agentId, route.getEndLinkId());
            eventQueue.add(linkEnterEvent);
            return tt + travelTime.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()), tt + startTime, null, null);
        }

        private double calcRouteTravelTime(NetworkRoute route, double startTime, TravelTime travelTime, Network network) {
            double tt = 0;
            if (route.getStartLinkId() != route.getEndLinkId()) {

                List<Id<Link>> ids = route.getLinkIds();
                for (Id<Link> id : ids) {
                    tt += travelTime.getLinkTravelTime(network.getLinks().get(id), startTime, null, null);
                    tt++;// 1 sec for each node
                }
                tt += travelTime.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()), startTime, null, null);
            }

            return tt;
        }
    }


    class TransitWalkTimeAndDistance {
        final double time;
        final double distance;

        public TransitWalkTimeAndDistance(Coord startCoord, Coord endCoord) {
            distance = beelineDistanceFactor * CoordUtils.calcEuclideanDistance(startCoord, endCoord);
            time = distance / walkSpeed;
        }

    }
}
