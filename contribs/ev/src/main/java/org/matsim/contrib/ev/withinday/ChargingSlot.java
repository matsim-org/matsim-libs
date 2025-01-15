package org.matsim.contrib.ev.withinday;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.ev.infrastructure.Charger;

import com.google.common.base.Preconditions;

/**
 * A charging slot represents when an agent intends to charge during the day.
 * Charging slots can be leg-based and activity-based. See ChargingSlotFinder
 * for more information.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public record ChargingSlot(@Nullable Activity startActivity, @Nullable Activity endActivity, @Nullable Leg leg,
        double duration, Charger charger) {
    public boolean isLegBased() {
        return duration > 0.0;
    }

    public ChargingSlot(Activity startActivity, Activity endActivity, Charger charger) {
        this(startActivity, endActivity, null, 0.0, charger);
    }

    public ChargingSlot(Leg leg, double duration, Charger charger) {
        this(null, null, leg, duration, charger);
        Preconditions.checkArgument(duration > 0.0);
    }
}
