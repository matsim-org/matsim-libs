package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.network.Link;

public class DvrpVehicleWithChangeableCapacityImpl extends DvrpVehicleImpl {

	private DvrpVehicleLoad capacity;

	public DvrpVehicleWithChangeableCapacityImpl(DvrpVehicleSpecification specification, Link startLink) {
		super(specification, startLink);
		this.capacity = specification.getCapacity();
	}

	public void setCapacity(DvrpVehicleLoad capacity) {
		this.capacity = capacity;
	}

	@Override
	public DvrpVehicleLoad getCapacity() {
		return this.capacity;
	}
}
