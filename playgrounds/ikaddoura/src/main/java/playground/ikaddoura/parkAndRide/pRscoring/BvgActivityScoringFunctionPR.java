package playground.ikaddoura.parkAndRide.pRscoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.PtConstants;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideConstants;

public class BvgActivityScoringFunctionPR extends CharyparNagelActivityScoring {

	private CharyparNagelScoringParameters params;
	private BvgScoringFunctionParametersPR bvgParams;

	public BvgActivityScoringFunctionPR(Plan plan, CharyparNagelScoringParameters params, BvgScoringFunctionParametersPR bvgParams) {
		super(params);
		this.params = params;
		this.bvgParams = bvgParams;
	}

	@Override
	protected double calcActScore(double arrivalTime, double departureTime, Activity act) {
		if (act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			return (departureTime - arrivalTime) * this.params.marginalUtilityOfWaiting_s; // ?
		} else if (act.getType().toString().equals(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){		
			return this.bvgParams.intermodalTransferPenalty;
		} else {
			return super.calcActScore(arrivalTime, departureTime, act);
		}
	}
}
