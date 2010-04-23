/**
 * 
 */
package playground.yu.analysis.MZComparison;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
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
		PlanAlgorithm {
	public static boolean isInRange(Id linkId, RoadPricingScheme toll) {
		// if (toll == null)
		// throw new RuntimeException("RoadPricingScheme toll == null");
		// if (linkId == null)
		// throw new RuntimeException("linkId == null");
		// if (toll.getLinkIdSet() == null)
		// throw new RuntimeException("toll.getLinkIdSet() == null");
		return toll.getLinkIdSet().contains(linkId);
	}

	private double carDist, ptDist, wlkDist,
			otherDist,
			// count,// counts all persons in population
			carDist_toll, ptDist_toll, wlkDist_toll,
			otherDist_toll,
			count_toll,// counts all inhabitants in toll area
			carTime, ptTime, wlkTime, otherTime, carTime_toll, ptTime_toll,
			wlkTime_toll, otherTime_toll
			// , linearDist, linearDist_toll
			;
	private RoadPricingScheme toll;
	private boolean inTollRange;

	public MZComparisonData(RoadPricingScheme toll) {
		this.toll = toll;
		reset(0);
	}

	@Override
	public void run(Person person) {
		// count++;
		Plan pl = person.getSelectedPlan();
		inTollRange = isInRange(((PlanImpl) pl).getFirstActivity().getLinkId(),
				toll);
		if (inTollRange)
			count_toll++;
		run(pl);
	}

	public void run(Plan p) {
		PlanImpl plan = (PlanImpl) p;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				double legDist = ((LegImpl) pe).getRoute().getDistance();// leg
				// distance
				// [m]
				String legMode = ((LegImpl) pe).getMode().name();
				double legTime = ((LegImpl) pe).getTravelTime() / 60.0;// travel
				// time
				// [min]
				double legLinearDist = CoordUtils.calcDistance(plan
						.getPreviousActivity((LegImpl) pe).getCoord(), plan
						.getNextActivity((LegImpl) pe).getCoord());// leg linear
				// distance [m]
				/*
				 * linearDist += legLinearDist; if (inTollRange) linearDist_toll
				 * += legLinearDist;
				 */
				if (legMode.equals("car")) {
					carDist += legDist;
					carTime += legTime;
					if (inTollRange) {
						carDist_toll += legDist;
						carTime_toll += legTime;
					}
				} else if (legMode.equals("pt")) {
					ptDist += legDist;
					ptTime += legTime;
					if (inTollRange) {
						ptDist_toll += legDist;
						ptTime_toll += legTime;
					}
				} else if (legMode.equals("walk")) {
					legDist = legLinearDist * 1.5;
					wlkDist += legDist;
					wlkTime += legTime;
					if (inTollRange) {
						wlkDist_toll += legDist;
						wlkTime_toll += legTime;
					}
				} else {
					otherDist += legDist;
					otherTime += legTime;
					if (inTollRange) {
						otherDist_toll += legDist;
						otherTime_toll += legTime;
					}
				}
			}
		}
	}

	public void reset(int iteration) {
		carDist = 0;
		ptDist = 0;
		wlkDist = 0;
		otherDist = 0;
		// count = 0;
		carDist_toll = 0;
		ptDist_toll = 0;
		wlkDist_toll = 0;
		otherDist_toll = 0;
		count_toll = 0;
	}

	public double getAvgTollDailyDistance_car_m() {
		return carDist_toll / count_toll;
	}

	public double getAvgTollDailyDistance_pt_m() {
		return ptDist_toll / count_toll;
	}

	public double getAvgTollDailyDistance_walk_m() {
		return wlkDist_toll / count_toll;
	}

	public double getAvgTollDailyDistance_other_m() {
		return otherDist_toll / count_toll;
	}

	public double getAvgTollDailyEnRouteTime_car_min() {
		return carTime_toll / count_toll;
	}

	public double getAvgTollDailyEnRouteTime_pt_min() {
		return ptTime_toll / count_toll;
	}

	public double getAvgTollDailyEnRouteTime_walk_min() {
		return wlkTime_toll / count_toll;
	}

	public double getAvgTollDailyEnRouteTime_other_min() {
		return otherTime_toll / count_toll;
	}
	/*
	 * // getter -- Leg Linear Distance public double
	 * getAvgLegLinearDistance_m() { return linearDist / count; }
	 * 
	 * public double getAvgTollLegLinearDistance_m() { return linearDist_toll /
	 * count_toll; }
	 * 
	 * // getter -- Work Home Linear Distance public double
	 * getAvgWorkHomeLinearDistance_m() { return 0.0; }
	 * 
	 * public double getAvgTollWorkHomeLinearDistance_m() { return 0.0; }
	 */
}
