package org.matsim.contrib.drt.optimizer.distributed;


import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import java.util.List;

/**
 * Provides information to and from the optimizer.
 */
public class DrtOptimizerCommunicator implements MobsimBeforeSimStepListener, MobsimAfterSimStepListener {

	private final DrtNodeCommunicator comm;
	private final String mode;
	private final Fleet fleet;
	private final DrtOptimizer optimizer;

	public DrtOptimizerCommunicator(DrtNodeCommunicator comm, String mode,
									Fleet fleet, DrtOptimizer optimizer) {
		this.comm = comm;
		this.mode = mode;
		this.fleet = fleet;
		this.optimizer = optimizer;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		List<DrtRequest> requests = comm.getRequests(mode);
		requests.forEach(optimizer::requestSubmitted);

		for (DvrpVehicle vehicle : fleet.getVehicles().values()) {

			Schedule schedule = comm.getSchedule(vehicle.getId());
			if (schedule == null)
				continue;

			vehicle.getSchedule().update(schedule);
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		comm.sendSchedules(fleet);
	}
}
