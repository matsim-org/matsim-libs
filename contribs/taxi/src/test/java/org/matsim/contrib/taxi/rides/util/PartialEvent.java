package org.matsim.contrib.taxi.rides.util;

import com.google.common.base.MoreObjects;
import org.matsim.api.core.v01.events.Event;

import javax.annotation.Nullable;
import java.util.Objects;

public class PartialEvent {
	@Nullable
	Double time;
	String type;
	String person;
	String vehicle;

	public PartialEvent(Double time, String type, String person, String vehicle) {
		this.time = time;
		this.type = type;
		this.person = person;
		this.vehicle = vehicle;
	}

	boolean matches(Event ev) {
		return Objects.equals(type, ev.getEventType()) &&
				(time == null || time.equals(ev.getTime())) &&
				(person == null || person.equals(ev.getAttributes().get("person"))) &&
				(vehicle == null || vehicle.equals(ev.getAttributes().get("vehicle")));
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("time", time)
				.add("type", type)
				.add("person", person)
				.add("vehicle", vehicle)
				.toString();
	}
}
