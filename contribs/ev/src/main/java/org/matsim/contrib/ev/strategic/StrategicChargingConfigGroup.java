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

	public ChargingPlanScoringParameters scoring;
	public ChargingCostsParameters costs;

	@Parameter
	@Comment("Minimum duration of the activity-based charging slots")
	@PositiveOrZero
	public double minimumActivityChargingDuration = 900.0;

	@Parameter
	@Comment("Maximum duration of the activity-based charging slots")
	@PositiveOrZero
	public double maximumActivityChargingDuration = Double.POSITIVE_INFINITY;

	@Parameter
	@Comment("Minimum drive duration at which charging along the route is considered")
	@PositiveOrZero
	public double minimumEnrouteDriveTime = 3600.0;;

	@Parameter
	@Comment("Minimum duration of enroute charging. A random value between the minimum and this value is sampled.")
	@PositiveOrZero
	public double minimumEnrouteChargingDuration = 3600.0;

	@Parameter
	@Comment("Maximum duration of enroute charging. A random value between the minimum and this value is sampled.")
	@PositiveOrZero
	public double maximumEnrouteChargingDuration = 3600.0;

	@Parameter
	@Comment("Euclidean search radius to find candidates for charging")
	@PositiveOrZero
	public double chargerSearchRadius = 1000.0;

	@Parameter
	@Comment("Defines the probability with which a charging plan is selected among the existing ones versus creating a new charging plan")
	@DecimalMin("0.0")
	@DecimalMax("1.0")
	public double selectionProbability = 0.8;

	@Parameter
	@Comment("Defines how many charging plans per regular plan can exist")
	@Positive
	public int maximumChargingPlans = 4;

	@Parameter
	@Comment("Defines the weight with which the charging score is added to the standard plan score")
	public double chargingScoreWeight = 0.0;

	public enum SelectionStrategy {
		Best, Exponential, Random
	}

	@Parameter
	@Comment("Defines the selection strategy for the charging plans")
	public SelectionStrategy selectionStrategy = SelectionStrategy.Exponential;

	public enum AlternativeSearchStrategy {
		Naive, OccupancyBased, ReservationBased
	}

	@Parameter
	@Comment("Defines the scaling factor for the exponential charging plan selection strategy")
	public double exponentialSelectionBeta = 0.1;

	@Parameter
	@Comment("Defines what to do when planned charger is not available. Naive: Select any other eligible charger nearby. Occupancy: Check whether there is at least a free spot upon departure. Reservation: Select among chargers that can be prebooked for the planned charging slot.")
	public AlternativeSearchStrategy onlineSearchStrategy = AlternativeSearchStrategy.Naive;

	@Parameter
	@Comment("Defines whether online search is applied proactively (when approaching a planned charging activity)")
	public boolean useProactiveOnlineSearch = true;

	@Parameter
	@Comment("Defines how often to write out detailed information about charging scoring")
	public int scoreTrackingInterval = 0;

	@Parameter
	@Comment("Maximum attempts (excluding the initial one) that an agent tries to find a charger before the search is aborted")
	int maximumAlternatives = 2;

	@Parameter
	@Comment("Defines whether to precompute viable charger alternatives for each charging activity planned in an agent's plan. A value of -1 means no caching.")
	int alternativeCacheSize = -1;

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
}
