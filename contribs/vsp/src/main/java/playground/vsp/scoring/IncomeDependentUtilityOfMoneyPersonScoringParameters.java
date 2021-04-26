/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.vsp.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;

/**
 * @author tschlenther
 *
 * This class is an adoption of {@link org.matsim.core.scoring.functions.SubpopulationScoringParameters}.
 * It additionaly allows for person-specific marginalUtilityOfMoney.
 * In order to use this, you need to provide an attribute {@link #PERSONAL_INCOME_ATTRIBUTE_NAME} for persons that have
 * a specific income. Persons in the population, that have no attribute {@link #PERSONAL_INCOME_ATTRIBUTE_NAME} will use the
 * default marginal utility set in their subpopulation's scoring parameters.
 *
 * The person specific marginal utility is computed by subpopulationMgnUtilityOfMoney * AVERAGE_INCOME / PERSONAL_INCOME
 * where
 * AVERAGE_INCOME is computed as the average of all values of the attribute {@link #PERSONAL_INCOME_ATTRIBUTE_NAME} that are contained in the population.
 *
 * If you want to distinguish between 'rich' areas and 'poor' areas, make use of the subpopulation feature and set subpopulation-specific mgnUtilityOfMoney in
 * the #PlanCalcScoreConfigGroup
 */
public class IncomeDependentUtilityOfMoneyPersonScoringParameters implements ScoringParametersForPerson {
	Logger log = Logger.getLogger(IncomeDependentUtilityOfMoneyPersonScoringParameters.class);

	public static final String PERSONAL_INCOME_ATTRIBUTE_NAME = "income";

	private final PlanCalcScoreConfigGroup config;
	private final ScenarioConfigGroup scConfig;
	private final TransitConfigGroup transitConfigGroup;
	private final Map<Id<Person>, ScoringParameters> params = new IdMap<>(Person.class);
	private final double globalAvgIncome;
	private Map<String, Map<String, ActivityUtilityParameters>> activityParamsPerSubpopulation = new HashMap<>();

	@Inject
	IncomeDependentUtilityOfMoneyPersonScoringParameters(Population population, PlanCalcScoreConfigGroup planCalcScoreConfigGroup, ScenarioConfigGroup scenarioConfigGroup, TransitConfigGroup transitConfigGroup) {
		this.config = planCalcScoreConfigGroup;
		this.scConfig = scenarioConfigGroup;
		this.transitConfigGroup = transitConfigGroup;
		this.globalAvgIncome = computeAvgIncome(population);
	}

	private double computeAvgIncome(Population population) {
		log.info("read income attribute '" + PERSONAL_INCOME_ATTRIBUTE_NAME + "' of all agents and compute global average.\n" +
				"Make sure to set this attribute only to appropriate agents (i.e. true 'persons' and not freight agents) \n" +
				"Income values <= 0 are ignored. Agents that have negative or 0 income will use the marginalUtilityOfMOney in their subpopulation's scoring params..");
		OptionalDouble averageIncome =  population.getPersons().values().stream()
				.filter(person -> person.getAttributes().getAttribute(PERSONAL_INCOME_ATTRIBUTE_NAME) != null) //consider only agents that have a specific income provided
				.mapToDouble(person -> (double) person.getAttributes().getAttribute(PERSONAL_INCOME_ATTRIBUTE_NAME))
				.filter(dd -> dd > 0)
				.average();

		if(! averageIncome.isPresent()){
			throw new RuntimeException("you are using " + this.getClass() + " but there is not a single income attribute in the population! " +
					"If you are not aiming for person-specific marginalUtilityOfMOney, better use other PersonScoringParams, e.g. SUbpopulationPersonScoringParams, which have higher performance." +
					"Otherwise, please provide income attributes in the population...");
		} else {
			log.info("global average income is " + averageIncome);
			return averageIncome.getAsDouble();
		}
	}

	@Override
	public ScoringParameters getScoringParameters(Person person) {

		ScoringParameters scoringParametersForThisPerson = params.get(person.getId());
		if (scoringParametersForThisPerson == null) {
			final String subpopulation = PopulationUtils.getSubpopulation( person );

			//the following is a comment that was orinally put into SubpopulationScoringParams, which is the template for this class...
			/* lazy initialization of params. not strictly thread safe, as different threads could
			 * end up with different params-object, although all objects will have the same
			 * values in them due to using the same config. Still much better from a memory performance
			 * point of view than giving each ScoringFunction its own copy of the params.
			 */

			PlanCalcScoreConfigGroup.ScoringParameterSet subpopulationScoringParams = this.config.getScoringParameters(subpopulation);

			// save the activityParams of the subpopulation so we need to build them only once.
			this.activityParamsPerSubpopulation.computeIfAbsent(subpopulation, k -> {
				Map<String, ActivityUtilityParameters> activityParams = new TreeMap<>();
				for (PlanCalcScoreConfigGroup.ActivityParams params : subpopulationScoringParams.getActivityParams()) {
					ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params) ;
					activityParams.put(params.getActivityType(), factory.build() ) ;
				}
				return activityParams;
			});

			//use the builder that does not make defensive copies of the activity params
			ScoringParameters.Builder builder = new ScoringParameters.Builder(this.config, subpopulationScoringParams, this.activityParamsPerSubpopulation.get(subpopulation), scConfig);

			if (transitConfigGroup.isUseTransit()) {

				PlanCalcScoreConfigGroup.ActivityParams transitActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
				transitActivityParams.setTypicalDuration(120.0);
				transitActivityParams.setOpeningTime(0.) ;
				transitActivityParams.setClosingTime(0.) ;
				ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
				modeParamsBuilder.setScoreAtAll(false);
				builder.setActivityParameters(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder.build());
			}

			if (person.getAttributes().getAttribute(PERSONAL_INCOME_ATTRIBUTE_NAME) != null){
				//here is where we put person-specific stuff
				double personalIncome = (double) person.getAttributes().getAttribute(PERSONAL_INCOME_ATTRIBUTE_NAME);
				if(personalIncome > 0){
					builder.setMarginalUtilityOfMoney(subpopulationScoringParams.getMarginalUtilityOfMoney()  * globalAvgIncome / personalIncome);
				} else {
					log.warn("you have set income to " + personalIncome + " for person " + person + ". This is invalid and gets ignored." +
							"Instead, the marginalUtilityOfMoney is derived from the subpopulation's scoring parameters.");
				}
			}

			scoringParametersForThisPerson = builder.build();
			this.params.put(
					person.getId(),
					scoringParametersForThisPerson);
		}

		return scoringParametersForThisPerson;
	}
}
