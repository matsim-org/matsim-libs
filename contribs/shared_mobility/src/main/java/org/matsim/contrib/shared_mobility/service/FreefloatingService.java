package org.matsim.contrib.shared_mobility.service;

import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.shared_mobility.io.SharingVehicleSpecification;
import org.matsim.contrib.shared_mobility.routing.InteractionPoint;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.common.base.Verify;

public class FreefloatingService implements SharingService {
	private final Id<SharingService> serviceId;
	private final double maximumDistance;
	private static final Logger LOG = LogManager.getLogger(FreefloatingService.class);

	private final Network network;

	private final IdSet<SharingVehicle> activeRentals = new IdSet<>(SharingVehicle.class);
	private final IdMap<SharingVehicle, SharingVehicle> vehicles = new IdMap<>(SharingVehicle.class);
	private final IdMap<Person, SharingVehicle> reservations = new IdMap<>(Person.class);
	private final QuadTree<SharingVehicle> availableVehicles;

	public FreefloatingService(Id<SharingService> serviceId, Collection<SharingVehicleSpecification> fleet,
							   Network network, double maximumDistance) {
		this.network = network;

		this.maximumDistance = maximumDistance;
		this.serviceId = serviceId;

		for (SharingVehicleSpecification vehicle : fleet) {
			Link link = network.getLinks().get(vehicle.getStartLinkId().get());
			vehicles.put(vehicle.getId(), new SharingVehicle(vehicle, link));
		}

		double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());
		this.availableVehicles = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
		vehicles.values().forEach(v -> {
			availableVehicles.put(v.getLink().getCoord().getX(), v.getLink().getCoord().getY(), v);
		});
	}

	@Override
	public void pickupVehicle(SharingVehicle vehicle, MobsimAgent agent) {
		Verify.verify(!activeRentals.contains(vehicle.getId()));
		activeRentals.add(vehicle.getId());

		Coord coord = vehicle.getLink().getCoord();
		availableVehicles.remove(coord.getX(), coord.getY(), vehicle);
	}

	@Override
	public void dropoffVehicle(SharingVehicle vehicle, MobsimAgent agent) {
		Verify.verify(activeRentals.contains(vehicle.getId()));
		activeRentals.remove(vehicle.getId());

		Link link = network.getLinks().get(agent.getCurrentLinkId());
		vehicle.setLink(link);

		Coord coord = vehicle.getLink().getCoord();
		availableVehicles.put(coord.getX(), coord.getY(), vehicle);
	}

	@Override
	public Optional<VehicleInteractionPoint> findClosestVehicle(MobsimAgent agent) {
		Link currentLink = network.getLinks().get(agent.getCurrentLinkId());

		if (availableVehicles.size() == 0) {
			return Optional.empty();
		}

		SharingVehicle vehicle = availableVehicles.getClosest(currentLink.getCoord().getX(),
				currentLink.getCoord().getY());

		if (CoordUtils.calcEuclideanDistance(vehicle.getLink().getCoord(), currentLink.getCoord()) > maximumDistance) {
			return Optional.empty();
		}

		return Optional.of(VehicleInteractionPoint.of(vehicle));
	}

	@Override
	public InteractionPoint findClosestDropoffLocation(SharingVehicle vehicle, MobsimAgent agent) {
		return InteractionPoint.of(agent.getCurrentLinkId());
	}

	@Override
	public IdMap<SharingVehicle, SharingVehicle> getVehicles() {
		return vehicles; // TODO: Make this unmodifiable
	}

	private final static IdMap<SharingStation, SharingStation> EMPTY_STATION_MAP = new IdMap<>(SharingStation.class);

	@Override
	public IdMap<SharingStation, SharingStation> getStations() {
		return EMPTY_STATION_MAP; // TODO: Make this unmodifiable
	}

	@Override
	public Id<SharingService> getId() {
		return serviceId;
	}

	// Remove the vehicle to be not anymore available for other bookings
	@Override
	public void reserveVehicle(MobsimAgent agent,SharingVehicle vehicle) {
		Verify.verify(!reservations.values().contains(vehicle)); // This vehicle should be reserved only once
		Coord coord = vehicle.getLink().getCoord();
		availableVehicles.remove(coord.getX(), coord.getY(), vehicle);
		reservations.put(agent.getId(), vehicle);

		LOG.debug("Reserve vehicle {} from agent {}",vehicle.getId().toString(), agent.getId().toString());
	}

	@Override
	public Optional<SharingVehicle> getReservedVehicle(MobsimAgent agent)
	{
		SharingVehicle value = reservations.get(agent.getId());
		return value == null ? Optional.empty() : Optional.of(value);
	}

	@Override
	public void releaseReservation(MobsimAgent agent) {
		SharingVehicle v = this.reservations.remove(agent.getId());
		if(v!=null)
		{
			Coord coord = v.getLink().getCoord();
			availableVehicles.put(coord.getX(), coord.getY(), v);
			LOG.debug("Release vehicle {} from agent {}",v.getId().toString(), agent.getId().toString());
		}
	}

}
