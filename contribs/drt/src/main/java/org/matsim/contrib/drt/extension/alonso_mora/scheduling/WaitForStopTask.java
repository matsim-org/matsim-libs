package org.matsim.contrib.drt.extension.alonso_mora.scheduling;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.StayTask;

import com.google.common.base.MoreObjects;

public class WaitForStopTask extends StayTask {
	public static final DrtTaskType TYPE = new DrtTaskType("WaitForStop", STAY);

	public WaitForStopTask(double beginTime, double endTime, Link link) {
		super(TYPE, beginTime, endTime, link);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("super", super.toString()).toString();
	}
}
