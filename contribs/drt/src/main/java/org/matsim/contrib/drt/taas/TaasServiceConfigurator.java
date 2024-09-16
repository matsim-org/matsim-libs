package org.matsim.contrib.drt.taas;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.routing.DrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class TaasServiceConfigurator implements PassengerStopDurationProvider, DrtRouteConstraintsCalculator {
	private final Population population;

	private final double maximumPassengerWaitTime;
	private final double maximumPassengerTravelDelay;
	private final double passengerInteractionDuration;

	private final double parcelLatestDeliveryTime;
	private final double parcelPickupDuration;
	private final double parcelDeliveryDuration;

	TaasServiceConfigurator(Population population, double maximumPassengerWaitTime, double maximumPassengerTravelDelay,
			double passengerInteractionDuration, double parcelLatestDeliveryTime, double parcelPickupDuration,
			double parcelDeliveryDuration) {
		this.population = population;
		this.maximumPassengerWaitTime = maximumPassengerWaitTime;
		this.maximumPassengerTravelDelay = maximumPassengerTravelDelay;
		this.passengerInteractionDuration = passengerInteractionDuration;
		this.parcelLatestDeliveryTime = parcelLatestDeliveryTime;
		this.parcelPickupDuration = parcelPickupDuration;
		this.parcelDeliveryDuration = parcelDeliveryDuration;
	}

	@Override
	public DrtRouteConstraints calculateRouteConstraints(double departureTime, Link accessActLink, Link egressActLink,
			Person person, Attributes tripAttributes, double unsharedRideTime, double unsharedDistance) {
		final double maxTravelTime;
		final double maxWaitTime;

		switch (getRequestType(person)) {
		case "passenger":
			maxWaitTime = maximumPassengerWaitTime;
			maxTravelTime = unsharedRideTime + maximumPassengerTravelDelay;
			break;
		case "parcel":
			maxWaitTime = 30.0 * 3600.0;
			maxTravelTime = parcelLatestDeliveryTime - departureTime;
			break;
		default:
			throw new IllegalStateException("Unknown request type");
		}

		return new DrtRouteConstraints(maxTravelTime, Double.POSITIVE_INFINITY, maxWaitTime);
	}

	@Override
	public double calcPickupDuration(DvrpVehicle vehicle, DrtRequest request) {
		double pickupDuration = 0.0;

		for (Id<Person> personId : request.getPassengerIds()) {
			Person person = population.getPersons().get(personId);

			switch (getRequestType(person)) {
			case "passenger":
				pickupDuration = Math.max(pickupDuration, passengerInteractionDuration);
				break;
			case "parcel":
				pickupDuration = Math.max(pickupDuration, parcelPickupDuration);
				break;
			default:
				throw new IllegalStateException("Unknown request type");
			}
		}

		return pickupDuration;
	}

	@Override
	public double calcDropoffDuration(DvrpVehicle vehicle, DrtRequest request) {
		double dropoffDuration = 0.0;

		for (Id<Person> personId : request.getPassengerIds()) {
			Person person = population.getPersons().get(personId);

			switch (getRequestType(person)) {
			case "passenger":
				dropoffDuration = Math.max(dropoffDuration, passengerInteractionDuration);
				break;
			case "parcel":
				dropoffDuration = Math.max(dropoffDuration, parcelDeliveryDuration);
				break;
			default:
				throw new IllegalStateException("Unknown request type");
			}
		}

		return dropoffDuration;
	}

	private String getRequestType(Person person) {
		return (String) person.getAttributes().getAttribute("requestType");
	}
}
