package cba.toynet2;

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

	private final TourSequence tourSequence;

	private final Plan plan;

	private final double activityModeUtility;

	private final double sampersTravelTimeUtility;

	private final double sampersChoiceProba;

	private final EpsilonDistribution epsDistr;

	private Double matsimTimeScore = null;

	private Double epsilonRealization = null;

	// -------------------- CONSTRUCTION --------------------

	PlanForResampling(final TourSequence tourSequence, final Plan plan, final double activityModeUtility,
			final double sampersTravelTimeUtility, final double sampersChoiceProba,
			final EpsilonDistribution epsDistr) {
		this.tourSequence = tourSequence;
		this.plan = plan;
		this.activityModeUtility = activityModeUtility;
		this.sampersTravelTimeUtility = sampersTravelTimeUtility;
		this.matsimTimeScore = sampersTravelTimeUtility;
		this.sampersChoiceProba = sampersChoiceProba;
		this.epsDistr = epsDistr;
	}

	// -------------------- FOR TESTING --------------------

	TourSequence getTourSequence() {
		return this.tourSequence;
	}

	// -------------------- IMPLEMENTATION OF Attribute --------------------

	@Override
	public double getSampersOnlyScore() {
		return this.activityModeUtility;
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
		return this.sampersTravelTimeUtility;
	}

	@Override
	public double getMATSimTimeScore() {
		return this.matsimTimeScore;
	}

	@Override
	public double getSampersEpsilonRealization() {
		return this.epsilonRealization;
	}

	@Override
	public void setSampersEpsilonRealization(double eps) {
		this.epsilonRealization = eps;
	}

	@Override
	public void setMATSimTimeScore(double score) {
		this.matsimTimeScore = score;
	}

	@Override
	public Plan getMATSimPlan() {
		return this.plan;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName() + " for person " + this.plan.getPerson() + " and type "
				+ this.tourSequence.type + "\n");
		result.append("V(type,dest,mode) = " + this.activityModeUtility + "\n");
		result.append("V_Sampers(time)   = " + this.sampersTravelTimeUtility + "\n");
		result.append("V_MATSim(time)    = " + this.matsimTimeScore + "\n");
		result.append("P_sampers(this)   = " + this.sampersChoiceProba);
		return result.toString();
	}

}
