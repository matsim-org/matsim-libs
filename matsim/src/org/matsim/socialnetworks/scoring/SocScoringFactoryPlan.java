package org.matsim.socialnetworks.scoring;

import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;


public class SocScoringFactoryPlan implements ScoringFunctionFactory {

	private String factype;
	private TrackActsOverlap scorer;
	private ScoringFunctionFactory factory;

	public SocScoringFactoryPlan(String factype, TrackActsOverlap scorer, ScoringFunctionFactory sf) {
		this.factype=factype;
		this.scorer=scorer;
		this.factory=sf;

	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new SocScoringFunctionPlan(plan, this.factory.getNewScoringFunction(plan), factype, scorer);
	}


}