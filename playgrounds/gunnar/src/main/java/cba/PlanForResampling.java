package cba;

import org.matsim.api.core.v01.population.Plan;

import cba.resampling.Alternative;
import cba.resampling.EpsilonDistribution;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class PlanForResampling implements Alternative {

	// -------------------- MEMBERS --------------------d

	final Plan plan;
	
	private final double sampersOnlyScore;
	
	private final double sampersAndMATSimScore;

	private final double sampersChoiceProba;
	
	private final EpsilonDistribution epsDistr;
	
	// -------------------- CONSTRUCTION --------------------

	PlanForResampling(final Plan plan, final double sampersOnlyScore, final double sampersAndMATSimScore, final double sampersChoiceProba, final EpsilonDistribution epsDistr) {
		this.plan = plan;
		this.sampersOnlyScore = sampersOnlyScore;
		this.sampersAndMATSimScore = sampersAndMATSimScore;
		this.sampersChoiceProba = sampersChoiceProba;
		this.epsDistr = epsDistr;
	}

	// -------------------- TODO IMPLEMENTATION OF Attribute --------------------

	@Override
	public double getSampersScore() {
		return this.sampersOnlyScore;
	}

	@Override
	public double getSampersChoiceProbability() {
		return this.sampersChoiceProba;
	}

	@Override
	public EpsilonDistribution getEpsilonDistribution() {
		return this.epsDistr;
	}

	@Override
	public double getSampersTimeScore() {
		return 0;
	}

	@Override
	public double getMATSimTimeScore() {
		return (this.sampersAndMATSimScore - this.sampersOnlyScore);
	}
}
