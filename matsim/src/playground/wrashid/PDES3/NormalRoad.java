package playground.wrashid.PDES3;

import org.matsim.core.api.network.Link;
import org.matsim.mobsim.jdeqsim.Road;
import org.matsim.mobsim.jdeqsim.Scheduler;


public class NormalRoad extends Road {

	private int zoneId = 0;

	public NormalRoad(PScheduler scheduler, Link link, int zoneId) {
		super(scheduler, link);
		this.zoneId = zoneId;
	}

}
