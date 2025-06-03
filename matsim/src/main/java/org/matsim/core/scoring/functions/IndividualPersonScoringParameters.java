package org.matsim.core.scoring.functions;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides individual scoring parameters for each person. Used by {@link TasteVariationsConfigParameterSet}
 */
public class IndividualPersonScoringParameters implements ScoringParametersForPerson {

	private static final Logger log = LogManager.getLogger(IndividualPersonScoringParameters.class);

	/**
	 * Cache instances of {@link ActivityUtilityParameters} for each subpopulation.
	 */
	private final Map<String, Map<String, ActivityUtilityParameters>> actUtils = new ConcurrentHashMap<>();

	/**
	 * Average income of all agents for each subpopulation.
	 */
	private final Object2DoubleMap<String> avgIncome = new Object2DoubleOpenHashMap<>();

	/**
	 * Cache instances of {@link ScoringParameters} for each person.
	 */
	private final IdMap<Person, ScoringParameters> cache;

	private final Scenario scenario;
	private final ScoringConfigGroup basicScoring;
	private final TransitConfigGroup transitConfig;

	@Inject
	public IndividualPersonScoringParameters(Scenario scenario) {
		this.scenario = scenario;
		this.basicScoring = scenario.getConfig().scoring();
		this.transitConfig = scenario.getConfig().transit();
		this.cache = new IdMap<>(Person.class, scenario.getPopulation().getPersons().size());
	}

