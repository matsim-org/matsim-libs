package org.matsim.contrib.ev.strategic.replanning.innovator.chargers;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.strategic.replanning.innovator.InnovationHelper;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.LegBasedCandidate;

public interface ChargerSelector {
    ChargerSpecification select(ActivityBasedCandidate slot, List<ChargerSpecification> chargers);

    ChargerSpecification select(LegBasedCandidate slot, double duration, List<ChargerSpecification> chargers);

    interface Factory {
        ChargerSelector create(Person person, Plan plan, Random random, InnovationHelper innovationHelper);
    }
}
