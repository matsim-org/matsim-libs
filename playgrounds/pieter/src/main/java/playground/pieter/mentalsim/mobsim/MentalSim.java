/**
 * 
 */
package playground.pieter.mentalsim.mobsim;

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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.pieter.mentalsim.util.CollectionUtils;

/**
 * @author fouriep
 * 
 */
public class MentalSim implements Mobsim {

	Scenario sc;
	EventsManager eventManager;

	private final static double MIN_ACT_DURATION = 1.0;

	private final static double MIN_LEG_DURATION = 0.0;

	private SimThread[] threads;

	private Future<?>[] futures;

	private final ExecutorService executor;
	private TravelTimeCalculator linkTravelTimes;

	public MentalSim(Scenario sc, EventsManager eventsManager) {
		this.sc = sc;
		this.eventManager = eventsManager;
		int numThreads = Integer.parseInt(sc.getConfig().getParam("global",
				"numberOfThreads"));
		executor = Executors.newFixedThreadPool(numThreads);

		threads = new SimThread[numThreads];
		for (int i = 0; i < numThreads; i++)
			threads[i] = new SimThread();

		futures = new Future[numThreads];
	}

	public MentalSim(Scenario sc2, EventsManager eventsManager,
			PersonalizableTravelTime ttcalc) {
		this(sc2, eventsManager);
		this.linkTravelTimes = (TravelTimeCalculator) ttcalc;
	}

	public void run() {

		Collection<Plan> plans = new LinkedHashSet<Plan>();
		for (Person p : sc.getPopulation().getPersons().values()) {
			// if (Math.random() < 0.1)
			if (p.getSelectedPlan().getScore() == 0.0) {

				plans.add(p.getSelectedPlan());
			}
				Logger.getLogger(this.getClass()).error("Executing "+plans.size()+" plans in mental simulation.");
			
		}

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
			threads[i]
					.init(segments[i], network, linkTravelTimes, eventManager);
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
						NetworkRoute route = (NetworkRoute) prevLeg.getRoute();
						double travelTime = calcRouteTravelTime(route,
								prevEndTime, linkTravelTimes, network);
						if (prevLeg.getMode().equals("pt"))
							travelTime *= 2.06;
						travelTime = Math.max(MIN_LEG_DURATION, travelTime);
						double arrivalTime = travelTime + prevEndTime;
						/*
						 * If act end time is not specified...
						 */
						if (Double.isInfinite(actEndTime)) {
							throw new RuntimeException(
									"I think this is discuraged.");
						}
						/*
						 * Make sure that the activity does not end before the
						 * agent arrives.
						 */
						actEndTime = Math.max(arrivalTime + MIN_ACT_DURATION,
								actEndTime);
						/*
						 * Send arrival and activity start events.
						 */
						AgentArrivalEvent arrivalEvent = new AgentArrivalEventImpl(
								arrivalTime, plan.getPerson().getId(),
								act.getLinkId(), prevLeg.getMode());
						eventQueue.add(arrivalEvent);
						ActivityEvent startEvent = new ActivityStartEventImpl(
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
						ActivityEvent endEvent = new ActivityEndEventImpl(
								actEndTime, plan.getPerson().getId(),
								act.getLinkId(), act.getFacilityId(),
								act.getType());
						eventQueue.add(endEvent);
						AgentDepartureEvent departureEvent = new AgentDepartureEventImpl(
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
				double startTime, TravelTime travelTime, Network network) {
			double tt = 0;
			if (route.getStartLinkId() != route.getEndLinkId()) {

				List<Id> ids = route.getLinkIds();
				for (int i = 0; i < ids.size(); i++) {
					tt += travelTime.getLinkTravelTime(
							network.getLinks().get(ids.get(i)), startTime);
					tt++;// 1 sec for each node
				}
				tt += travelTime
						.getLinkTravelTime(
								network.getLinks().get(route.getEndLinkId()),
								startTime);
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
