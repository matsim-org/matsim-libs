package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.Condition;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

import java.util.Collection;
import java.util.stream.StreamSupport;

class EventsAssert extends AbstractCollectionAssert<EventsAssert, Collection<? extends Event>, Event, EventAssert> {
	protected EventsAssert(Collection<? extends Event> events, Class<?> selfType) {
		super(events, selfType);
	}

	public EventsAssert hasTrainState(String veh, double time, double headPosition, double speed) {
		return haveExactly(1,
			new Condition<>(event ->
				(event instanceof RailsimTrainStateEvent ev)
					&& ev.getVehicleId().equals(Id.createVehicleId(veh))
					&& FuzzyUtils.equals(ev.getTime(), time)
					&& FuzzyUtils.equals(ev.getHeadPosition(), headPosition)
					&& FuzzyUtils.equals(ev.getSpeed(), speed),
				String.format("event with veh %s time %.0f headPosition: %.2f speed: %.2f", veh, time, headPosition, speed))
		);
	}

	@Override
	protected EventAssert toAssert(Event value, String description) {
		return null;
	}

	@Override
	protected EventsAssert newAbstractIterableAssert(Iterable<? extends Event> iterable) {
		return new EventsAssert(StreamSupport.stream(iterable.spliterator(), false).toList(), EventsAssert.class);
	}
}
