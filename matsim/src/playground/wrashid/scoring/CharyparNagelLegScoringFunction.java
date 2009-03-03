package playground.wrashid.scoring;

import org.apache.log4j.Logger;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Route;
import org.matsim.population.ActUtilityParameters;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.utils.misc.Time;

import playground.wrashid.scoring.interfaces.BasicScoringFunction;
import playground.wrashid.scoring.interfaces.LegScoringFunction;

public class CharyparNagelLegScoringFunction implements LegScoringFunction, BasicScoringFunction {

	protected final Person person;
	protected final Plan plan;

	protected double score;
	private double lastTime;
	private int index; // the current position in plan.actslegs
	private double firstActTime;
	private final int lastActIndex;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final int INITIAL_INDEX = 1;
	private static final double INITIAL_FIRST_ACT_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;
	
	private static int firstLastActWarning = 0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;
	
	private static final Logger log = Logger.getLogger(CharyparNagelScoringFunction.class);

	public CharyparNagelLegScoringFunction(final Plan plan, final CharyparNagelScoringParameters params) {
		this.params = params;
		this.reset();

		this.plan = plan;
		this.person = this.plan.getPerson();
		this.lastActIndex = this.plan.getActsLegs().size() - 1;
	}

	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.index = INITIAL_INDEX;
		this.firstActTime = INITIAL_FIRST_ACT_TIME;
		this.score = INITIAL_SCORE;
	}


	public void startLeg(final double time, final Leg leg) {
		this.lastTime = time;
	}

	public void endLeg(final double time) {
		handleLeg(time);
		this.lastTime = time;
	}



	public void finish() {
		
	}

	public double getScore() {
		return this.score;
	}




	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds

		/* we only as for the route when we have to calculate a distance cost,
		 * because route.getDist() may calculate the distance if not yet
		 * available, which is quite an expensive operation
		 */
		double dist = 0.0; // distance in meters


		if (BasicLeg.Mode.car.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceCar != 0.0) {
				Route route = leg.getRoute();
				dist = route.getDist();
				/* TODO the route-distance does not contain the length of the first or
				 * last link of the route, because the route doesn't know those. Should
				 * be fixed somehow, but how? MR, jan07
				 */
				/* TODO in the case of within-day replanning, we cannot be sure that the
				 * distance in the leg is the actual distance driven by the agent.
				 */
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling + this.params.marginalUtilityOfDistanceCar * dist;
		} else if (BasicLeg.Mode.pt.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistancePt != 0.0){
				dist = leg.getRoute().getDist();
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingPT + this.params.marginalUtilityOfDistancePt * dist;
		} else if (BasicLeg.Mode.walk.equals(leg.getMode())) {
			if (this.params.marginalUtilityOfDistanceWalk != 0.0){
				dist = leg.getRoute().getDist();
			}
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk + this.params.marginalUtilityOfDistanceWalk * dist;
		} else {
			if (this.params.marginalUtilityOfDistanceCar != 0.0){
				dist = leg.getRoute().getDist();
			}
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling + this.params.marginalUtilityOfDistanceCar * dist;
		}

		return tmpScore;
	}





	private void handleLeg(final double time) {
		Leg leg = (Leg)this.plan.getActsLegs().get(this.index);
		this.score += calcLegScore(this.lastTime, time, leg);
		this.index+=2;
	}

}
