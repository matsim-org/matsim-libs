package org.matsim.socialnetworks.scoring;

import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;



public class SNScoringGeneralFactory2 implements ScoringFunctionFactory {

	private String factype;
	private TrackEventsOverlap teo;
	private ScoringFunctionFactory factory;

	public SNScoringGeneralFactory2(String factype, TrackEventsOverlap teo, ScoringFunctionFactory sf) {
		this.factype=factype;
		this.teo=teo;
		this.factory=sf;

	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
//		return new SNScoringMaxFriendFoeRatio(plan, this.factype, this.scorer);
		return new SocializingScoringFunction2(plan, this.factory.getNewScoringFunction(plan), factype, teo);
	}


}