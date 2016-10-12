package cba;

import static java.lang.Math.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;

import com.google.inject.Provider;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class UtilityFunction {

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final Map<Tour.Act, Map<Link, Double>> act2link2logSize = new LinkedHashMap<>();

	private final Map<Tour.Act, Double> act2betaSize = new LinkedHashMap<>();

	private final Map<List<Tour.Act>, Double> tourActSeq2asc = new LinkedHashMap<>();

	private final Map<Tour.Mode, Double> mode2asc = new LinkedHashMap<>();

	private final TimeStructureOptimizer timeOpt;

	// -------------------- CONSTRUCTION --------------------

	UtilityFunction(final Scenario scenario, final Provider<TripRouter> tripRouterProvider, final int maxTrials,
			final int maxFailures) {

		this.scenario = scenario;

		final Network net = scenario.getNetwork(); // just a shortcut

		/*
		 * DISTRIBUTION OF 8000 WORK OPPORTUNITIES
		 */

		final Map<Link, Double> workSizes = new LinkedHashMap<>();

		// no work opportunities in the outer ring

		workSizes.put(net.getLinks().get(Id.createLinkId("1_2")), log(0.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("2_1")), log(0.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("1_6")), log(0.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("6_1")), log(0.0));

		workSizes.put(net.getLinks().get(Id.createLinkId("3_2")), log(0.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("2_3")), log(0.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("3_4")), log(0.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("4_3")), log(0.0));

		workSizes.put(net.getLinks().get(Id.createLinkId("5_4")), log(0.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("4_5")), log(0.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("5_6")), log(0.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("6_5")), log(0.0));

		// 2000 work opportunities in the middle ring

		workSizes.put(net.getLinks().get(Id.createLinkId("6_7")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("7_6")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("6_9")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("9_6")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("4_9")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("9_4")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("4_8")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("8_4")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("2_8")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("8_2")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("2_7")), log(2000.0 / 12));
		workSizes.put(net.getLinks().get(Id.createLinkId("7_2")), log(2000.0 / 12));

		// 6000 work opportunities in the center

		workSizes.put(net.getLinks().get(Id.createLinkId("7_8")), log(1000.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("8_7")), log(1000.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("9_7")), log(1000.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("7_9")), log(1000.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("8_9")), log(1000.0));
		workSizes.put(net.getLinks().get(Id.createLinkId("9_8")), log(1000.0));

		this.act2link2logSize.put(Tour.Act.work, workSizes);

		/*
		 * DISTRIBUTION OF 10'000 OTHER ACTIVITY OPPORTUNITIES
		 */

		final Map<Link, Double> otherSizes = new LinkedHashMap<>();

		// uniformly distributed over network

		otherSizes.put(net.getLinks().get(Id.createLinkId("1_2")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("2_1")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("1_6")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("6_1")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("3_2")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("2_3")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("3_4")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("4_3")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("5_4")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("4_5")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("5_6")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("6_5")), log(10000.0 / 30));

		otherSizes.put(net.getLinks().get(Id.createLinkId("2_8")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("8_2")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("2_7")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("7_2")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("6_7")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("7_6")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("6_9")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("9_6")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("4_8")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("8_4")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("4_9")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("9_4")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("7_8")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("8_7")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("8_9")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("9_8")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("9_7")), log(10000.0 / 30));
		otherSizes.put(net.getLinks().get(Id.createLinkId("7_9")), log(10000.0 / 30));

		this.act2link2logSize.put(Tour.Act.other, otherSizes);

		/*
		 * SIZE COEFFICIENTS
		 */

		this.act2betaSize.put(Tour.Act.work, 1.0);
		this.act2betaSize.put(Tour.Act.other, 1.0);

		/*
		 * 
		 */

		this.tourActSeq2asc.put(new ArrayList<Tour.Act>(0), 0.0);
		this.tourActSeq2asc.put(Arrays.asList(Tour.Act.work), 0.0);
		this.tourActSeq2asc.put(Arrays.asList(Tour.Act.other), 0.0);
		this.tourActSeq2asc.put(Arrays.asList(Tour.Act.work, Tour.Act.other), -9.0);

		this.mode2asc.put(Tour.Mode.car, 0.0);
		this.mode2asc.put(Tour.Mode.pt, 0.0);

		/*
		 * 
		 */

		if (tripRouterProvider != null) {
			this.timeOpt = new TimeStructureOptimizer(this.scenario, tripRouterProvider, maxTrials, maxFailures);
		} else {
			this.timeOpt = null;
		}
	}

	// -------------------- INTERNALS --------------------

	private void extractTourData(final Plan plan) {
		this.tourPurposes = new ArrayList<>();
		this.tourLocations = new ArrayList<>();
		this.tourModes = new ArrayList<>();
		/*-
		 * -- Skip plan element 0, which is initial home activity.
		 * -- Skip plan element 1, which is trip to first activity.
		 * -- Use plan element 2, which is first relevant activity.
		 * -- Skip the next 3 plan elements, which are
		 * 		- trip back home (but just extract the mode!)
		 * 		- being again at home
		 * 		- trip to the next activity
		 * -- Etc.
		 * -- This automatically skips the last home activity.
		 */
		for (int q = 2; q < plan.getPlanElements().size(); q += 4) {

			final Activity act = (Activity) plan.getPlanElements().get(q);
			if (act.getType().equals("home")) {
				throw new RuntimeException("Not expected: Plan element with index " + q + " is a home activity.");
			}
			this.tourPurposes.add(Tour.Act.valueOf(act.getType()));
			this.tourLocations.add(this.scenario.getNetwork().getLinks().get(act.getLinkId()));

			final Leg leg = (Leg) plan.getPlanElements().get(q + 1);
			this.tourModes.add(Tour.Mode.valueOf(leg.getMode()));
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	private List<Tour.Act> tourPurposes = null;
	private List<Link> tourLocations = null;
	private List<Tour.Mode> tourModes = null;

	double getUtility(final Plan plan) {

		this.extractTourData(plan); // result in member variables

		// ASC for activity sequence
		double result = this.tourActSeq2asc.get(this.tourPurposes);

		// Score of optimal time structure
		if (this.timeOpt != null) {
			result += this.timeOpt.computeScore(plan);
		}

		for (int i = 0; i < this.tourPurposes.size(); i++) {
			final Tour.Act act = this.tourPurposes.get(i);
			final Link loc = this.tourLocations.get(i);
			final Tour.Mode mode = this.tourModes.get(i);

			// Size of activity location.
			result += this.act2betaSize.get(act) * this.act2link2logSize.get(act).get(loc);

			// ASC for mode.
			result += this.mode2asc.get(mode);
		}

		return result;
	}
}
