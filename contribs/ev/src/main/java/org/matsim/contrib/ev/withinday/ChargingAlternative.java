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
public record ChargingAlternative(Id<ChargingAlternative> id, Id<Charger> charger, double duration, RequestStatus status) {
	public ChargingAlternative(Id<ChargingAlternative> id, Id<Charger> charger) {
		this(id, charger, 0.0, RequestStatus.ACCEPTED);
	}

	public ChargingAlternative(Id<ChargingAlternative> id, Id<Charger> charger, double duration) {
		this(id, charger, duration, RequestStatus.ACCEPTED);
	}

	public boolean isLegBased() {
		return duration > 0.0;
	}

	public enum RequestStatus {PENDING, ACCEPTED, REJECTED}
}