	private double computeAvgIncome(Population population, String subpopulation) {
		log.info("reading income attribute using {} of all agents and compute global average.\nMake sure to set this attribute only to appropriate agents (i.e. true 'persons' and not freight agents) \nIncome values <= 0 are ignored. Agents that have negative or 0 income will use the marginalUtilityOfMoney in their subpopulation's scoring params..", PersonUtils.class);
		OptionalDouble averageIncome = population.getPersons().values().stream()
			//consider only agents that have a specific income provided
			.filter(person -> PersonUtils.getIncome(person) != null)
			.filter(person -> Objects.equals(PopulationUtils.getSubpopulation(person), subpopulation))
			.mapToDouble(PersonUtils::getIncome)
			.filter(dd -> dd >= 0)
			.average();

		log.info("average income for {} is {}", subpopulation, averageIncome);

		if (averageIncome.isEmpty()) {
			throw new RuntimeException("you have enabled income dependent scoring but there is not a single income attribute in the population! " +
				"If you are not aiming for person-specific marginalUtilityOfMoney, better use other PersonScoringParams, e.g. SubpopulationPersonScoringParams, which have higher performance." +
				"Otherwise, please provide income attributes in the population...");
		} else {
			return averageIncome.getAsDouble();
		}
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {

		return this.cache.computeIfAbsent(person.getId(), id -> {

			String subpopulation = PopulationUtils.getSubpopulation(person);
			ScoringConfigGroup.ScoringParameterSet scoringParameters = Objects.requireNonNull(basicScoring.getScoringParameters(subpopulation), () -> "No scoring parameters found for subpopulation " + subpopulation);

			// For the act utils cache the subpopulation can not be null
			String subPopKey = subpopulation == null ? "__null__" : subpopulation;

			// Activity params can be reused per subpopulation
			Map<String, ActivityUtilityParameters> activityParams = actUtils.computeIfAbsent(subPopKey, k -> {
				Map<String, ActivityUtilityParameters> ap = new TreeMap<>();
				for (ScoringConfigGroup.ActivityParams params : scoringParameters.getActivityParams()) {
					ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params);
					ap.put(params.getActivityType(), factory.build());
				}

				// The code to add this activity type is always copied between different scoring implementations
				// it might not be actually needed anymore (because default staging activities are also added elsewhere)
				// but it's not clear if it's safe to remove it.
				if (transitConfig.isUseTransit()) {
					ScoringConfigGroup.ActivityParams transitActivityParams = new ScoringConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
					transitActivityParams.setTypicalDuration(120.0);
					transitActivityParams.setOpeningTime(0.);
					transitActivityParams.setClosingTime(0.);
					ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
					modeParamsBuilder.setScoreAtAll(false);
					ap.put(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder.build());
				}

				return ap;
			});

			ScoringParameters.Builder builder = new ScoringParameters.Builder(basicScoring,
				scoringParameters, activityParams, scenario.getConfig().scenario());

			TasteVariationsConfigParameterSet tasteVariationsParams = scoringParameters.getTasteVariationsParams();

			// If there is no taste variation config the parameters are ready
			if (tasteVariationsParams == null)
				return builder.build();

			// Ignore subpopulation if specified
			if (tasteVariationsParams.getExcludeSubpopulations() != null &&
				tasteVariationsParams.getExcludeSubpopulations().contains(subPopKey)) {
				return builder.build();
			}

			Double personalIncome = PersonUtils.getIncome(person);
			double refIncome = tasteVariationsParams.getIncomeExponent() > 0 ? avgIncome.computeIfAbsent(subPopKey, (k) -> computeAvgIncome(scenario.getPopulation(), subpopulation)) : Double.NaN;

			// Income dependent scoring might be disabled
			if (!Double.isNaN(refIncome) && personalIncome != null) {
				if (personalIncome != 0) {
					builder.setMarginalUtilityOfMoney(scoringParameters.getMarginalUtilityOfMoney() *
						Math.pow(refIncome / personalIncome, tasteVariationsParams.getIncomeExponent()));
				} else {
					log.warn("You have set income to {} for person {}. This is invalid and gets ignored.Instead, the marginalUtilityOfMoney is derived from the subpopulation's scoring parameters.", personalIncome, person);
				}
			}

			// There are no further variations in the utilities
			if (tasteVariationsParams.getVariationsOf().isEmpty())
				return builder.build();

			Map<String, Map<ModeUtilityParameters.Type, Double>> modeTasteVariations = PersonUtils.getModeTasteVariations(person);

			if (modeTasteVariations == null || modeTasteVariations.isEmpty()) {
				throw new IllegalStateException("Person " + person.getId() + " does not have any mode taste variations. Either provide them or remove the taste variations config.");
			}

			for (Map.Entry<String, Map<ModeUtilityParameters.Type, Double>> kv : modeTasteVariations.entrySet()) {
				String mode = kv.getKey();

				ModeUtilityParameters.Builder modeParameters = new ModeUtilityParameters.Builder(builder.getModeParameters(mode));

				Map<ModeUtilityParameters.Type, Double> variations = kv.getValue();

				for (Map.Entry<ModeUtilityParameters.Type, Double> variation : variations.entrySet()) {
					ModeUtilityParameters.Type type = variation.getKey();
					Double value = variation.getValue();

					switch (type) {
						case constant:
							modeParameters.setConstant(modeParameters.getConstant() + value);
							break;
						case dailyUtilityConstant:
							modeParameters.setDailyUtilityConstant(modeParameters.getDailyUtilityConstant() + value);
							break;
						case dailyMoneyConstant:
							modeParameters.setDailyMoneyConstant(modeParameters.getDailyMoneyConstant() + value);
							break;
						case monetaryDistanceCostRate:
							modeParameters.setMonetaryDistanceRate(modeParameters.getMonetaryDistanceRate() + value);
							break;
						case marginalUtilityOfDistance_m:
							modeParameters.setMarginalUtilityOfDistance_m(modeParameters.getMarginalUtilityOfDistance_m() + value);
							break;
						case marginalUtilityOfTraveling_s:
							modeParameters.setMarginalUtilityOfTraveling_s(modeParameters.getMarginalUtilityOfTraveling_s() + value);
							break;
						default:
							throw new IllegalStateException("Unexpected value: " + type);
					}
				}

				builder.setModeParameters(mode, modeParameters.build());

			}

			return builder.build();
		});
	}
}
