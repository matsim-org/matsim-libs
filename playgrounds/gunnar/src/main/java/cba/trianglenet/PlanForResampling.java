package cba.trianglenet;

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

	private final double sampersTimeScore;

	private final double matsimTimeScore;

	private final double sampersChoiceProba;

	private final EpsilonDistribution epsDistr;

	// for testing
	private Double matsimChoiceProba = null;

	// -------------------- CONSTRUCTION --------------------

	PlanForResampling(final Plan plan, final double sampersOnlyScore, final double sampersTimeScore,
			final double matsimTimeScore, final double sampersChoiceProba, final EpsilonDistribution epsDistr) {
		this.plan = plan;
		this.sampersOnlyScore = sampersOnlyScore;
		this.sampersTimeScore = sampersTimeScore;
		this.matsimTimeScore = matsimTimeScore;
		this.sampersChoiceProba = sampersChoiceProba;
		this.epsDistr = epsDistr;
	}

	// -------------------- FOR TESTING --------------------

	void setMATSimChoiceProba(final Double matsimChoiceProba) {
		this.matsimChoiceProba = matsimChoiceProba;
	}

	Double getMATSimChoiceProba() {
		return this.matsimChoiceProba;
	}

	// -------------------- IMPLEMENTATION OF Attribute --------------------

	@Override
	public double getSampersOnlyScore() {
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
		return this.sampersTimeScore;
	}

	@Override
	public double getMATSimTimeScore() {
		return this.matsimTimeScore;
	}

	@Override
	public double getSampersEpsilonRealization() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSampersEpsilonRealization(double eps) {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public void setMATSimTimeScore(double score) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public Plan getMATSimPlan() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateMATSimTimeScore(double score, double innovationWeight) {
		// TODO Auto-generated method stub
		
	}
}
