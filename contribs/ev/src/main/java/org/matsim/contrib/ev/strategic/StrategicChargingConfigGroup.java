package org.matsim.contrib.ev.strategic;

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.contrib.ev.strategic.costs.AttributeBasedChargingCostsParameters;
import org.matsim.contrib.ev.strategic.costs.ChargingCostsParameters;
import org.matsim.contrib.ev.strategic.costs.DefaultChargingCostsParameters;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostsParameters;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoringParameters;
import org.matsim.core.config.Config;

import com.google.common.base.Verify;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Configuration for the Strategic Electric Vehicle Charging package (SEVC).
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {
	public static final String GROUP_NAME = "strategic_charging";

	public static StrategicChargingConfigGroup get(Config config) {
		return (StrategicChargingConfigGroup) config.getModules().get(GROUP_NAME);
	}

	public StrategicChargingConfigGroup() {
		super(GROUP_NAME);

		addDefinition(ChargingPlanScoringParameters.GROUP_NAME, //
				ChargingPlanScoringParameters::new, //
				() -> scoring, //
				s -> {
					scoring = (ChargingPlanScoringParameters) s;
				});

		addDefinition(DefaultChargingCostsParameters.SET_NAME, //
				DefaultChargingCostsParameters::new, //
				() -> (DefaultChargingCostsParameters) costs, //
				c -> {
					costs = (DefaultChargingCostsParameters) c;
				});

		addDefinition(AttributeBasedChargingCostsParameters.SET_NAME, //
				AttributeBasedChargingCostsParameters::new, //
				() -> (AttributeBasedChargingCostsParameters) costs, //
				c -> {
					costs = (AttributeBasedChargingCostsParameters) c;
				});

		addDefinition(TariffBasedChargingCostsParameters.SET_NAME, //
				TariffBasedChargingCostsParameters::new, //
				() -> (TariffBasedChargingCostsParameters) costs, //
				c -> {
					costs = (TariffBasedChargingCostsParameters) c;
				});
	}

	private ChargingPlanScoringParameters scoring;
	private ChargingCostsParameters costs;

	@Parameter
	@Comment("Minimum duration of the activity-based charging slots")
	@PositiveOrZero
	private double minimumActivityChargingDuration = 900.0;

	@Parameter
	@Comment("Maximum duration of the activity-based charging slots")
	@PositiveOrZero
	private double maximumActivityChargingDuration = Double.POSITIVE_INFINITY;

	@Parameter
	@Comment("Minimum drive duration at which charging along the route is considered")
	@PositiveOrZero
	private double minimumEnrouteDriveTime = 3600.0;;

	@Parameter
	@Comment("Minimum duration of enroute charging. A random value between the minimum and this value is sampled.")
	@PositiveOrZero
	private double minimumEnrouteChargingDuration = 3600.0;

	@Parameter
	@Comment("Maximum duration of enroute charging. A random value between the minimum and this value is sampled.")
	@PositiveOrZero
	private double maximumEnrouteChargingDuration = 3600.0;

	@Parameter
	@Comment("Euclidean search radius to find candidates for charging")
	@PositiveOrZero
	private double chargerSearchRadius = 1000.0;

	@Parameter
	@Comment("Defines the probability with which a charging plan is selected among the existing ones versus creating a new charging plan")
	@DecimalMin("0.0")
	@DecimalMax("1.0")
	private double selectionProbability = 0.8;

	@Parameter
	@Comment("Defines how many charging plans per regular plan can exist")
	@Positive
	private int maximumChargingPlans = 4;

	@Parameter
	@Comment("Defines the weight with which the charging score is added to the standard plan score")
	private double chargingScoreWeight = 0.0;

	public enum SelectionStrategy {
		Best, Exponential, Random
	}

	@Parameter
	@Comment("Defines the selection strategy for the charging plans")
	private SelectionStrategy selectionStrategy = SelectionStrategy.Exponential;

	public enum AlternativeSearchStrategy {
		Naive, OccupancyBased, ReservationBased
	}

	@Parameter
	@Comment("Defines the scaling factor for the exponential charging plan selection strategy")
	private double exponentialSelectionBeta = 0.1;

	@Parameter
	@Comment("Defines what to do when planned charger is not available. Naive: Select any other eligible charger nearby. Occupancy: Check whether there is at least a free spot upon departure. Reservation: Select among chargers that can be prebooked for the planned charging slot.")
	private AlternativeSearchStrategy onlineSearchStrategy = AlternativeSearchStrategy.Naive;

	@Parameter
	@Comment("Defines whether online search is applied proactively (when approaching a planned charging activity)")
	private boolean useProactiveOnlineSearch = true;

	@Parameter
	@Comment("Defines how often to write out detailed information about charging scoring")
	private int scoreTrackingInterval = 0;

	@Parameter
	@Comment("Maximum attempts (excluding the initial one) that an agent tries to find a charger before the search is aborted")
	private int maximumAlternatives = 2;

	@Parameter
	@Comment("Defines whether to precompute viable charger alternatives for each charging activity planned in an agent's plan. A value of -1 means no caching.")
	private int alternativeCacheSize = -1;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Verify.verify(maximumEnrouteChargingDuration >= minimumEnrouteChargingDuration);
		Verify.verifyNotNull(scoring, "Charging scoring parameters not found");

		if (alternativeCacheSize > -1) {
			Verify.verify(maximumAlternatives == 1,
					"When using alternative caching, only one alternative can be chosen, so maximumAlternatives must be set to 1!");
		}
	}

	public ChargingPlanScoringParameters getScoringParameters() {
		return scoring;
	}

	public ChargingCostsParameters getCostParameters() {
		return costs;
	}

	public double getMinimumActivityChargingDuration() {
		return minimumActivityChargingDuration;
	}

	public void setMinimumActivityChargingDuration(double minimumActivityChargingDuration) {
		this.minimumActivityChargingDuration = minimumActivityChargingDuration;
	}

	public double getMaximumActivityChargingDuration() {
		return maximumActivityChargingDuration;
	}

	public void setMaximumActivityChargingDuration(double maximumActivityChargingDuration) {
		this.maximumActivityChargingDuration = maximumActivityChargingDuration;
	}

	public double getMinimumEnrouteDriveTime() {
		return minimumEnrouteDriveTime;
	}

	public void setMinimumEnrouteDriveTime(double minimumEnrouteDriveTime) {
		this.minimumEnrouteDriveTime = minimumEnrouteDriveTime;
	}

	public double getMinimumEnrouteChargingDuration() {
		return minimumEnrouteChargingDuration;
	}

	public void setMinimumEnrouteChargingDuration(double minimumEnrouteChargingDuration) {
		this.minimumEnrouteChargingDuration = minimumEnrouteChargingDuration;
	}

	public double getMaximumEnrouteChargingDuration() {
		return maximumEnrouteChargingDuration;
	}

	public void setMaximumEnrouteChargingDuration(double maximumEnrouteChargingDuration) {
		this.maximumEnrouteChargingDuration = maximumEnrouteChargingDuration;
	}

	public double getChargerSearchRadius() {
		return chargerSearchRadius;
	}

	public void setChargerSearchRadius(double chargerSearchRadius) {
		this.chargerSearchRadius = chargerSearchRadius;
	}

	public double getSelectionProbability() {
		return selectionProbability;
	}

	public void setSelectionProbability(double selectionProbability) {
		this.selectionProbability = selectionProbability;
	}

	public int getMaximumChargingPlans() {
		return maximumChargingPlans;
	}

	public void setMaximumChargingPlans(int maximumChargingPlans) {
		this.maximumChargingPlans = maximumChargingPlans;
	}

	public double getChargingScoreWeight() {
		return chargingScoreWeight;
	}

	public void setChargingScoreWeight(double chargingScoreWeight) {
		this.chargingScoreWeight = chargingScoreWeight;
	}

	public SelectionStrategy getSelectionStrategy() {
		return selectionStrategy;
	}

	public void setSelectionStrategy(SelectionStrategy selectionStrategy) {
		this.selectionStrategy = selectionStrategy;
	}

	public double getExponentialSelectionBeta() {
		return exponentialSelectionBeta;
	}

	public void setExponentialSelectionBeta(double exponentialSelectionBeta) {
		this.exponentialSelectionBeta = exponentialSelectionBeta;
	}

	public AlternativeSearchStrategy getOnlineSearchStrategy() {
		return onlineSearchStrategy;
	}

	public void setOnlineSearchStrategy(AlternativeSearchStrategy onlineSearchStrategy) {
		this.onlineSearchStrategy = onlineSearchStrategy;
	}

	public boolean isUseProactiveOnlineSearch() {
		return useProactiveOnlineSearch;
	}

	public void setUseProactiveOnlineSearch(boolean useProactiveOnlineSearch) {
		this.useProactiveOnlineSearch = useProactiveOnlineSearch;
	}

	public int getScoreTrackingInterval() {
		return scoreTrackingInterval;
	}

	public void setScoreTrackingInterval(int scoreTrackingInterval) {
		this.scoreTrackingInterval = scoreTrackingInterval;
	}

	public int getMaximumAlternatives() {
		return maximumAlternatives;
	}

	public void setMaximumAlternatives(int maximumAlternatives) {
		this.maximumAlternatives = maximumAlternatives;
	}

	public int getAlternativeCacheSize() {
		return alternativeCacheSize;
	}

	public void setAlternativeCacheSize(int alternativeCacheSize) {
		this.alternativeCacheSize = alternativeCacheSize;
	}
}
