package org.matsim.contrib.sharing.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.io.SharingServiceSpecification;
import org.matsim.contrib.sharing.io.SharingStationSpecification;
import org.matsim.contrib.sharing.io.SharingVehicleSpecification;
import org.matsim.contrib.sharing.routing.InteractionPoint;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.common.base.Verify;

public class StationBasedService implements SharingService {
	private final Id<SharingService> serviceId;
	private final double maximumDistance;

	private final Network network;

	private final IdSet<SharingVehicle> activeRentals = new IdSet<>(SharingVehicle.class);
	private final IdMap<SharingVehicle, SharingVehicle> vehicles = new IdMap<>(SharingVehicle.class);
	private final IdMap<SharingStation, SharingStation> stations = new IdMap<>(SharingStation.class);

	private final QuadTree<SharingVehicle> availableVehicles;
	private final QuadTree<SharingStation> availableStations;

	private final Map<Id<Link>, Id<SharingStation>> linkStationMap = new HashMap<>();
	private final Map<Id<SharingVehicle>, SharingStation> vehicleStationMap = new HashMap<>();

	public StationBasedService(Id<SharingService> serviceId, SharingServiceSpecification specification, Network network,
			double maximumDistance) {
		this.network = network;

		this.maximumDistance = maximumDistance;
		this.serviceId = serviceId;

		for (SharingStationSpecification station : specification.getStations()) {
			Link link = network.getLinks().get(station.getLinkId());
			Verify.verifyNotNull(link);

			this.stations.put(station.getId(), new SharingStation(station, link));
			this.linkStationMap.put(station.getLinkId(), station.getId());
		}

		for (SharingVehicleSpecification vehicle : specification.getVehicles()) {
			SharingStation station = this.stations.get(vehicle.getStartStationId().get());
			SharingVehicle instance = new SharingVehicle(vehicle, station.getLink());

			this.vehicles.put(vehicle.getId(), instance);
			this.vehicleStationMap.put(vehicle.getId(), station);
			station.addVehicle(instance);
		}

		double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());

		this.availableVehicles = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
		vehicles.values().forEach(v -> {
			availableVehicles.put(v.getLink().getCoord().getX(), v.getLink().getCoord().getY(), v);
		});

		this.availableStations = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
		stations.values().stream().filter(s -> s.getFreeCapacity() > 0).forEach(s -> {
			availableStations.put(s.getLink().getCoord().getX(), s.getLink().getCoord().getY(), s);
		});
	}

	@Override
	public void pickupVehicle(SharingVehicle vehicle, MobsimAgent agent) {
		Verify.verify(!activeRentals.contains(vehicle.getId()));
		activeRentals.add(vehicle.getId());

		Coord coord = vehicle.getLink().getCoord();
		availableVehicles.remove(coord.getX(), coord.getY(), vehicle);

		SharingStation station = vehicleStationMap.get(vehicle.getId());

		if (station.getFreeCapacity() == 0) {
			// Space is freed up, so add it to the available stations
			Coord stationCoord = station.getLink().getCoord();
			availableStations.put(stationCoord.getX(), stationCoord.getY(), station);
		}

		station.removeVehicle(vehicle);
	}

	@Override
	public void dropoffVehicle(SharingVehicle vehicle, MobsimAgent agent) {
		Verify.verify(activeRentals.contains(vehicle.getId()));
		activeRentals.remove(vehicle.getId());

		Link link = network.getLinks().get(agent.getCurrentLinkId());
		vehicle.setLink(link);

		Coord coord = vehicle.getLink().getCoord();
		availableVehicles.put(coord.getX(), coord.getY(), vehicle);

		SharingStation station = vehicleStationMap.get(vehicle.getId());

		if (station.getFreeCapacity() == 1) {
			// Station becomes full no, so remove it from available stations
			Coord stationCoord = station.getLink().getCoord();
			availableStations.remove(stationCoord.getX(), stationCoord.getY(), station);
		}

		station.addVehicle(vehicle);
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

		SharingStation station = vehicleStationMap.get(vehicle.getId());
		return Optional.of(VehicleInteractionPoint.of(vehicle, station));
	}

	@Override
	public InteractionPoint findClosestDropoffLocation(SharingVehicle vehicle, MobsimAgent agent) {
		Link currentLink = network.getLinks().get(agent.getCurrentLinkId());
		Verify.verify(availableStations.size() > 0,
				"It should never happen that no station is available (it means spots < vehicles)");

		return InteractionPoint
				.of(availableStations.getClosest(currentLink.getCoord().getX(), currentLink.getCoord().getY()));
	}

	@Override
	public Id<SharingService> getId() {
		return serviceId;
	}

	@Override
	public IdMap<SharingVehicle, SharingVehicle> getVehicles() {
		return vehicles; // TODO: Make this unmodifiable
	}

	@Override
	public IdMap<SharingStation, SharingStation> getStations() {
		return stations; // TODO: Make this unmodifiable
	}
}
