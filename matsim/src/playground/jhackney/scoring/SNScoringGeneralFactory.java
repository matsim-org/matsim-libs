package playground.jhackney.scoring;

import java.util.Collection;

import org.matsim.plans.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.socialnetworks.interactions.*;

public class SNScoringGeneralFactory implements ScoringFunctionFactory {

	private String factype="";
	private HashMap<Activity, SocialAct> socialPlans=null;

	public SNScoringGeneralFactory(String factype, HashMap<Activity,SocialAct> socialPlans) {
			this.factype=factype;
			this.socialPlans = socialPlans;
			//INITIALIZE THE SCORER BY CALLING THE SOCIAL.PLANS.GENERATOR FOR THE
			//NEWLY CHANGED PLANS.
	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new SNScoringMaxFriendFoeRatio(plan, socialPlansMap, factype);
	}

}