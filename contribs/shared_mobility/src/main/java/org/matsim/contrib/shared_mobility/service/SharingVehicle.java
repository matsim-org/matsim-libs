package org.matsim.contrib.shared_mobility.service;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.io.SharingVehicleSpecification;

public class SharingVehicle {
	private final SharingVehicleSpecification specification;
	private Link link;

	public SharingVehicle(SharingVehicleSpecification specification, Link link) {
		this.specification = specification;
		this.link = link;
	}

	public Id<SharingVehicle> getId() {
		return specification.getId();
	}

	public SharingVehicleSpecification getSpecification() {
		return specification;
	}

	public Link getLink() {
		return link;
	}

	public void setLink(Link link) {
		this.link = link;
	}
}
