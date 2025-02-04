package org.matsim.contrib.dvrp.passenger;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public class VehicleCapacityChangeActivity extends FirstLastSimStepDynActivity {

	private final DvrpLoad dvrpLoad;
	private final DvrpVehicle dvrpVehicle;
	private final double endTime;

	public VehicleCapacityChangeActivity(String activityType, DvrpVehicle dvrpVehicle, DvrpLoad dvrpLoad, double endTime) {
		super(activityType);
		this.dvrpVehicle = dvrpVehicle;
		this.dvrpLoad = dvrpLoad;
		this.endTime = endTime;
	}

	@Override
	protected void simStep(double now) {
		if(!dvrpLoad.equals(dvrpVehicle.getCapacity())) {
			this.dvrpVehicle.setCapacity(dvrpLoad);
		}
	}

	@Override
	protected boolean isLastStep(double now) {
		return now >= this.endTime;
	}
}
