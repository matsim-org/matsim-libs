package org.matsim.contrib.drt.extension.edrt.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtWaitTask;
import org.matsim.contrib.evrp.ETask;

public class EDrtWaitTask extends DrtWaitTask implements ETask {
	private final double consumedEnergy;

	public EDrtWaitTask(double beginTime, double endTime, Link link, double consumedEnergy) {
		super(beginTime, endTime, link);
		this.consumedEnergy = consumedEnergy;
	}

	@Override
	public double getTotalEnergy() {
		return consumedEnergy;
	}
}
