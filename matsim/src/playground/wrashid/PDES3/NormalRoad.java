package playground.wrashid.PDES3;

import org.matsim.network.Link;

import playground.wrashid.DES.Road;
import playground.wrashid.DES.Scheduler;

public class NormalRoad extends Road {

	private int zoneId = 0;

	public NormalRoad(PScheduler scheduler, Link link, int zoneId) {
		super(scheduler, link);
		this.zoneId = zoneId;
	}

}
