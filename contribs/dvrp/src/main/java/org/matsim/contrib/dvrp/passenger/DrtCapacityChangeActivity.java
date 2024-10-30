package org.matsim.contrib.dvrp.passenger;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLoad;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;

public class DrtCapacityChangeActivity extends FirstLastSimStepDynActivity {

	private final DvrpVehicleLoad dvrpVehicleLoad;
	private final DvrpVehicle dvrpVehicle;
	private final double endTime;

	public DrtCapacityChangeActivity(String activityType, DvrpVehicle dvrpVehicle, DvrpVehicleLoad dvrpVehicleLoad, double endTime) {
		super(activityType);
		this.dvrpVehicle = dvrpVehicle;
		this.dvrpVehicleLoad = dvrpVehicleLoad;
		this.endTime = endTime;
	}

	@Override
	protected void simStep(double now) {
		if(!dvrpVehicleLoad.equals(dvrpVehicle.getCapacity())) {
			this.dvrpVehicle.setCapacity(dvrpVehicleLoad);
		}
	}

	@Override
	protected boolean isLastStep(double now) {
		return now >= this.endTime;
	}
}
