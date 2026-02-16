package org.matsim.contrib.ev.strategic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.strategic.access.SubscriptionRegistry;
import org.matsim.contrib.ev.strategic.analysis.ChargerTypeAnalysisListener;
import org.matsim.contrib.ev.strategic.costs.AttributeBasedChargingCostCalculator;
import org.matsim.contrib.ev.strategic.costs.DynamicEnergyCosts;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostCalculator;
import org.matsim.contrib.ev.strategic.infrastructure.FacilityChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.PersonChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.PublicChargerProvider;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.strategic.plan.ChargingPlansConverter;
import org.matsim.contrib.ev.strategic.replanning.StrategicChargingReplanningStrategy;
import org.matsim.contrib.ev.strategic.replanning.innovator.RandomChargingPlanInnovator;
import org.matsim.contrib.ev.strategic.reservation.StrategicChargingReservationEngine;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoringParameters;
import org.matsim.contrib.ev.withinday.WithinDayChargingStrategy;
import org.matsim.contrib.ev.withinday.WithinDayEvConfigGroup;
import org.matsim.contrib.ev.withinday.WithinDayEvEngine;
import org.matsim.contrib.ev.withinday.WithinDayEvModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.vehicles.Vehicle;

/**
 * This is a convenience class that gives a central access point to attributes
 * that may need to be prepared in the scenario when using strategic charging.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingUtils {
    private StrategicChargingUtils() {
    }

    /**
     * Adds a subscription to the person attributes. To be used with
     * AttributeBasedChargerAccess.
     */
    static public void addSubscription(Person person, String subscription) {
        SubscriptionRegistry.addSubscription(person, subscription);
    }

    /**
     * Returns the subscriptions of a person
     */
    static public Set<String> getSubscriptions(Person person) {
        return SubscriptionRegistry.getSubscriptions(person);
    }

    /**
     * Adds a subscription to the charger attributes. To be used with
     * AttributeBasedChargerAccess.
     */
    static public void addSubscription(ChargerSpecification charger, String subscription) {
        SubscriptionRegistry.addSubscription(charger, subscription);
    }

    /**
     * Returns the required subscriptions for a charger
     */
    static public Set<String> getSubscriptions(ChargerSpecification charger) {
        return SubscriptionRegistry.getSubscriptions(charger);
    }

    /**
     * Adds an analysis type to a charger
     */
    static public void addAnalysisType(ChargerSpecification charger, String analysisType) {
        ChargerTypeAnalysisListener.addAnalysisType(charger, analysisType);
    }

    /**
     * Returns the list of analysis types of a charger
     */
    static public Set<String> getAnalysisTypes(ChargerSpecification charger) {
        return ChargerTypeAnalysisListener.getAnalysisTypes(charger);
    }

    /**
     * Sets the cost structure for the charger.
     */
    static public void setChargingCosts(ChargerSpecification charger, double costPerUse, double costPerEnergy_kWh,
            double costPerDuration_min) {
        AttributeBasedChargingCostCalculator.setCostPerUse(charger, costPerUse);
        AttributeBasedChargingCostCalculator.setCostPerEnergy_kWh(charger, costPerEnergy_kWh);
        AttributeBasedChargingCostCalculator.setCostPerDuration_min(charger, costPerDuration_min);
    }

    /**
     * Sets the cost structure for the charger.
     */
    static public void setChargingCosts(ChargerSpecification charger, double costPerUse, double costPerEnergy_kWh,
            double costPerDuration_min, double costPerBlockingDuration_min, double blockingDuration_min,
            double costPerReservation) {
        AttributeBasedChargingCostCalculator.setChargingCosts(charger, costPerUse, costPerEnergy_kWh,
                costPerDuration_min, costPerBlockingDuration_min, blockingDuration_min, costPerReservation);
    }

    /**
     * The blocking costs are charged
     * additionally for any duration that exceeds the blocking duration.
     */
    static public void setBlockingCosts(ChargerSpecification charger, double costPerBlockingDuration_min,
            double blockingDuration_min) {
        AttributeBasedChargingCostCalculator.setCostPerBlockingDuration_min(charger, costPerBlockingDuration_min);
        AttributeBasedChargingCostCalculator.setBlockingDuration_min(charger, blockingDuration_min);
    }

    /**
     * The dynamic costs are added to the base costs of the charger.
     */
    static public void setDynamicCosts(ChargerSpecification charger, DynamicEnergyCosts dynamicCosts) {
        AttributeBasedChargingCostCalculator.setDynamicEnergyCost_kWh(charger, dynamicCosts);
    }

    /**
     * Adds a tariff to a charger.
     */
    static public void addTariff(ChargerSpecification charger, String tariff) {
        TariffBasedChargingCostCalculator.addTariff(charger, tariff);
    }

    /**
     * Retrieve the list of tariffs for a charger.
     */
    static public Set<String> getTariffs(ChargerSpecification charger) {
        return TariffBasedChargingCostCalculator.getTariffs(charger);
    }

    /**
     * Sets the minimum SoC under which a person doesn't want to fall.
     */
    static public void setMinimumSoc(Person person, double minimumSoc) {
        ChargingPlanScoring.setMinimumSoc(person, minimumSoc);
    }

    /**
     * Returns the minimum SoC under which a person doesn't want to fall.
     */
    static public Double getMinimumSoc(Person person) {
        return ChargingPlanScoring.getMinimumSoc(person);
    }

    /**
     * Sets the minimum SoC under which a person doesn't want to be at the end of
     * the day.
     */
    static public void setMinimumEndSoc(Person person, double minimumEndSoc) {
        ChargingPlanScoring.setMinimumEndSoc(person, minimumEndSoc);
    }

    /**
     * Returns the minimum SoC under which a person doesn't want to be at the end of
     * the day.
     */
    static public Double getMinimumEndSoc(Person person) {
        return ChargingPlanScoring.getMinimumEndSoc(person);
    }

    /**
     * Sets the target SoC that the person wants to achieve at the end of the day.
     */
    static public void setTargetSoc(Person person, double targetSoc) {
        ChargingPlanScoring.setTargetSoc(person, targetSoc);
    }

    /**
     * Returns the target SoC that the person wants to achieve at the end of the
     * day.
     */
    static public Double getTargetSoc(Person person) {
        return ChargingPlanScoring.getTargetSoc(person);
    }

    /**
     * Returns whether a charger is assigned to at least one facility.
     */
    static public boolean isFacilityCharger(ChargerSpecification charger) {
        return FacilityChargerProvider.isFacilityCharger(charger);
    }

    /**
     * Sets the facilities to which a charger is assigned.
     */
    static public void assignChargerFacilities(ChargerSpecification charger,
            Collection<Id<ActivityFacility>> facilityIds) {
        FacilityChargerProvider.setFacilityIds(charger, facilityIds);
    }

    /**
     * Returns the facilities to which a charger is assigned.
     */
    static public Set<Id<ActivityFacility>> getChargerFacilities(ChargerSpecification charger) {
        return FacilityChargerProvider.getFacilityIds(charger);
    }

    /**
     * Returns whether a charger is assigned to at least one person.
     */
    static public boolean isPersonCharger(ChargerSpecification charger) {
        return PersonChargerProvider.isPersonCharger(charger);
    }

    /**
     * Sets the persons to which a charger is assigned.
     */
    static public void assignChargerPersons(ChargerSpecification charger, Collection<Id<Person>> personIds) {
        PersonChargerProvider.setPersonIds(charger, personIds);
    }

    /**
     * Returns the persons to which a charger is assigned.
     */
    static public Set<Id<Person>> getChargerPersons(ChargerSpecification charger) {
        return PersonChargerProvider.getPersonIds(charger);
    }

    /**
     * Return whether a charger is public.
     */
    static public boolean isPublicCharger(ChargerSpecification charger) {
        return PublicChargerProvider.isPublicCharger(charger);
    }

    /**
     * Sets a charge public or not.
     */
    static public void assignPublic(ChargerSpecification charger, boolean isPublic) {
        PublicChargerProvider.setPublic(charger, isPublic);
    }

    /**
     * Sets the critical SoC for an agent.
     */
    static public void setCriticalSoC(Person person, double criticalSoc) {
        CriticalAlternativeProvider.setCriticalSoC(person, criticalSoc);
    }

    /**
     * Retrieves the critical SoC from an agent.
     */
    static public Double getCriticalSoc(Person person) {
        return CriticalAlternativeProvider.getCriticalSoc(person);
    }

    /**
     * Returns the maximum SoC of a vehicle.
     */
    static public Double getMaximumSoc(Vehicle vehicle) {
        return WithinDayChargingStrategy.getMaximumSoc(vehicle);
    }

    /**
     * Sets the maximum SoC of a vehicle.
     */
    static public void setMaximumSoc(Vehicle vehicle, double maximumSoc) {
        WithinDayChargingStrategy.setMaximumSoc(vehicle, maximumSoc);
    }

    /**
     * Adds the activity scoring parameters to the 'normal' MATSim scoring config
     * group.
     */
    static public void configureScoring(Config config) {
        for (String activityType : Arrays.asList(WithinDayEvEngine.PLUG_ACTIVITY_TYPE,
                WithinDayEvEngine.UNPLUG_ACTIVITY_TYPE, WithinDayEvEngine.ACCESS_ACTIVITY_TYPE,
                WithinDayEvEngine.WAIT_ACTIVITY_TYPE)) {
            ActivityParams activityParams = new ActivityParams(activityType);
            activityParams.setScoringThisActivityAtAll(false);
            config.scoring().addActivityParams(activityParams);
        }
    }

    /**
     * Sets up configuration for a SEVC simulation.
     * 
     * - Agents still need to be activated using the `WithinDayEvUtils.activate`
     * method.
     * - Replanning still needs to be configured (see the helper methods)
     */
    static public void configure(Config config) {
        // make sure that config groups are present
        EvConfigGroup.get(config, true);
        WithinDayEvConfigGroup.get(config, true);
        StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(config, true);

        // sensible defaults for scoring parameters
        ChargingPlanScoringParameters scoringParameters = new ChargingPlanScoringParameters();
        sevcConfig.addParameterSet(scoringParameters);

        // sensible defaults for innovator parameters
        RandomChargingPlanInnovator.Parameters innovationParameters = new RandomChargingPlanInnovator.Parameters();
        sevcConfig.addParameterSet(innovationParameters);

        StrategySettings sevcStrategy = new StrategySettings();
        sevcStrategy.setStrategyName(StrategicChargingReplanningStrategy.STRATEGY);
        sevcStrategy.setWeight(0.05);
        config.replanning().addStrategySettings(sevcStrategy);

        configureScoring(config);
    }

    /**
     * Adds a replanning startegy to the given subpopulation
     */
    static public void configureDefaultReplanning(Config config, String subpopulation, double weight) {
        StrategySettings sevcStrategy = new StrategySettings();
        sevcStrategy.setStrategyName(StrategicChargingReplanningStrategy.STRATEGY);
        sevcStrategy.setWeight(weight);
        sevcStrategy.setSubpopulation(subpopulation);
        config.replanning().addStrategySettings(sevcStrategy);
    }

    /**
     * Adds a replanning strategy
     */
    static public void configureDefaultReplanning(Config config, double weight) {
        configureDefaultReplanning(config, null, weight);
    }

    /**
     * Sets up the subpopulation for standalone strategic charging without any other
     * innovation strategy
     */
    static public void configureStandaloneReplanning(Config config, String subpopulation) {
        // clean up
        List<StrategySettings> remove = new LinkedList<>();

        for (StrategySettings settings : config.replanning().getStrategySettings()) {
            if (settings.getSubpopulation() == subpopulation) {
                remove.add(settings);
            }
        }

        remove.forEach(config.replanning()::removeParameterSet);

        // add new
        StrategySettings sevcStrategy = new StrategySettings();
        sevcStrategy.setStrategyName(StrategicChargingReplanningStrategy.STRATEGY);
        sevcStrategy.setWeight(1.0);
        sevcStrategy.setSubpopulation(subpopulation);
        config.replanning().addStrategySettings(sevcStrategy);
    }

    /**
     * Sets up standalone strategic charging without any other innovation strategy
     */
    static public void configureStandaloneReplanning(Config config) {
        configureStandaloneReplanning(config, null);
    }

    /**
     * Sets up the controller
     */
    static public void configureController(Controler controller, boolean addEvModule) {
        if (addEvModule) {
            controller.addOverridingModule(new EvModule());
        }

        controller.addOverridingModule(new WithinDayEvModule());
        controller.addOverridingModule(new StrategicChargingModule());
    }

    /**
     * Sets up the controller
     */
    static public void configureController(Controler controller) {
        configureController(controller, true);
    }

    /**
     * Helper function that reads a list of string items from an attribute.
     */
    public static Set<String> readList(Attributable source, String attribute) {
        String value = (String) source.getAttributes().getAttribute(attribute);

        if (value == null) {
            return new HashSet<>();
        } else {
            return new HashSet<>(Arrays.stream(value.split(",")).collect(Collectors.toSet()));
        }
    }

    /**
     * Helper function that writes a list of string items to an attribute.
     */
    public static void writeList(Attributable target, String attribute, Set<String> items) {
        target.getAttributes().putAttribute(attribute, String.join(",", items));
    }

    /**
     * Sets the duration at which reservations for chargers are made in advance by
     * that person.
     */
    static public void setReservationSlack(Person person, double slack) {
        StrategicChargingReservationEngine.setReservationSlack(person, slack);
    }

    /**
     * Gets the duration at which reservations for chargers are made in advance by
     * that person.
     */
    static public Double getReservationSlack(Person person) {
        return StrategicChargingReservationEngine.getReservationSlack(person);
    }

    /**
     * Helper to obtain a map of attribute converters
     */
    static public Map<Class<?>, AttributeConverter<?>> getAttributeConverters() {
        return Collections.singletonMap(ChargingPlans.class, new ChargingPlansConverter());
    }

    /**
     * Helper that wraps around ScenarioUtils.loadScenario
     */
    static public Scenario loadScenario(Config config, Map<Class<?>, AttributeConverter<?>> converters) {
        Map<Class<?>, AttributeConverter<?>> extendedConverters = new HashMap<>();
        extendedConverters.putAll(getAttributeConverters());
        extendedConverters.putAll(converters);

        return ScenarioUtils.loadScenario(config, extendedConverters);
    }

    /**
     * Helper that wraps around ScenarioUtils.loadScenario
     */
    static public Scenario loadScenario(Scenario scenario, Map<Class<?>, AttributeConverter<?>> converters) {
        return ScenarioUtils.loadScenario(scenario.getConfig(),
                Collections.singletonMap(ChargingPlans.class, new ChargingPlansConverter()));
    }

    /**
     * Helper that wraps around ScenarioUtils.loadScenario
     */
    static public Scenario loadScenario(Config config) {
        return loadScenario(config, Collections.emptyMap());
    }

    /**
     * Helper that wraps around ScenarioUtils.loadScenario
     */
    static public Scenario loadScenario(Scenario scenario) {
        return loadScenario(scenario, Collections.emptyMap());
    }
}
