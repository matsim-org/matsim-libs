/**
 * 
 */
package playground.yu.analysis;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * prepares the data to compare with "BFS/ARE: Mikrozensus zum Verkehrsverhalten
 * 2005" (Kanton Zurich)
 * 
 * @author yu
 * 
 */
public class MZComparisonData extends AbstractPersonAlgorithm implements
		PlanAlgorithm, AgentDepartureEventHandler, AgentArrivalEventHandler {
	public static boolean isInRange(Link loc, RoadPricingScheme toll) {
		return toll.getLinks().contains(loc);
	}

	private double carDist, ptDist, wlkDist, otherDist, count, carDist_toll,
			ptDist_toll, wlkDist_toll, otherDist_toll, count_toll;

	private RoadPricingScheme toll;

	private boolean inTollRange;

	public MZComparisonData(RoadPricingScheme toll) {
		this.toll = toll;
		reset(0);
	}

	@Override
	public void run(Person person) {
		count++;
		Plan pl = person.getSelectedPlan();
		inTollRange = isInRange(pl.getFirstActivity().getLink(), toll);
		if (inTollRange)
			count_toll++;
		run(pl);
	}

	public void run(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				double legDist = ((Leg) pe).getRoute().getDistance();
				if (((Leg) pe).getMode().name().equals("car")) {
					carDist += legDist;
					if (inTollRange)
						carDist_toll += legDist;
				} else if (((Leg) pe).getMode().name().equals("pt")) {
					ptDist += legDist;
					if (inTollRange)
						ptDist_toll += legDist;
				} else if (((Leg) pe).getMode().name().equals("walk")) {
					legDist = CoordUtils.calcDistance(plan.getPreviousActivity(
							(Leg) pe).getCoord(), plan
							.getNextActivity((Leg) pe).getCoord()) * 1.5;
					wlkDist += legDist;
					if (inTollRange)
						wlkDist_toll += legDist;
				}
			}
		}
	}

	public void reset(int iteration) {
		carDist = 0;
		ptDist = 0;
		wlkDist = 0;
		otherDist = 0;
		count = 0;
		carDist_toll = 0;
		ptDist_toll = 0;
		wlkDist_toll = 0;
		otherDist_toll = 0;
		count_toll = 0;
	}

	public void handleEvent(AgentDepartureEvent event) {
		// TODO Auto-generated method stub
	}

	public void handleEvent(AgentArrivalEvent event) {
		// TODO Auto-generated method stub

	}

	// getter -- DailyDistance
	public double getDailyDistance_car_m() {
		return carDist / count;
	}

	public double getTollDailyDistance_car_m() {
		return carDist_toll / count_toll;
	}

	public double getDailyDistance_pt_m() {
		return ptDist / count;
	}

	public double getTollDailyDistance_pt_m() {
		return ptDist_toll / count_toll;
	}

	public double getDailyDistance_walk_m() {
		return wlkDist / count;
	}

	public double getTollDailyDistance_walk_m() {
		return wlkDist_toll / count_toll;
	}

	public double getDailyDistance_other_m() {
		return otherDist / count;
	}

	public double getTollDailyDistance_other_m() {
		return otherDist_toll / count_toll;
	}

	// getter -- DailyEnRouteTime
	public double getDailyEnRouteTime_min() {
		return 0.0;
	}

	public double getLegLinearDistance_m() {
		return 0.0;
	}

	public double getWorkHomeLinearDistance_m() {
		return 0.0;
	}
}
