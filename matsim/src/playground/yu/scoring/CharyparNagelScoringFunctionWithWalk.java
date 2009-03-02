/**
 * 
 */
package playground.yu.scoring;

import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.CharyparNagelScoringParameters;

/**
 * change scoring function, because "walk"-mode will be implemented
 * 
 * @author yu
 * 
 */
public class CharyparNagelScoringFunctionWithWalk extends
		CharyparNagelScoringFunction {
	private static double offsetWlk = 6.0;

	public CharyparNagelScoringFunctionWithWalk(Plan plan, final CharyparNagelScoringParameters params) {
		super(plan, params);
	}

	@Override
	protected double calcLegScore(double departureTime, double arrivalTime,
			Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in
		// seconds
		// double dist = 0.0; // distance in meters

		// in our current tests, marginalUtilityOfDistance always == 0.0.
		// if (marginalUtilityOfDistance != 0.0) {
		// /* we only as for the route when we have to calculate a distance
		// cost,
		// * because route.getDist() may calculate the distance if not yet
		// * available, which is quite an expensive operation
		// */
		// Route route = leg.getRoute();
		// dist = route.getDist();
		// /* TODO the route-distance does not contain the length of the first
		// or
		// * last link of the route, because the route doesn't know those.
		// Should
		// * be fixed somehow, but how? MR, jan07
		// */
		// /* TODO in the case of within-day replanning, we cannot be sure that
		// the
		// * distance in the leg is the actual distance driven by the agent.
		// */
		// }

		if (BasicLeg.Mode.car.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling;
			System.out.println("car\ttmpScore=" + travelTime + "*"
					+ this.params.marginalUtilityOfTraveling + "=" + travelTime
					* this.params.marginalUtilityOfTraveling);
		} else if (BasicLeg.Mode.pt.equals(leg.getMode())) {
			tmpScore += travelTime * (-3.0) / 3600.0;
			System.out.println("pt\ttmpScore=" + travelTime + "*" + (-3.0)
					/ 3600.0 + "=" + travelTime * (-3.0) / 3600.0);
		} else if (BasicLeg.Mode.walk.equals(leg.getMode())) {
			tmpScore += offsetWlk + travelTime * (-18.0) / 3600.0;
			System.out.println("walk\ttmpScore=" + offsetWlk + "+" + travelTime
					+ "*" + (-18.0) / 3600.0 + "=" + offsetWlk + travelTime
					* (-18.0) / 3600.0);
		} else {
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling;
		}

		return tmpScore;
	}

}
