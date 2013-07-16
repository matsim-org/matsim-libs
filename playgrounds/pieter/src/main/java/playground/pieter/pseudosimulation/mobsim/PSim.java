/**
 * 
 */
package playground.pieter.pseudosimulation.mobsim;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TravelledEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import playground.pieter.pseudosimulation.util.CollectionUtils;
import playground.sergioo.singapore2012.transitRouterVariable.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.WaitTimeStuckCalculator;

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

	Scenario scenario;
	EventsManager eventManager;

	private final static double MIN_ACT_DURATION = 1.0;

	private final static double MIN_LEG_DURATION = 0.0;

	private SimThread[] threads;

	private Future<?>[] futures;

	private final ExecutorService executor;
	private PSimTravelTimeCalculator carLinkTravelTimeCalculator;
	private PSimControler controler;
	private double beelineWalkSpeed;
	private StopStopTimeCalculator transitStopToStopTimeCalculator;
	private WaitTimeStuckCalculator transitWaitTimeCalculator;
	private Map<Id, TransitLine> transitLines;
	private Map<Id, TransitStopFacility> stopFacilities;

	public PSim(Scenario sc, EventsManager eventsManager) {
		this.scenario = sc;
		this.eventManager = eventsManager;
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

	public PSim(Scenario sc2, EventsManager eventsManager, PSimControler c) {
		this(sc2, eventsManager);
		this.controler = c;
		this.carLinkTravelTimeCalculator = controler.getCarTravelTimeCalculator();
		if (controler.getConfig().scenario().isUseTransit()) {
			transitStopToStopTimeCalculator = controler.getStopStopTimeCalculator();
			transitWaitTimeCalculator = controler.getWaitTimeCalculator();
			transitLines = scenario.getTransitSchedule().getTransitLines();
			stopFacilities = scenario.getTransitSchedule().getFacilities();
		}
	}

	public void run() {

		Collection<Plan> plans = new LinkedHashSet<Plan>();

		plans.addAll(controler.getPlansForPseudoSimulation());

		Logger.getLogger(this.getClass()).error("Executing " + plans.size() + " plans in mental simulation.");

		Network network = scenario.getNetwork();

		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(plans.size(), threads.length);
		List<Plan>[] segments = CollectionUtils.split(plans, n);
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
			Queue<Event> eventQueue = new LinkedList<Event>();
			PLANS: for (Plan plan : plans) {
				Id personId = plan.getPerson().getId();
				List<PlanElement> elements = plan.getPlanElements();

				double prevEndTime = 0;
				LEGS: for (int idx = 0; idx < elements.size(); idx += 2) {
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

								travelTime = calcRouteTravelTime(croute, prevEndTime,
										carLinkTravelTimeCalculator.getLinkTravelTimes(), network, eventQueue, personId);

								eventQueue.add(new PersonLeavesVehicleEvent(prevEndTime + travelTime, personId,
										personId));
							} catch (NullPointerException ne) {
								Logger.getLogger(this.getClass()).error(
										"No route for car leg. Continuing with next leg");
								continue LEGS;
							}
						} else if (prevLeg.getMode().equals(TransportMode.transit_walk)) {
							TransitWalkTimeAndDistance tnd = new TransitWalkTimeAndDistance(act.getCoord(),
									prevAct.getCoord());
							travelTime = tnd.time;
							eventQueue.add(new TravelledEvent(prevEndTime + tnd.time, personId, tnd.distance));
						} else if (prevLeg.getMode().equals(TransportMode.pt)) {
							if (controler.getConfig().scenario().isUseTransit()) {
								ExperimentalTransitRoute route = (ExperimentalTransitRoute) prevLeg.getRoute();
								Id accessStopId = route.getAccessStopId();
								Id egressStopId = route.getEgressStopId();
								Id dummyVehicleId = new IdImpl("dummy");
								TransitLine line = transitLines.get(route.getLineId());
								TransitRoute transitRoute = line.getRoutes().get(route.getRouteId());
								travelTime += transitWaitTimeCalculator.getWaitTimes().getRouteStopWaitTime(line,
										transitRoute, accessStopId, prevEndTime);
								eventQueue.add(new PersonEntersVehicleEvent(prevEndTime + travelTime, personId,
										dummyVehicleId));
								travelTime += findTransitTravelTime(route, prevEndTime + travelTime);
								eventQueue.add(new PersonLeavesVehicleEvent(prevEndTime + travelTime, personId,
										dummyVehicleId));
								eventQueue.add(new TravelledEvent(prevEndTime + travelTime, personId, Double.NaN));
							} else {
								try {
									GenericRoute route = (GenericRoute) prevLeg.getRoute();
									travelTime = route.getTravelTime();
								} catch (NullPointerException e) {
									Logger.getLogger(this.getClass()).error(
											"No route for this leg. Continuing with next leg");
									continue LEGS;
								}
							}
						} else {
							try {
								GenericRoute route = (GenericRoute) prevLeg.getRoute();
								travelTime = route.getTravelTime();
							} catch (NullPointerException e) {
								Logger.getLogger(this.getClass()).error(
										"No route for this leg. Continuing with next leg");
								continue LEGS;
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
							throw new RuntimeException("I think this is discuraged.");
						}
						/*
						 * Send arrival and activity start events.
						 */
						AgentArrivalEvent arrivalEvent = new AgentArrivalEvent(arrivalTime, personId, act.getLinkId(),
								prevLeg.getMode());
						eventQueue.add(arrivalEvent);
						ActivityStartEvent startEvent = new ActivityStartEvent(arrivalTime, personId, act.getLinkId(),
								act.getFacilityId(), act.getType());
						eventQueue.add(startEvent);
					}

					if (idx < elements.size() - 1) {
						/*
						 * This is not the last activity, send activity end and
						 * departure events.
						 */
						Leg nextLeg = (Leg) elements.get(idx + 1);
						ActivityEndEvent endEvent = new ActivityEndEvent(actEndTime, personId, act.getLinkId(),
								act.getFacilityId(), act.getType());
						eventQueue.add(endEvent);
						AgentDepartureEvent departureEvent = new AgentDepartureEvent(actEndTime, personId,
								act.getLinkId(), nextLeg.getMode());

						eventQueue.add(departureEvent);
					}

					prevEndTime = actEndTime;
				}
			}

			for (Event event : eventQueue) {
				eventManager.processEvent(event);
			}
			return new Boolean(true);

		}

		private double findTransitTravelTime(ExperimentalTransitRoute route, double prevEndTime) {
			double travelTime = 0;
			TransitRouteImpl transitRoute = (TransitRouteImpl) transitLines.get(route.getLineId()).getRoutes()
					.get(route.getRouteId());
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
				travelTime += transitStopToStopTimeCalculator.getStopStopTimes().getStopStopTime(fromId, toId,
						prevEndTime);
				prevEndTime += travelTime;
				if (toStop.getStopFacility().getId().equals(dest)) {
					destinationFound = true;
					break;
				}
				i++;
			}
			if (destinationFound) {
				return travelTime;
			} else {

				return Double.NEGATIVE_INFINITY;
			}
		}

		private double calcRouteTravelTime(NetworkRoute route, double startTime, TravelTime travelTime,
				Network network, Queue<Event> eventQueue, Id agentId) {
			if (agentId.toString().equals("141"))
				System.out.println();
			double tt = 0;
			if (route.getStartLinkId() != route.getEndLinkId()) {
				Id startLink = route.getStartLinkId();
				double linkEnterTime = startTime;
				AgentWait2LinkEvent wait2Link = new AgentWait2LinkEvent(linkEnterTime, agentId, startLink, agentId);
				LinkEnterEvent linkEnterEvent = null;
				LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(++linkEnterTime, agentId, startLink, agentId);
				eventQueue.add(wait2Link);
				eventQueue.add(linkLeaveEvent);
				double linkLeaveTime = linkEnterTime;
				List<Id> ids = route.getLinkIds();
				for (int i = 0; i < ids.size(); i++) {
					Id link = ids.get(i);
					linkEnterTime = linkLeaveTime;
					linkEnterEvent = new LinkEnterEvent(linkEnterTime, agentId, link, agentId);
					eventQueue.add(linkEnterEvent);

					double linkTime = travelTime.getLinkTravelTime(network.getLinks().get(link), linkEnterTime, null,
							null);
					tt += Math.max(linkTime, 1.0);

					linkLeaveTime = Math.max(linkEnterTime + 1, linkEnterTime + linkTime);
					linkLeaveEvent = new LinkLeaveEvent(linkLeaveTime, agentId, link, agentId);
					eventQueue.add(linkLeaveEvent);
				}
				tt = linkLeaveTime - startTime;
			}
			LinkEnterEvent linkEnterEvent = new LinkEnterEvent(startTime + tt, agentId, route.getEndLinkId(), agentId);
			eventQueue.add(linkEnterEvent);
			return tt
					+ travelTime.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()), tt + startTime, null,
							null);
		}

		private double calcRouteTravelTime(NetworkRoute route, double startTime, TravelTime travelTime, Network network) {
			double tt = 0;
			if (route.getStartLinkId() != route.getEndLinkId()) {

				List<Id> ids = route.getLinkIds();
				for (int i = 0; i < ids.size(); i++) {
					tt += travelTime.getLinkTravelTime(network.getLinks().get(ids.get(i)), startTime, null, null);
					tt++;// 1 sec for each node
				}
				tt += travelTime.getLinkTravelTime(network.getLinks().get(route.getEndLinkId()), startTime, null, null);
			}

			return tt;
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
