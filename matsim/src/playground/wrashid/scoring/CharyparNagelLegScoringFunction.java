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
	private static final int INITIAL_INDEX = 0;
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

	public void startActivity(final double time, final Act act) {
		// the activity is currently handled by startLeg()
	}

	public void endActivity(final double time) {
	}

	public void startLeg(final double time, final Leg leg) {
		if (this.index % 2 == 0) {
			// it seems we were not informed about activities
			handleAct(time);
		}
		this.lastTime = time;
	}

	public void endLeg(final double time) {
		handleLeg(time);
		this.lastTime = time;
	}

	public void agentStuck(final double time) {
		this.lastTime = time;
		this.score += getStuckPenalty();
	}

	public void finish() {
		if (this.index == this.lastActIndex) {
			handleAct(24*3600); // handle the last act
		}
	}

	public double getScore() {
		return this.score;
	}

	protected double calcActScore(final double arrivalTime, final double departureTime, final Act act) {

		ActUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
		}

		double tmpScore = 0.0;

		/* Calculate the times the agent actually performs the
		 * activity.  The facility must be open for the agent to
		 * perform the activity.  If it's closed, but the agent is
		 * there, the agent must wait instead of performing the
		 * activity (until it opens).
		 *
		 *                                             Interval during which
		 * Relationship between times:                 activity is performed:
		 *
		 *      O________C A~~D  ( 0 <= C <= A <= D )   D...D (not performed)
		 * A~~D O________C       ( A <= D <= O <= C )   D...D (not performed)
		 *      O__A+++++C~~D    ( O <= A <= C <= D )   A...C
		 *      O__A++D__C       ( O <= A <= D <= C )   A...D
		 *   A~~O++++++++C~~D    ( A <= O <= C <= D )   O...C
		 *   A~~O+++++D__C       ( A <= O <= D <= C )   O...D
		 *
		 * Legend:
		 *  A = arrivalTime    (when agent gets to the facility)
		 *  D = departureTime  (when agent leaves the facility)
		 *  O = openingTime    (when facility opens)
		 *  C = closingTime    (when facility closes)
		 *  + = agent performs activity
		 *  ~ = agent waits (agent at facility, but not performing activity)
		 *  _ = facility open, but agent not there
		 *
		 * assume O <= C
		 * assume A <= D
		 */

		double[] openingInterval = this.getOpeningInterval(act);
		double openingTime = openingInterval[0];
		double closingTime = openingInterval[1];

		double activityStart = arrivalTime;
		double activityEnd = departureTime;

		if (openingTime >=  0 && arrivalTime < openingTime) {
			activityStart = openingTime;
		}
		if (closingTime >= 0 && closingTime < departureTime) {
			activityEnd = closingTime;
		}
		if (openingTime >= 0 && closingTime >= 0
				&& ((openingTime > departureTime) || (closingTime < arrivalTime))) {
			// agent could not perform action
			activityStart = departureTime;
			activityEnd = departureTime;
		}
		double duration = activityEnd - activityStart;

		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives to early, has to wait
			tmpScore += this.params.marginalUtilityOfWaiting * (activityStart - arrivalTime);
		}

		// disutility if too late

		double latestStartTime = actParams.getLatestStartTime();
		if (latestStartTime >= 0 && activityStart > latestStartTime) {
			tmpScore += this.params.marginalUtilityOfLateArrival * (activityStart - latestStartTime);
		}

		// utility of performing an action, duration is >= 1, thus log is no problem
		double typicalDuration = actParams.getTypicalDuration();

		if (duration > 0) {
			double utilPerf = this.params.marginalUtilityOfPerforming * typicalDuration
					* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration());
			double utilWait = this.params.marginalUtilityOfWaiting * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
		} else {
			tmpScore += 2*this.params.marginalUtilityOfLateArrival*Math.abs(duration);
		}

		// disutility if stopping too early
		double earliestEndTime = actParams.getEarliestEndTime();
		if (earliestEndTime >= 0 && activityEnd < earliestEndTime) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture * (earliestEndTime - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += this.params.marginalUtilityOfWaiting * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		double minimalDuration = actParams.getMinimalDuration();
		if (minimalDuration >= 0 && duration < minimalDuration) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture * (minimalDuration - duration);
		}

		return tmpScore;
	}

	protected double[] getOpeningInterval(final Act act) {

		ActUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
		}

		double openingTime = actParams.getOpeningTime();
		double closingTime = actParams.getClosingTime();

		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{openingTime, closingTime};

		return openInterval;
	}

	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		double dist = 0.0; // distance in meters

		if (this.params.marginalUtilityOfDistance != 0.0) {
			/* we only as for the route when we have to calculate a distance cost,
			 * because route.getDist() may calculate the distance if not yet
			 * available, which is quite an expensive operation
			 */
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

		if (BasicLeg.Mode.car.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling + this.params.marginalUtilityOfDistance * dist;
		} else if (BasicLeg.Mode.pt.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingPT + this.params.marginalUtilityOfDistance * dist;
		} else if (BasicLeg.Mode.walk.equals(leg.getMode())) {
			tmpScore += travelTime * this.params.marginalUtilityOfTravelingWalk + this.params.marginalUtilityOfDistance * dist;
		} else {
			// use the same values as for "car"
			tmpScore += travelTime * this.params.marginalUtilityOfTraveling + this.params.marginalUtilityOfDistance * dist;
		}

		return tmpScore;
	}

	private double getStuckPenalty() {
		return this.params.abortedPlanScore;
	}

	protected void handleAct(final double time) {
		Act act = (Act)this.plan.getActsLegs().get(this.index);
		if (this.index == 0) {
			this.firstActTime = time;
		} else if (this.index == this.lastActIndex) {
			String lastActType = act.getType();
			if (lastActType.equals(((Act) this.plan.getActsLegs().get(0)).getType())) {
				// the first Act and the last Act have the same type
				this.score += calcActScore(this.lastTime, this.firstActTime + 24*3600, act); // SCENARIO_DURATION
			} else {
				if (this.params.scoreActs) {
				    if (firstLastActWarning <= 10) {
				    	log.warn("The first and the last activity do not have the same type. The correctness of the scoring function can thus not be guaranteed.");
				        if (firstLastActWarning == 10) {
				            log.warn("Additional warnings of this type are suppressed.");
				        }
				        firstLastActWarning++;
				    }					
					
					// score first activity
					Act firstAct = (Act)this.plan.getActsLegs().get(0);
					this.score += calcActScore(0.0, this.firstActTime, firstAct);
					// score last activity
					this.score += calcActScore(this.lastTime, 24*3600, act); // SCENARIO_DURATION
				}
			}
		} else {
			this.score += calcActScore(this.lastTime, time, act);
		}
		this.index++;
	}

	private void handleLeg(final double time) {
		Leg leg = (Leg)this.plan.getActsLegs().get(this.index);
		this.score += calcLegScore(this.lastTime, time, leg);
		this.index++;
	}

}
