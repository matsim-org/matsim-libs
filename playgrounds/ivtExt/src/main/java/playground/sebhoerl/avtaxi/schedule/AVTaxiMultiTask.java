package playground.sebhoerl.avtaxi.schedule;

import org.matsim.contrib.dvrp.schedule.Task;

public interface AVTaxiMultiTask extends Task {
	static enum AVTaxiMultiTaskType {
		MULTI_PICKUP, MULTI_DROPOFF
	}
	
	AVTaxiMultiTaskType getMultiTaxiTaskType();
}
