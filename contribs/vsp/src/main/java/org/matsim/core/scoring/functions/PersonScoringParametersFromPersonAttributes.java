/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring.functions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tschlenther / vsp-gleich
 * <p>
 * This class is an extension of {@link playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters}
 * which is an adoption of {@link org.matsim.core.scoring.functions.SubpopulationScoringParameters}.
 * This class additionaly allows for person-specific mode scoring parameters (for now ASC only) and marginalUtilityOfMoney.
 * In order to use this, you need to provide the respective attributes (otherwise default values for the subpopulation
 * are used). The person specific mode parameters are interpreted as offset added to the subpopulation's parameters.
 * For marginalUtilityOfMoney an attribute {@link org.matsim.core.population.PersonUtils#getIncome(Person)} for persons that have a specific
 * income is used. Persons in the population, that have no attribute {@link org.matsim.core.population.PersonUtils#getIncome(Person)} will use the
 * default marginal utility set in their subpopulation's scoring parameters.
 * <p>
 * The person specific marginal utility is computed by subpopulationMgnUtilityOfMoney * AVERAGE_INCOME / PERSONAL_INCOME
 * where
 * AVERAGE_INCOME is computed as the average of all values of the attribute {@link org.matsim.core.population.PersonUtils#getIncome(Person)} that are contained in the population.
 * <p>
 * If you want to distinguish between 'rich' areas and 'poor' areas, make use of the subpopulation feature and set subpopulation-specific mgnUtilityOfMoney in
 * the #PlanCalcScoreConfigGroup
 */
public class PersonScoringParametersFromPersonAttributes implements ScoringParametersForPerson {
    Logger log = LogManager.getLogger(PersonScoringParametersFromPersonAttributes.class);

    private final ScoringConfigGroup config;
    private final ScenarioConfigGroup scConfig;
    private final TransitConfigGroup transitConfigGroup;
    private final Map<Id<Person>, ScoringParameters> params = new IdMap<>(Person.class);
    private final double globalAvgIncome;
    private final Map<String, Map<String, ActivityUtilityParameters>> activityParamsPerSubpopulation = new ConcurrentHashMap<>();

    @Inject
    PersonScoringParametersFromPersonAttributes(Population population, ScoringConfigGroup scoringConfigGroup, ScenarioConfigGroup scenarioConfigGroup, TransitConfigGroup transitConfigGroup) {
        this.config = scoringConfigGroup;
        this.scConfig = scenarioConfigGroup;
        this.transitConfigGroup = transitConfigGroup;
        this.globalAvgIncome = computeAvgIncome(population);
    }

    private double computeAvgIncome(Population population) {
        log.info("reading income attribute using " + PersonUtils.class + " of all agents and compute global average.\n" +
                "Make sure to set this attribute only to appropriate agents (i.e. true 'persons' and not freight agents) \n" +
                "Income values <= 0 are ignored. Agents that have negative or 0 income will use the marginalUtilityOfMOney in their subpopulation's scoring params..");
        OptionalDouble averageIncome = population.getPersons().values().stream()
                .filter(person -> PersonUtils.getIncome(person) != null) //consider only agents that have a specific income provided
                .mapToDouble(PersonUtils::getIncome)
                .filter(dd -> dd > 0)
                .average();

        if (averageIncome.isEmpty()) {
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
        // (as so often, this functionality is not attached to the person (i.e. person. get...), but "wrapped around" it (i.e. get...(person)). kai, apr'22)

        // We check if we have this cached ...
        ScoringParameters scoringParametersForThisPerson = params.get(person.getId());

        // ... if not, it is generated:
        if (scoringParametersForThisPerson == null) {
            final String subpopulation = PopulationUtils.getSubpopulation(person) == null ? "default" : PopulationUtils.getSubpopulation(person) ;
            // (weird, but so be it.  kai, apr'22)

            //the following is a comment that was orinally put into SubpopulationScoringParams, which is the template for this class...
            /* lazy initialization of params. not strictly thread safe, as different threads could
             * end up with different params-object, although all objects will have the same
             * values in them due to using the same config. Still much better from a memory performance
             * point of view than giving each ScoringFunction its own copy of the params.
             */

            ScoringConfigGroup.ScoringParameterSet subpopulationScoringParams = this.config.getScoringParameters(subpopulation);
            // (we can set scoring params per subpopulation, so retrieve them as starting point.  kai, apr'22)

            // save the activityParams of the subpopulation so we need to build them only once.
            this.activityParamsPerSubpopulation.computeIfAbsent(subpopulation, k -> {
                Map<String, ActivityUtilityParameters> activityParams = new TreeMap<>();
                for (ScoringConfigGroup.ActivityParams params : subpopulationScoringParams.getActivityParams()) {
                    ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params);
                    activityParams.put(params.getActivityType(), factory.build());
                }
                return activityParams;
            });
            // (I think that this is just an adapter class, converting the representation in the config to some internal representation.  kai, apr'22)

            //use the builder that does not make defensive copies of the activity params
            ScoringParameters.Builder builder = new ScoringParameters.Builder(this.config, subpopulationScoringParams, this.activityParamsPerSubpopulation.get(subpopulation), scConfig);
            // (odd that this now needs both the version from the config and the version from the adapter class.  kai, apr'22)

            if (transitConfigGroup.isUseTransit()) {
                // this is the PT stage activity:
                ScoringConfigGroup.ActivityParams transitActivityParams = new ScoringConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
                transitActivityParams.setTypicalDuration(120.0);
                transitActivityParams.setOpeningTime(0.);
                transitActivityParams.setClosingTime(0.);
                ActivityUtilityParameters.Builder modeParamsBuilder = new ActivityUtilityParameters.Builder(transitActivityParams);
                modeParamsBuilder.setScoreAtAll(false);
                builder.setActivityParameters(PtConstants.TRANSIT_ACTIVITY_TYPE, modeParamsBuilder.build());
            }

            if (PersonUtils.getIncome(person) != null) {
                //here is where we put person-specific stuff
                double personalIncome = PersonUtils.getIncome(person);
                // (getIncome returns Double, i.e. "null" is an option.  Not sure what happens if this is now converted to the primitive type.  kai, apr'22)

                if (personalIncome > 0) {
                    builder.setMarginalUtilityOfMoney(subpopulationScoringParams.getMarginalUtilityOfMoney() * globalAvgIncome / personalIncome);
                } else {
                    // (not sure what this means.  "null" would have been an option, but is made impossible, see above.  An income of 0 may be possible, but with the 1/y that we often use it should be avoided.)
                    log.warn("you have set income to " + personalIncome + " for person " + person + ". This is invalid and gets ignored." +
                            "Instead, the marginalUtilityOfMoney is derived from the subpopulation's scoring parameters.");
                }
            }

            Map<String, String> personalScoringModeConstants = PersonUtils.getModeConstants(person);
            if (personalScoringModeConstants != null) {
                for (Map.Entry<String, String> entry: personalScoringModeConstants.entrySet()) {
					ScoringConfigGroup.ModeParams subpopulationModeParams = subpopulationScoringParams.getModes().get(entry.getKey());
					ModeUtilityParameters.Builder modeUtilityParamsBuilder = new ModeUtilityParameters.Builder();
                    try {
                        modeUtilityParamsBuilder.setConstant(Double.parseDouble(entry.getValue()) +
							subpopulationModeParams.getConstant());
                    } catch (NumberFormatException e) {
                        log.error("PersonalScoringModeConstants from person attribute could not be parsed for person " +
                                person.getId().toString() + ".");
                        throw new RuntimeException(e);
                    }

                    // copy other params from subpopulation config
                    modeUtilityParamsBuilder.setMarginalUtilityOfTraveling_s(subpopulationModeParams.getMarginalUtilityOfTraveling());
                    modeUtilityParamsBuilder.setMarginalUtilityOfDistance_m(subpopulationModeParams.getMarginalUtilityOfDistance());
                    modeUtilityParamsBuilder.setMonetaryDistanceRate(subpopulationModeParams.getMonetaryDistanceRate());
                    modeUtilityParamsBuilder.setDailyMoneyConstant(subpopulationModeParams.getDailyMonetaryConstant());
                    modeUtilityParamsBuilder.setDailyUtilityConstant(subpopulationModeParams.getDailyUtilityConstant());

                    ModeUtilityParameters modeUtilityParameters = modeUtilityParamsBuilder.build();
                    builder.setModeParameters(entry.getKey(), modeUtilityParameters);
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
