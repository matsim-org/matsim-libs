package playground.wrashid.jdeqsim.parallel;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.jdeqsim.Road;
import org.matsim.core.mobsim.jdeqsim.SimulationParameters;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.population.routes.NetworkRoute;

/*
 * TODO: (next steps):
 * - compare the simulations (jdeqsim and p-jdeqsim) => only continue, if it is really faster...
 * - try bigger scenario...
 * - write tests
 * - events handling => how to deal with that?
 */

/*
 * - both halfs of the simulation should have arround the same number of events for best performance (as expected)
 * => but this is not the case yet => need to make the algorithm better
 *
 *
 */

/*
 * JVM options
 * ============
 * => -XX:+UseSpinning is useful for this application
 * => -XX:PreBlockSpin=10 has an influence on the application. A good value found for my local machine was: 10,
 *    but this might be vm/machine dependent...
 */


/*
 * Some first performance experiments:
 * (100% Zürich, on home compi)
 * - parallel JDEQSim (optimal division - manual): 147.7sec / 130.5sec.
 * - JDEQSim: 181.4sec.
 * - parallel JDEQSim (without optimal devision): 153.2 sec./ 136.3 sec. (not yet optimal, as not fifty fifty devision of events).
 * - parallel JDEQSim (optimal division - manual), maxTimeDelta=600: 126.2sec / 109sec.
 * - parallel JDEQSim (optimal division - manual), maxTimeDelta=100000: 121sec / 104sec.
 *
 * (10% Zürich, on home compi)
 * - parallel JDEQSim (optimal division - manual): 19.4 sec. / 17.1 sec.
 * - JDEQSim: 25sec.
 * - parallel JDEQSim (without optimal devision): 21.9 sec. / 19.490 sec.
 *
 *
 * for parallel JDEQSim: (time including initialization / time just of the simulation)
 *
 */
public class PJDEQSimulation extends JDEQSimulation {

	private final static Logger log = Logger.getLogger(PJDEQSimulation.class);
	private int numOfThreads;

	public PJDEQSimulation(Scenario scenario, EventsManager events, int numOfThreads) {
		super(scenario, events);
		this.numOfThreads = numOfThreads; // TODO: use this number really...
	}

	@Override
	public void run() {

		Timer t = new Timer();
		t.startTimer();

		Population population = this.scenario.getPopulation();
		Network network = this.scenario.getNetwork();


		PMessageQueue queue=new PMessageQueue();

		// set the maxTimeDelta
		String JDEQ_SIM="JDEQSim";
		String MAX_TIME_DELTA="maxTimeDelta";
		String maxTimeDelta=this.scenario.getConfig().findParam(JDEQ_SIM, MAX_TIME_DELTA);

		if (maxTimeDelta!=null){
			queue.setMaxTimeDelta(Integer.parseInt(maxTimeDelta));
		}

		PScheduler scheduler = new PScheduler(queue);
		scheduler.getQueue().idOfMainThread = Thread.currentThread().getId();
		SimulationParameters.setAllRoads(new HashMap<Id, Road>());

		// find out networkXMedian
		int numberOfLinks = 0;
		double sumXCoord = 0;
		for (Person person : population.getPersons().values()) {
			// estimate, where to cut the map

			// System.out.println(((Activity)
			// (person.getSelectedPlan().getPlanElements().get(0))).getCoord().getX());

			// count each link of each route...
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				// because home activity gets to little attention
				// because the leg gives the activities on the path enough
				// attention
				// sumXCoord += ((Activity) (actsLegs.get(0))).getCoord()
				// .getX();
				// sumXCoord += ((Activity) (actsLegs.get(0))).getCoord()
				// .getX();
				// numberOfLinks++;

				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getRoute() instanceof NetworkRoute) {
						List<Id> linkIds = ((NetworkRoute) leg.getRoute()).getLinkIds();
						Id[] currentLinkRoute = linkIds.toArray(new Id[linkIds.size()]);

						for (Id linkId : currentLinkRoute) {
							Link link = network.getLinks().get(linkId);
							// because at each road there are many messages
							// enterReq, enter, end, leave, deadlock...
							sumXCoord += link.getCoord().getX();
							// sumXCoord += currentLinkRoute[j].getCoord().getX();
							// sumXCoord += currentLinkRoute[j].getCoord().getX();
							// sumXCoord += currentLinkRoute[j].getCoord().getX();
							// sumXCoord += currentLinkRoute[j].getCoord().getX();

							numberOfLinks++;
						}
					}
				}
			}

		}

		// estimate, where to cut the map
		double networkXMedian = sumXCoord / numberOfLinks;

		// TODO: remove this line...
		// just for finding out how a perfect distribution would work
		// for 10% Zh and 100%:
		// best performance, when for all threads the same number of messages/same number of waiting...
		networkXMedian=683000;
		//networkXMedian += 1500;

		System.out.println();
		System.out.println("SimulationParameters.networkXMedian:"
				+ networkXMedian);
		System.out.println();

		// initialize network
		ExtendedRoad road = null;
		for (Link link : network.getLinks().values()) {
			road = new ExtendedRoad(scheduler, link);

			if (link.getCoord().getX() < networkXMedian) {
				road.setThreadZoneId(0);
			} else {
				road.setThreadZoneId(1);
			}

			SimulationParameters.getAllRoads().put(link.getId(), road);
		}

		// define border roads
		// just one layer long
		ExtendedRoad tempRoad = null;
		for (Link link : network.getLinks().values()) {
			road = (ExtendedRoad) Road.getRoad(link.getId());

			// mark roads, which go away from border roads
			for (Link outLink : road.getLink().getToNode().getOutLinks().values()) {
				tempRoad = (ExtendedRoad) Road.getRoad(outLink.getId());

				if (road.getThreadZoneId()!=tempRoad.getThreadZoneId()) {
					road.setBorderZone(true);
					tempRoad.setBorderZone(true);
				}
			}

		}

		// initialize vehicles
		for (Person person : population.getPersons().values()) {
			new PVehicle(scheduler, person); // the vehicle registers itself to the scheduler
		}

		scheduler.startSimulation();

		t.endTimer();
		log
				.info("Time needed for one iteration (only Parallel JDEQSimulation part): "
						+ t.getMeasuredTime() + "[ms]");
	}
}
