package playground.vsp.parkAndRide.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.PtConstants;

import playground.vsp.parkAndRide.PRConstants;
import playground.vsp.parkAndRide.replanning.EllipseSearch;

/**
 * An activity scoring function which extends the ordinary CharyparNagelActivityScoring.
 * For each park-and-ride activity the intermodal transfer penalty is added to the score of a plan.
 * 
 * @author ikaddoura
 *
 */
public class PRActivityScoringFunction extends org.matsim.deprecated.scoring.functions.CharyparNagelActivityScoring {
	private static final Logger log = Logger.getLogger(EllipseSearch.class);

	private ScoringParameters params;
	private double intermodalTransferPenalty;

	public PRActivityScoringFunction(ScoringParameters params, double intermodalTransferPenalty) {
		super(params);
		this.params = params;
		this.intermodalTransferPenalty = intermodalTransferPenalty;
	}

	@Override
	protected double calcActScore(double arrivalTime, double departureTime, Activity act) {
		if (act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			return (departureTime - arrivalTime) * this.params.marginalUtilityOfWaiting_s;
		} else if (act.getType().toString().equals(PRConstants.PARKANDRIDE_ACTIVITY_TYPE)){
			log.info("Adding the intermodal transfer penalty to the plan score: " + this.intermodalTransferPenalty);
			return this.intermodalTransferPenalty;
		} else {
			return super.calcActScore(arrivalTime, departureTime, act);
		}
	}
}
