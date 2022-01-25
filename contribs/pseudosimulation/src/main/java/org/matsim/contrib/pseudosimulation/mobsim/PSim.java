/**
 *
 */
package org.matsim.contrib.pseudosimulation.mobsim;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
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
import org.matsim.contrib.pseudosimulation.util.CollectionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.vehicles.Vehicle;

/**
 * @author illenberger
 *         <p></p>
 *         Original car mode code and multi-threading by Johannes Illenberger
 *         for his social network research.
 * @author fouriep
 *         <p></p>
 *         Extended to produce link events, making it compatible with road
 *         pricing.
 * @author fouriep, sergioo
 *         <p></p>
 *         Extended for transit simulation.
 */
public class PSim implements Mobsim {

    private final Scenario scenario;
    private final EventsManager eventManager;

    private final static double MIN_ACT_DURATION = 1.0;

    private final static double MIN_LEG_DURATION = 0.0;

    private final SimThread[] threads;
    AtomicInteger numThreads;

    private final TravelTime carLinkTravelTimes;
    private final Collection<Plan> plans;
    private final double endTime;
    
    // Encapsulates TransitPerformance, WaitTime, StopStopTime, ...
    private TransitEmulator transitEmulator = null;
    private Set<String> transitModes = new LinkedHashSet<>();
    
    public PSim(Scenario sc, EventsManager eventsManager, Collection<Plan> plans, TravelTime carLinkTravelTimes) {
        Logger.getLogger(getClass()).warn("Constructing PSim");
        this.scenario = sc;
        this.endTime = sc.getConfig().qsim().getEndTime().seconds();
        this.eventManager = eventsManager;
        int numThreads = sc.getConfig().global().getNumberOfThreads() ;
        threads = new SimThread[numThreads];
        for (int i = 0; i < numThreads; i++)
            threads[i] = new SimThread();

        this.carLinkTravelTimes = carLinkTravelTimes;
        this.plans = plans;
    }

