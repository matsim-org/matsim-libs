package playground.sebhoerl.avtaxi.schedule;

import org.matsim.contrib.dvrp.schedule.Task;

public interface AVTask extends Task {
	static enum AVTaskType {
		PICKUP, DROPOFF, DRIVE
	}
	
	AVTaskType getAVTaskType();

}
