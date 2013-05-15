/**
 * 
 */
package playground.pieter.pseudosim.mobsim;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
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
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.util.CollectionUtils;

/**
 * @author illenberger, fouriep
 *         <p>
 *         Original code by Johannes Illenberger for his social network
 *         research, extended to produce link events, making it compatible with
 *         road pricing. Currently, transit will only generate activity start
 *         and end events, with travel times taken from the generic route
 *         object.
 * 
 */
public class PseudoSim implements Mobsim {

	Scenario sc;
	EventsManager eventManager;

	private final static double MIN_ACT_DURATION = 1.0;

	private final static double MIN_LEG_DURATION = 0.0;

	private SimThread[] threads;

	private Future<?>[] futures;

	private final ExecutorService executor;
	private TravelTime travelTimeCalculator;
	private boolean linkEvents = false;
	private PseudoSimControler controler;

	public PseudoSim(Scenario sc, EventsManager eventsManager) {
		this.sc = sc;
		this.eventManager = eventsManager;
		int numThreads = Integer.parseInt(sc.getConfig().getParam("global",
				"numberOfThreads"));
		linkEvents = Boolean.parseBoolean(sc.getConfig().getParam("MentalSim",
				"linkEvents"));
		executor = Executors.newFixedThreadPool(numThreads);

		threads = new SimThread[numThreads];
		for (int i = 0; i < numThreads; i++)
			threads[i] = new SimThread();

		futures = new Future[numThreads];
	}

	public PseudoSim(Scenario sc2, EventsManager eventsManager,
			TravelTime ttcalc, PseudoSimControler c) {
		this(sc2, eventsManager);
		this.travelTimeCalculator = ttcalc;
		this.controler = c;
	}

