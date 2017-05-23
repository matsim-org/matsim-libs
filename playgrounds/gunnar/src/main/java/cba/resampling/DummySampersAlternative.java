package cba.resampling;

import org.matsim.api.core.v01.population.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class DummySampersAlternative implements Alternative {

	// -------------------- CONSTANTS --------------------

	private final double sampersScore;

	private final double sampersTimeScore;

	private final double matsimTimeScore;

	private final double sampersChoiceProba;

	// -------------------- MEMBERS --------------------

	private final EpsilonDistribution epsilonDistribution;

	// -------------------- CONSTRUCTION --------------------

	DummySampersAlternative(final double sampersScore, final double sampersTimeScore, final double matsimTimeScore,
			final double sampersChoiceProba, final EpsilonDistribution epsilonDistribution) {
		this.sampersScore = sampersScore;
		this.sampersTimeScore = sampersTimeScore;
		this.matsimTimeScore = matsimTimeScore;
		this.sampersChoiceProba = sampersChoiceProba;
		this.epsilonDistribution = epsilonDistribution;
	}

	// -------------------- IMPLEMENTATION OF Alternative --------------------

	@Override
	public double getSampersOnlyScore() {
		return this.sampersScore;
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
	public double getSampersChoiceProbability() {
		return sampersChoiceProba;
	}

	@Override
	public EpsilonDistribution getEpsilonDistribution() {
		return this.epsilonDistribution;
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
