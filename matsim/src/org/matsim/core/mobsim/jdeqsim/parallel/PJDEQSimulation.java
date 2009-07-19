package org.matsim.core.mobsim.jdeqsim.parallel;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.population.Activity;
import org.matsim.core.api.experimental.population.Route;
import org.matsim.core.events.Events;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.jdeqsim.*;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.api.experimental.population.Leg;

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
 * - parallel JDEQSim (optimal division - manual): 
 * - JDEQSim: 181.4sec.
 * - parallel JDEQSim (without optimal devision): 153.2 sec./ 136.3 sec. (not yet optimal, as not fifty fifty devision of events).
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

	private int numOfThreads;

	public PJDEQSimulation(NetworkLayer network, PopulationImpl population,
			Events events, int numOfThreads) {
		super(network, population, events);
		this.numOfThreads = numOfThreads; // TODO: use this number really...
		log = Logger.getLogger(JDEQSimulation.class);
	}

	public void run() {
		log = Logger.getLogger(PJDEQSimulation.class);

		Timer t = new Timer();
		t.startTimer();

		PScheduler scheduler = new PScheduler(new PMessageQueue());
		scheduler.getQueue().idOfMainThread = Thread.currentThread().getId();
		SimulationParameters.setAllRoads(new HashMap<String, Road>());

		// find out networkXMedian
		int numberOfLinks = 0;
		double sumXCoord = 0;
		for (PersonImpl person : this.population.getPersons().values()) {
			// estimate, where to cut the map

			// System.out.println(((Activity)
			// (person.getSelectedPlan().getPlanElements().get(0))).getCoord().getX());

			List<? extends BasicPlanElement> actsLegs = person
					.getSelectedPlan().getPlanElements();

			// count each link of each route...
			int i = 1;
			while (i < actsLegs.size()) {
				// because home activity gets to little attention
				// because the leg gives the activities on the path enough
				// attention
				// sumXCoord += ((Activity) (actsLegs.get(0))).getCoord()
				// .getX();
				// sumXCoord += ((Activity) (actsLegs.get(0))).getCoord()
				// .getX();
				// numberOfLinks++;

				Leg leg = ((Leg) actsLegs.get(i));
				if (leg.getRoute() instanceof NetworkRoute) {
					List<Link> links = ((NetworkRoute) leg.getRoute())
							.getLinks();
					Link[] currentLinkRoute = links.toArray(new LinkImpl[links
							.size()]);

					for (int j = 0; j < currentLinkRoute.length; j++) {

						// because at each road there are many messages
						// enterReq, enter, end, leave, deadlock...
						sumXCoord += currentLinkRoute[j].getCoord().getX();
						// sumXCoord += currentLinkRoute[j].getCoord().getX();
						// sumXCoord += currentLinkRoute[j].getCoord().getX();
						// sumXCoord += currentLinkRoute[j].getCoord().getX();
						// sumXCoord += currentLinkRoute[j].getCoord().getX();

						numberOfLinks++;
					}
				}
				i += 2;
			}

		}

		// estimate, where to cut the map
		double networkXMedian = sumXCoord / numberOfLinks;

		// TODO: remove this line...
		// just for finding out how a perfect distribution would work
		// for 10% Zh: 
		//networkXMedian=682997.8204694573;
		//networkXMedian += 3000;

		System.out.println();
		System.out.println("SimulationParameters.networkXMedian:"
				+ networkXMedian);
		System.out.println();

		// initialize network
		ExtendedRoad road = null;
		for (LinkImpl link : this.network.getLinks().values()) {
			road = new ExtendedRoad(scheduler, link);

			if (link.getCoord().getX() < networkXMedian) {
				road.setThreadZoneId(0);
			} else {
				road.setThreadZoneId(1);
			}

			SimulationParameters.getAllRoads().put(link.getId().toString(),
					road);
		}

		// define border roads
		// just one layer long
		ExtendedRoad tempRoad = null;
		for (LinkImpl link : this.network.getLinks().values()) {
			road = (ExtendedRoad) Road.getRoad(link.getId().toString());
			
			// mark roads, which go away from border roads
			for (LinkImpl outLink : road.getLink().getToNode().getOutLinks()
					.values()) {
				tempRoad = (ExtendedRoad) Road.getRoad(outLink.getId()
						.toString());

				if (road.getThreadZoneId()!=tempRoad.getThreadZoneId()) {
					road.setBorderZone(true);
					tempRoad.setBorderZone(true);
				}
			}

		}

		// initialize vehicles
		PVehicle vehicle = null;
		// the vehicle has registered itself to the scheduler
		for (PersonImpl person : this.population.getPersons().values()) {
			vehicle = new PVehicle(scheduler, person);
		}

		// just inserted to remove message in bug analysis, that vehicle
		// variable is never read
		vehicle.toString();

		scheduler.startSimulation();

		t.endTimer();
		log
				.info("Time needed for one iteration (only Parallel JDEQSimulation part): "
						+ t.getMeasuredTime() + "[ms]");
	}
}