	public void run() {

		Collection<Plan> plans = new LinkedHashSet<Plan>();

		if (controler.isSimulateSubsetPersonsOnly()) {
			for (Person p : sc.getPopulation().getPersons().values()) {
				if (controler.getAgentPlansMarkedForSubsetMentalSim().getAttribute(
						p.getId().toString(), controler.AGENT_ATT) != null) {
					plans.add(p.getSelectedPlan());
				}
			}
		} else {

			// if (p.getSelectedPlan().getScore() == 0.0) {

			plans.addAll(controler.getPlansForMentalSimulation());
			// }
		}

		Logger.getLogger(this.getClass()).error(
				"Executing " + plans.size() + " plans in mental simulation.");

		Network network = sc.getNetwork();

		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(plans.size(), threads.length);
		List<Plan>[] segments = CollectionUtils.split(plans, n);
		/*
		 * submit tasks
		 */
		for (int i = 0; i < segments.length; i++) {
			threads[i].init(segments[i], network, travelTimeCalculator,
					eventManager);
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

		private TravelTime linkTravelTimes;

		private EventsManager eventManager;

		private Network network;

		public void init(Collection<Plan> plans, Network network,
				TravelTime linkTravelTimes, EventsManager eventManager) {
			this.plans = plans;
			this.network = network;
			this.linkTravelTimes = linkTravelTimes;
			this.eventManager = eventManager;
		}

		@Override
		public Boolean call() {
			Queue<Event> eventQueue = new LinkedList<Event>();

			for (Plan plan : plans) {
				List<PlanElement> elements = plan.getPlanElements();

				double prevEndTime = 0;
				for (int idx = 0; idx < elements.size(); idx += 2) {
					Activity act = (Activity) elements.get(idx);
					/*
					 * Make sure that the activity does not end before the
					 * previous activity.
					 */
					double actEndTime = Math.max(
							prevEndTime + MIN_ACT_DURATION, act.getEndTime());

					if (idx > 0) {
						/*
						 * If this is not the first activity, then there must
						 * exist a leg before.
						 */

						Leg prevLeg = (Leg) elements.get(idx - 1);
						double travelTime = 0.0;
						if (prevLeg.getMode().equals(TransportMode.car)) {
							try {
								NetworkRoute croute = (NetworkRoute) prevLeg
										.getRoute();
								if (linkEvents) {

									travelTime = calcRouteTravelTime(croute,
											prevEndTime, linkTravelTimes,
											network, eventQueue, plan
													.getPerson().getId());
								} else {
									travelTime = calcRouteTravelTime(croute,
											prevEndTime, linkTravelTimes,
											network);

								}

							} catch (NullPointerException ne) {
								Logger.getLogger(this.getClass())
										.error("No route for car plan. Continuing with next plan");
								continue;
							}
						} else {
							try {
								GenericRoute route = (GenericRoute) prevLeg
										.getRoute();
								travelTime = route.getTravelTime();
							} catch (NullPointerException e) {
								Logger.getLogger(this.getClass())
										.error("No route for pt plan. Continuing with next plan");
								continue;
							}
						}

						travelTime = Math.max(MIN_LEG_DURATION, travelTime);
						double arrivalTime = travelTime + prevEndTime;

						/*
						 * Make sure that the activity does not end before the
						 * agent arrives.
						 */
						actEndTime = Math.max(arrivalTime + MIN_ACT_DURATION,
								actEndTime);
						/*
						 * If act end time is not specified...
						 */
						if (Double.isInfinite(actEndTime)) {
							throw new RuntimeException(
									"I think this is discuraged.");
						}
						/*
						 * Send arrival and activity start events.
						 */
						AgentArrivalEvent arrivalEvent = new AgentArrivalEvent(
								arrivalTime, plan.getPerson().getId(),
								act.getLinkId(), prevLeg.getMode());
						eventQueue.add(arrivalEvent);
						ActivityStartEvent startEvent = new ActivityStartEvent(
								arrivalTime, plan.getPerson().getId(),
								act.getLinkId(), act.getFacilityId(),
								act.getType());
						eventQueue.add(startEvent);
					}

					if (idx < elements.size() - 1) {
						/*
						 * This is not the last activity, send activity end and
						 * departure events.
						 */
						Leg nextLeg = (Leg) elements.get(idx + 1);
						ActivityEndEvent endEvent = new ActivityEndEvent(
								actEndTime, plan.getPerson().getId(),
								act.getLinkId(), act.getFacilityId(),
								act.getType());
						eventQueue.add(endEvent);
						AgentDepartureEvent departureEvent = new AgentDepartureEvent(
								actEndTime, plan.getPerson().getId(),
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

		private double calcRouteTravelTime(NetworkRoute route,
				double startTime, TravelTime travelTime, Network network,
				Queue<Event> eventQueue, Id agentId) {
			double tt = 0;
			if (route.getStartLinkId() != route.getEndLinkId()) {
				Id startLink = route.getStartLinkId();
				double linkEnterTime = startTime;
				AgentWait2LinkEvent wait2Link = new AgentWait2LinkEvent(
						linkEnterTime, agentId, startLink, agentId);
				LinkEnterEvent linkEnterEvent = null;
				LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(
						++linkEnterTime, agentId, startLink, agentId);
				eventQueue.add(wait2Link);
				eventQueue.add(linkLeaveEvent);
				double linkLeaveTime = linkEnterTime;
				List<Id> ids = route.getLinkIds();
				for (int i = 0; i < ids.size(); i++) {
					Id link = ids.get(i);
					linkEnterTime = linkLeaveTime;
					linkEnterEvent = new LinkEnterEvent(linkEnterTime, agentId,
							link, agentId);
					eventQueue.add(linkEnterEvent);

					double linkTime = travelTime.getLinkTravelTime(network
							.getLinks().get(link), linkEnterTime, null, null);
					tt += Math.max(linkTime, 1.0);

					linkLeaveTime = Math.max(linkEnterTime + 1, linkEnterTime
							+ linkTime);
					linkLeaveEvent = new LinkLeaveEvent(linkLeaveTime, agentId,
							link, agentId);
					eventQueue.add(linkLeaveEvent);

//					tt += travelTime.getLinkTravelTime(
//							network.getLinks().get(ids.get(i)), linkLeaveTime,
//							null, null);
					// tt++;// 1 sec for each node
				}
				tt += travelTime.getLinkTravelTime(
						network.getLinks().get(route.getEndLinkId()),
						linkLeaveTime, null, null);
			}
			LinkEnterEvent linkEnterEvent = new LinkEnterEvent(startTime+tt, agentId,
					route.getEndLinkId(), agentId);
			eventQueue.add(linkEnterEvent);
			return tt;
		}

		private double calcRouteTravelTime(NetworkRoute route,
				double startTime, TravelTime travelTime, Network network) {
			double tt = 0;
			if (route.getStartLinkId() != route.getEndLinkId()) {

				List<Id> ids = route.getLinkIds();
				for (int i = 0; i < ids.size(); i++) {
					tt += travelTime.getLinkTravelTime(
							network.getLinks().get(ids.get(i)), startTime,
							null, null);
					tt++;// 1 sec for each node
				}
				tt += travelTime.getLinkTravelTime(
						network.getLinks().get(route.getEndLinkId()),
						startTime, null, null);
			}

			return tt;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		executor.shutdown();
	}

}
