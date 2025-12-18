package org.matsim.api.core.v01.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class PersonEntersPtEvent extends Event implements HasPersonId, HasVehicleId {

	private final Id<Person> person;
	private final Id<Vehicle> vehicle;
	private final Id<TransitStopFacility> stop;
	private final Id<TransitLine> line;
	private final Id<TransitRoute> route;

	public PersonEntersPtEvent(double time, Id<Person> person, Id<Vehicle> vehicle, Id<TransitStopFacility> stop, Id<TransitLine> line, Id<TransitRoute> route) {
		super(time);
		this.person = person;
		this.vehicle = vehicle;
		this.stop = stop;
		this.line = line;
		this.route = route;
	}

	@Override
	public String getEventType() {
		return "personEntersPt";
	}

	@Override
	public Id<Person> getPersonId() {
		return person;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicle;
	}

	public Id<TransitStopFacility> getStop() {
		return stop;
	}

	public Id<TransitLine> getLine() {
		return line;
	}

	public Id<TransitRoute> getRoute() {
		return route;
	}
}
