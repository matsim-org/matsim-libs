package cba.toynet;

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

	private final double activityModeOnlyUtility;

	private final double teleportationTravelTimeUtility;

	private double congestedTravelTimeUtility;

	private final double sampersChoiceProba;

	private final EpsilonDistribution epsDistr;

	private final double frozenEpsilon;

	private Double epsilonRealization = null;

	private Double matsimChoiceProba = null;

	private TourSequence tourSequence = null;

	// -------------------- CONSTRUCTION --------------------

	PlanForResampling(final Plan plan, final double activityModeOnlyUtility,
			final double teleportationTravelTimeUtility, final double congestedTravelTimeUtility,
			final double sampersChoiceProba, final EpsilonDistribution epsDistr, final double frozenEpsilon) {
		this.plan = plan;
		this.activityModeOnlyUtility = activityModeOnlyUtility;
		this.teleportationTravelTimeUtility = teleportationTravelTimeUtility;
		this.congestedTravelTimeUtility = congestedTravelTimeUtility;
		this.sampersChoiceProba = sampersChoiceProba;
		this.epsDistr = epsDistr;
		this.frozenEpsilon = frozenEpsilon;
	}

	PlanForResampling(final Plan plan, final double activityModeOnlyUtility,
			final double teleportationTravelTimeUtility, final double congestedTravelTimeUtility,
			final double sampersChoiceProba, final EpsilonDistribution epsDistr) {
		this(plan, activityModeOnlyUtility, teleportationTravelTimeUtility, congestedTravelTimeUtility,
				sampersChoiceProba, epsDistr, epsDistr.nextEpsilon());
	}

	// -------------------- FOR TESTING --------------------

	void setMATSimChoiceProba(final Double matsimChoiceProba) {
		this.matsimChoiceProba = matsimChoiceProba;
	}

	Double getMATSimChoiceProba() {
		return this.matsimChoiceProba;
	}

	void setTourSequence(final TourSequence tourSequence) {
		this.tourSequence = tourSequence;
	}

	TourSequence getTourSequence() {
		return this.tourSequence;
	}

	// -------------------- IMPLEMENTATION OF Attribute --------------------

	@Override
	public double getSampersOnlyScore() {
		return this.activityModeOnlyUtility;
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
		return this.teleportationTravelTimeUtility;
	}

	@Override
	public double getMATSimTimeScore() {
		return this.congestedTravelTimeUtility;
	}

	@Override
	public double getSampersEpsilonRealization() {
		return this.epsilonRealization;
	}

	@Override
	public void setSampersEpsilonRealization(double eps) {
		this.epsilonRealization = eps;
	}

//	@Override
	public void setMATSimTimeScore(double score) {
		this.congestedTravelTimeUtility = score;
	}

	// TODO NEW
	public double getFrozenEpsilon() {
		return this.frozenEpsilon;
	}

	@Override
	public Plan getMATSimPlan() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateMATSimTimeScore(double score, double innovationWeight) {
		throw new UnsupportedOperationException();
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName() + " for person " + this.plan.getPerson() + " and type "
				+ this.tourSequence.type + "\n");
		result.append("V(type,dest,mode) = " + this.activityModeOnlyUtility + "\n");
		result.append("V_Sampers(time)   = " + this.teleportationTravelTimeUtility + "\n");
		result.append("V_MATSim(time)    = " + this.congestedTravelTimeUtility + "\n");
		result.append("P_sampers(this)   = " + this.sampersChoiceProba + "\n");
		result.append("P_MATSim(this)    = " + this.matsimChoiceProba);
		return result.toString();
	}

}
