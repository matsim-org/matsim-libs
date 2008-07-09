package playground.jhackney.scoring;

import org.matsim.plans.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;


public class SNScoringGeneralFactory implements ScoringFunctionFactory {

	private static String factype="";
	private static SpatialScorer scorer;

	public SNScoringGeneralFactory(String factype, SpatialScorer scorer) {
		SNScoringGeneralFactory.factype=factype;
		this.scorer=scorer;

	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new SNScoringMaxFriendFoeRatio(plan);
	}
	public static String getFacType(){
		return factype;
	}
	public static SpatialScorer getScorer(){
		return scorer;
	}
}