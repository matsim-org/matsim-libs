package org.matsim.contrib.sharing.service;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.sharing.io.SharingStationSpecification;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

public class SharingStation {
	private final SharingStationSpecification specification;
	private final Link link;

	private List<SharingVehicle> vehicles = new LinkedList<>();

	public SharingStation(SharingStationSpecification specification, Link link) {
		this.specification = specification;
		this.link = link;
	}

	public Id<SharingStation> getId() {
		return specification.getId();
	}

	public void addVehicle(SharingVehicle vehicle) {
		Verify.verify(vehicles.size() + 1 <= specification.getCapacity());
		Verify.verify(!vehicles.contains(vehicle));
		vehicles.add(vehicle);
	}

	public void removeVehicle(SharingVehicle vehicle) {
		Verify.verify(vehicles.contains(vehicle));
		vehicles.remove(vehicle);
	}

	public ImmutableList<SharingVehicle> getVehicles() {
		return ImmutableList.copyOf(vehicles);
	}

	public int getFreeCapacity() {
		return specification.getCapacity() - vehicles.size();
	}

	public Link getLink() {
		return link;
	}
}
