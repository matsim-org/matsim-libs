package playground.vsp.parkAndRide.scoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.PtConstants;

import playground.vsp.parkAndRide.PRConstants;
import playground.vsp.parkAndRide.replanning.EllipseSearch;
import scala.Char;

/**
 * An activity scoring function which extends the ordinary CharyparNagelActivityScoring.
 * For each park-and-ride activity the intermodal transfer penalty is added to the score of a plan.
 *
 * @author ikaddoura
 */
public class PRActivityScoringFunction implements SumScoringFunction.ActivityScoring {
	private static final Logger log = LogManager.getLogger(EllipseSearch.class);
	private final CharyparNagelActivityScoring delegate;

	private ScoringParameters params;
	private double intermodalTransferPenalty;
	private double score = 0;

	public PRActivityScoringFunction(ScoringParameters params, double intermodalTransferPenalty) {
		this.params = params;
		this.intermodalTransferPenalty = intermodalTransferPenalty;
		this.delegate = new CharyparNagelActivityScoring(params);
	}

	@Override
	public void finish() {
		this.delegate.finish();
	}

	@Override
	public double getScore() {
		return this.score + this.delegate.getScore();
	}

	@Override
	public void handleFirstActivity(Activity act) {
		this.delegate.handleFirstActivity(act);
	}

	@Override
	public void handleLastActivity(Activity act) {
		this.delegate.handleLastActivity(act);
	}

	@Override
	public void handleActivity(Activity act) {
		this.score += calcActScore(act.getStartTime().seconds(), act.getEndTime().seconds(), act);
	}

	private double calcActScore(double arrivalTime, double departureTime, Activity act) {
		if (act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			return (departureTime - arrivalTime) * this.params.marginalUtilityOfWaiting_s;
		} else if (act.getType().toString().equals(PRConstants.PARKANDRIDE_ACTIVITY_TYPE)){
			log.info("Adding the intermodal transfer penalty to the plan score: " + this.intermodalTransferPenalty);
			return this.intermodalTransferPenalty;
		} else {
			this.delegate.handleActivity(act);
			return 0;
		}
	}
}