    public PSim(Scenario sc, EventsManager eventsManager, Collection<Plan> plans, TravelTime carLinkTravelTimes, TransitEmulator transitEmulator) {
        this(sc, eventsManager, plans, carLinkTravelTimes);
        this.transitEmulator = transitEmulator;
        this.transitModes = ConfigUtils.addOrGetModule(sc.getConfig(), TransitConfigGroup.class).getTransitModes();
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
            // plans:
            for( Plan plan : threadPlans ){
                Queue<Event> eventQueue = new LinkedList<>();
                Id<Person> personId = plan.getPerson().getId();
                Id<Vehicle> personVehicleId = Id.createVehicleId( personId.toString() ); // TODO: find cleaner access to vehicle id
                List<PlanElement> elements = plan.getPlanElements();

                double prevEndTime = 0;

                // legs:
                for( int idx = 0 ; idx < elements.size() ; idx += 2 ){
                    Activity act = (Activity) elements.get( idx );
                    /*
                     * Make sure that the activity does not end before the previous activity.
                     */
                    double actEndTime = Math.max( prevEndTime + MIN_ACT_DURATION, act.getEndTime().orElse(0));
                    if( idx > 0 ){
                        /*
                         * If this is not the first activity, then there must exist a leg before.
                         */

                        Leg prevLeg = (Leg) elements.get( idx - 1 );
                        double travelTime = 0.0;
                        if( prevLeg.getMode().equals( TransportMode.car ) ){
                            try{
                                eventQueue.add( new PersonEntersVehicleEvent( prevEndTime, personId, personVehicleId ) );
                                eventQueue.add( new VehicleEntersTrafficEvent( prevEndTime, personId, prevLeg.getRoute().getStartLinkId(), personVehicleId,
                                        TransportMode.car, 1.0 ) );
                                NetworkRoute croute = (NetworkRoute) prevLeg.getRoute();

                                travelTime = calcRouteTravelTime( croute, prevEndTime, carLinkTravelTimes, network, eventQueue, personVehicleId );
                                eventQueue.add(
                                        new VehicleLeavesTrafficEvent( prevEndTime + travelTime, personId, prevLeg.getRoute().getEndLinkId(), personVehicleId,
                                                TransportMode.car, 1.0 ) );
                                eventQueue.add( new PersonLeavesVehicleEvent( prevEndTime + travelTime, personId, personVehicleId ) );
                            } catch( NullPointerException ne ){
                                Logger.getLogger( this.getClass() ).error( "No route for car leg. Continuing with next leg" );
                                continue;
                            }
                        } else if( transitModes.contains( prevLeg.getMode() ) ){
                            TransitEmulator.Trip trip = transitEmulator.findTrip( prevLeg, prevEndTime );
                            if( trip != null ){

                                Id<Vehicle> vehicleId = trip.vehicleId();
                                if( vehicleId == null ){
                                    vehicleId = Id.create( "dummy", Vehicle.class );
                                }
                                eventQueue.add( new PersonEntersVehicleEvent( trip.accessTime_s(), personId, vehicleId ) ); // dummyVehicleId));
                                eventQueue.add( new PersonLeavesVehicleEvent( trip.egressTime_s(), personId, vehicleId ) ); // dummyVehicleId));
                                travelTime = trip.egressTime_s() - prevEndTime;
                            }
                        } else{
                                Route route = prevLeg.getRoute();
                                if (route == null) {
                                    Logger.getLogger( this.getClass() ).error( "No route for this leg. Continuing with next leg" );
                                    continue;
                                }

                                travelTime = route.getTravelTime().orElse(0);
                                eventQueue.add( new TeleportationArrivalEvent( prevEndTime + travelTime, personId,
                                        route.getDistance()
                                        , prevLeg.getMode()
                                ) );
                        }

                        travelTime = Math.max( MIN_LEG_DURATION, travelTime );
                        double arrivalTime = travelTime + prevEndTime;

                        /*
                         * Make sure that the activity does not end before the
                         * agent arrives.
                         */
                        actEndTime = Math.max( arrivalTime + MIN_ACT_DURATION, actEndTime );
                        /*
                         * Send arrival and activity start events.
                         */
                        PersonArrivalEvent arrivalEvent = new PersonArrivalEvent( arrivalTime, personId, act.getLinkId(), prevLeg.getMode() );
                        eventQueue.add( arrivalEvent );
                        ActivityStartEvent startEvent = new ActivityStartEvent( arrivalTime, personId, act.getLinkId(), act.getFacilityId(), act.getType() );
                        eventQueue.add( startEvent );
                    }

                    if( idx < elements.size() - 1 ){
                        /*
                         * This is not the last activity, send activity end and
                         * departure events.
                         */
                        Leg nextLeg = (Leg) elements.get( idx + 1 );
                        ActivityEndEvent endEvent = new ActivityEndEvent( actEndTime, personId, act.getLinkId(), act.getFacilityId(), act.getType() );
                        eventQueue.add( endEvent );
                        PersonDepartureEvent departureEvent = new PersonDepartureEvent( actEndTime, personId, act.getLinkId(), nextLeg.getMode(), TripStructureUtils.getRoutingMode(nextLeg) );

                        eventQueue.add( departureEvent );
                    }

                    prevEndTime = actEndTime;
                }
                for( Event event : eventQueue ){
                    if( event.getTime() > endTime ){
                        eventManager.processEvent( new PersonStuckEvent( endTime, personId, null, null ) );
                        break;
                    }
                    eventManager.processEvent( event );
                }
            }

            numThreads.decrementAndGet();
        }

        private double calcRouteTravelTime(NetworkRoute route, double startTime, TravelTime travelTime, Network network, Queue<Event> eventQueue, Id<Vehicle> personVehicleId) {

            double tt = 0;
            if (route.getStartLinkId() != route.getEndLinkId()) {
                Id<Link> startLink = route.getStartLinkId();
                double linkEnterTime = startTime;
                LinkEnterEvent linkEnterEvent = null;
                LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(++linkEnterTime, personVehicleId, startLink);
                eventQueue.add(linkLeaveEvent);
                double linkLeaveTime = linkEnterTime;
                List<Id<Link>> routeLinkIds = route.getLinkIds();
                for (Id<Link> routeLinkId : routeLinkIds) {
                    linkEnterTime = linkLeaveTime;
                    linkEnterEvent = new LinkEnterEvent(linkEnterTime, personVehicleId, routeLinkId);
                    eventQueue.add(linkEnterEvent);

                    double linkTime = travelTime.getLinkTravelTime(network.getLinks().get(routeLinkId), linkEnterTime, null, null);
                    tt += Math.max(linkTime, 1.0);

                    linkLeaveTime = Math.max(linkEnterTime + 1, linkEnterTime + linkTime);
                    linkLeaveEvent = new LinkLeaveEvent(linkLeaveTime, personVehicleId, routeLinkId);
                    eventQueue.add(linkLeaveEvent);
                }
                tt = linkLeaveTime - startTime;
            }
            LinkEnterEvent linkEnterEvent = new LinkEnterEvent(startTime + tt, personVehicleId, route.getEndLinkId());
            eventQueue.add(linkEnterEvent);
            return tt + travelTime.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()), tt + startTime, null, null);
        }
    }
}
