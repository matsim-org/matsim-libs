package org.matsim.socialnetworks.scoring;

import org.matsim.plans.Plan;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingScoringFunction;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;


public class SNScoringGeneralFactory implements ScoringFunctionFactory {

	private String factype;
	private SpatialScorer scorer;
	private ScoringFunctionFactory factory;

	public SNScoringGeneralFactory(String factype, SpatialScorer scorer, ScoringFunctionFactory sf) {
		this.factype=factype;
		this.scorer=scorer;
		this.factory=sf;

	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
//		return new SNScoringMaxFriendFoeRatio(plan, this.factype, this.scorer);
		return new SocializingScoringFunction(plan, this.factory.getNewScoringFunction(plan), factype, scorer);
	}
	

}