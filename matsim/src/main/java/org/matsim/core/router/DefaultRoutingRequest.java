package org.matsim.core.router;

import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class DefaultRoutingRequest implements RoutingRequest {

	// attribute for storing a vehicle id that should be used for this routing
	// request instead of the person's default vehicle
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicleId";

	private final Attributes attributes;

	private final Facility fromFactility;
	private final Facility toFacility;
	private final double departureTime;
	private final Person person;

	protected DefaultRoutingRequest(Facility fromFacility, Facility toFacility, double departureTime, Person person,
			Attributes attributes) {
		this.fromFactility = fromFacility;
		this.toFacility = toFacility;
		this.departureTime = departureTime;
		this.person = person;
		this.attributes = attributes;
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

	static public RoutingRequest of(Facility fromFacility, Facility toFacility, double departureTime, Person person,
			Attributes attributes) {
		return new DefaultRoutingRequest(fromFacility, toFacility, departureTime, person, attributes);
	}

	static public RoutingRequest withoutAttributes(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		return new DefaultRoutingRequest(fromFacility, toFacility, departureTime, person, new AttributesImpl());
	}
}
