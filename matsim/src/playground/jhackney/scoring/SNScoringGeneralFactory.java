package playground.jhackney.scoring;

import java.util.Collection;
import java.util.HashMap;

import org.matsim.plans.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.socialnetworks.interactions.*;
import org.matsim.facilities.*;


public class SNScoringGeneralFactory implements ScoringFunctionFactory {

	private String factype="";
	private HashMap<Activity, SocialAct> socialPlansMap=null;

	public SNScoringGeneralFactory(String factype, HashMap<Activity,SocialAct> socialPlansMap) {
			this.factype=factype;
			this.socialPlansMap = socialPlansMap;
			//INITIALIZE THE SCORER BY CALLING THE SOCIAL.PLANS.GENERATOR FOR THE
			//NEWLY CHANGED PLANS.
	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new SNScoringMaxFriendFoeRatio(plan, socialPlansMap, factype);
	}

}