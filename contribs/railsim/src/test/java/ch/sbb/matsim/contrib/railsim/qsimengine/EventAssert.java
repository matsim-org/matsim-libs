package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.assertj.core.api.AbstractAssert;
import org.matsim.api.core.v01.events.Event;

class EventAssert extends AbstractAssert<EventAssert, Event> {
	protected EventAssert(Event event, Class<?> selfType) {
		super(event, selfType);
	}
}
