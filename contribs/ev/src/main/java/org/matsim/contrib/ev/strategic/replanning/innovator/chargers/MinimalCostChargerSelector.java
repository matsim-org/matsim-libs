package org.matsim.contrib.ev.strategic.replanning.innovator.chargers;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.strategic.costs.ChargingCostCalculator;
import org.matsim.contrib.ev.strategic.replanning.innovator.InnovationHelper;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.LegBasedCandidate;

public class MinimalCostChargerSelector implements ChargerSelector {
    static public final String NAME = "MinimalCost";

    private final Person person;
    private final InnovationHelper innovationHelper;
    private final EnergyHelper energyHelper;

    private final ChargingCostCalculator costCalculator;

    public MinimalCostChargerSelector(Person person, InnovationHelper innovationHelper, EnergyHelper energyHelper,
            ChargingCostCalculator costCalculator) {
        this.person = person;
        this.innovationHelper = innovationHelper;
        this.energyHelper = energyHelper;
        this.costCalculator = costCalculator;
    }

    @Override
    public ChargerSpecification select(ActivityBasedCandidate slot, List<ChargerSpecification> chargers) {
        double startTime = innovationHelper.startTime(slot);
        double duration = innovationHelper.duration(slot);

        double bestObjecive = Double.POSITIVE_INFINITY;
        ChargerSpecification bestCharger = null;

        for (ChargerSpecification charger : chargers) {
            double chargedEnergy = energyHelper.charge(slot, charger);

            double cost = costCalculator.calculateChargingCost(person.getId(), charger.getId(), startTime, duration,
                    chargedEnergy);

            if (bestCharger == null || cost < bestObjecive) {
                bestObjecive = cost;
                bestCharger = charger;
            }
        }

        return bestCharger;
    }

    @Override
    public ChargerSpecification select(LegBasedCandidate slot, double duration, List<ChargerSpecification> chargers) {
        double startTime = innovationHelper.startTime(slot);
        double endTime = innovationHelper.endTime(slot);
        double referenceTime = 0.5 * (startTime + endTime);

        double bestObjecive = Double.POSITIVE_INFINITY;
        ChargerSpecification bestCharger = null;

        for (ChargerSpecification charger : chargers) {
            double chargedEnergy = energyHelper.charge(slot, duration, charger);

            double cost = costCalculator.calculateChargingCost(person.getId(), charger.getId(), referenceTime, duration,
                    chargedEnergy);

            if (bestCharger == null || cost < bestObjecive) {
                bestObjecive = cost;
                bestCharger = charger;
            }
        }

        return bestCharger;
    }

    static public class Factory implements ChargerSelector.Factory {
        private final ChargingCostCalculator costCalculator;
        private final EnergyHelper.Factory energyFactory;

        public Factory(ChargingCostCalculator costCalculator, EnergyHelper.Factory energyFactory) {
            this.costCalculator = costCalculator;
            this.energyFactory = energyFactory;
        }

        @Override
        public ChargerSelector create(Person person, Plan plan, Random random, InnovationHelper innovationHelper) {
            return new MinimalCostChargerSelector(person, innovationHelper, energyFactory.build(plan, innovationHelper),
                    costCalculator);
        }
    }
}
