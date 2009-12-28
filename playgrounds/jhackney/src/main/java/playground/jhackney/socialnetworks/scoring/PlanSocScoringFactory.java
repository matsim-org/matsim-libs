package playground.jhackney.socialnetworks.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;


public class PlanSocScoringFactory implements ScoringFunctionFactory {

	private String factype;
	private TrackActsOverlap scorer;
	private ScoringFunctionFactory factory;

	public PlanSocScoringFactory(String factype, TrackActsOverlap scorer, ScoringFunctionFactory sf) {
		this.factype=factype;
		this.scorer=scorer;
		this.factory=sf;

	}

	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return new PlanSocScoringFunction(plan, this.factory.getNewScoringFunction(plan), factype, scorer);
	}


}