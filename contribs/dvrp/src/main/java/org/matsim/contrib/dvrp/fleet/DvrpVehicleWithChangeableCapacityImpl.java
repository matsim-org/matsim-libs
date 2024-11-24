package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.network.Link;

public class DvrpVehicleWithChangeableCapacityImpl extends DvrpVehicleImpl {

	private DvrpLoad capacity;

	public DvrpVehicleWithChangeableCapacityImpl(DvrpVehicleSpecification specification, Link startLink) {
		super(specification, startLink);
		this.capacity = specification.getCapacity();
	}

	public void setCapacity(DvrpLoad capacity) {
		this.capacity = capacity;
	}

	@Override
	public DvrpLoad getCapacity() {
		return this.capacity;
	}
}
