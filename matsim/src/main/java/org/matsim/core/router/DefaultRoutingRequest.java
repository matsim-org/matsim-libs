package org.matsim.core.router;

import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class DefaultRoutingRequest implements RoutingRequest {
	private final Attributes attributes = new Attributes();

	private final Facility fromFactility;
	private final Facility toFacility;
	private double departureTime;
	private final Person person;

	public DefaultRoutingRequest(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		this.fromFactility = fromFacility;
		this.toFacility = toFacility;
		this.departureTime = departureTime;
		this.person = person;
	}

	public DefaultRoutingRequest(Facility fromFacility, Facility toFacility, double departureTime) {
		this(fromFacility, toFacility, departureTime, null);
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public Facility getFromFacility() {
		return fromFactility;
	}

	@Override
	public Facility getToFacility() {
		return toFacility;
	}

	@Override
	public double getDepartureTime() {
		return departureTime;
	}

	@Override
	public Person getPerson() {
		return person;
	}

	static public RoutingRequest of(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		return new DefaultRoutingRequest(fromFacility, toFacility, departureTime, person);
	}
}
