package org.matsim.contrib.drt.extension.operations.eshifts.charging;

import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.evrp.ChargingActivity;
import org.matsim.contrib.evrp.ChargingTask;

/**
 * @author nkuehnel / MOIA
 */
public class FixedTimeChargingActivity implements DynActivity {

	private final ChargingTask chargingTask;
	private final ChargingActivity delegate;

	private final double endTime;

	public FixedTimeChargingActivity(ChargingTask chargingTask, double endTime) {
		this.chargingTask = chargingTask;
		this.endTime = endTime;
		this.delegate = new ChargingActivity(chargingTask);
	}

	@Override
	public String getActivityType() {
		return ChargingActivity.ACTIVITY_TYPE;
	}

	@Override
	public double getEndTime() {
		return endTime;
	}

	@Override
	public void doSimStep(double now) {
		delegate.doSimStep(now);
		ChargingWithAssignmentLogic logic = chargingTask.getChargingLogic();
		ElectricVehicle ev = chargingTask.getElectricVehicle();
		if(now == endTime) {
			if(delegate.getEndTime() > now) {
				logic.removeVehicle(ev, now);
			}
		}
	}
}
