package playground.jhackney.socialnetworks.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.jhackney.SocNetConfigGroup;


public class PlanSocScoringFactory implements ScoringFunctionFactory {

	private String factype;
	private TrackActsOverlap scorer;
	private ScoringFunctionFactory factory;
	private final SocNetConfigGroup snConfig;

	public PlanSocScoringFactory(String factype, TrackActsOverlap scorer, ScoringFunctionFactory sf, SocNetConfigGroup snConfig) {
		this.factype=factype;
		this.scorer=scorer;
		this.factory=sf;
		this.snConfig = snConfig;
	}

	public ScoringFunction createNewScoringFunction(final Plan plan) {
		return new PlanSocScoringFunction(plan, this.factory.createNewScoringFunction(plan), factype, scorer, this.snConfig);
	}


}