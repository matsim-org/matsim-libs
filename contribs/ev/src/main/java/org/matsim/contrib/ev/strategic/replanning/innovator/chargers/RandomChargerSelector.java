package org.matsim.contrib.ev.strategic.replanning.innovator.chargers;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.strategic.replanning.innovator.InnovationHelper;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.LegBasedCandidate;

public class RandomChargerSelector implements ChargerSelector {
    static public final String NAME = "Random";

    private final Random random;

    private RandomChargerSelector(Random random) {
        this.random = random;
    }

    @Override
    public ChargerSpecification select(ActivityBasedCandidate slot, List<ChargerSpecification> chargers) {
        return chargers.get(random.nextInt(chargers.size()));
    }

    @Override
    public ChargerSpecification select(LegBasedCandidate slot, double duration, List<ChargerSpecification> chargers) {
        return chargers.get(random.nextInt(chargers.size()));
    }

    static public class Factory implements ChargerSelector.Factory {
        @Override
        public ChargerSelector create(Person person, Plan plan, Random random, InnovationHelper innovationHelper) {
            return new RandomChargerSelector(random);
        }
    }
}
