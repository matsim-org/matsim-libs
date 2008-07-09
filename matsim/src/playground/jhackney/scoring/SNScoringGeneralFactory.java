package playground.jhackney.scoring;

import org.matsim.plans.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;


public class SNScoringGeneralFactory implements ScoringFunctionFactory {

	private static String factype="";

	public SNScoringGeneralFactory(String factype) {
			SNScoringGeneralFactory.factype=factype;

	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new SNScoringMaxFriendFoeRatio(plan);
	}
	public static String getFacType(){
		return factype;
	}

}