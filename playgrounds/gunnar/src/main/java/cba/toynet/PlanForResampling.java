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

	private final double congestedTravelTimeUtility;

	private final double sampersChoiceProba;

	private final EpsilonDistribution epsDistr;

	private Double matsimChoiceProba = null;

	private TourSequence tourSequence = null;

	// -------------------- CONSTRUCTION --------------------

	PlanForResampling(final Plan plan, final double activityModeOnlyUtility,
			final double teleportationTravelTimeUtility, final double congestedTravelTimeUtility,
			final double sampersChoiceProba, final EpsilonDistribution epsDistr) {
		this.plan = plan;
		this.activityModeOnlyUtility = activityModeOnlyUtility;
		this.teleportationTravelTimeUtility = teleportationTravelTimeUtility;
		this.congestedTravelTimeUtility = congestedTravelTimeUtility;
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
}
