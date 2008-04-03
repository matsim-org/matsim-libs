package playground.jhackney.scoring;

import java.util.HashMap;

import org.matsim.plans.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.socialnetworks.interactions.*;
import org.matsim.facilities.*;


public class SNScoringGeneralFactory implements ScoringFunctionFactory {

	private static String factype="";
	private static HashMap<Activity, SocialAct> socialPlansMap=null;

	public SNScoringGeneralFactory(String factype, HashMap<Activity,SocialAct> socialPlansMap) {
			SNScoringGeneralFactory.factype=factype;
			SNScoringGeneralFactory.socialPlansMap = socialPlansMap;
			//INITIALIZE THE SCORER BY CALLING THE SOCIAL.PLANS.GENERATOR FOR THE
			//NEWLY CHANGED PLANS.
	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new SNScoringMaxFriendFoeRatio(plan);
	}
	public static String getFacType(){
		return factype;
	}
	public static HashMap<Activity,SocialAct> getSocialActsMap(){
		return socialPlansMap;
	}

}