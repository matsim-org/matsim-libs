package org.matsim.contrib.ev.withinday;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * A charging alternative is mainly used to encode an alternative charger that
 * can be used for a planned charging activity during the day. However, for
 * leg-based charging, also the planne duration can be changed. Furthermore, by
 * providing a duration, an initially activity-based charging slot can be
 * transformed into a leg-based charging slot.
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public record ChargingAlternative(Id<Charger> charger, double duration) {
	public ChargingAlternative(Id<Charger> charger) {
		this(charger, 0.0);
	}

	public boolean isLegBased() {
		return duration > 0.0;
	}
}


