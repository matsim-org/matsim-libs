package playground.wrashid.jdeqsim.parallel;

import org.matsim.core.mobsim.jdeqsim.Road;
import org.matsim.core.mobsim.jdeqsim.Scheduler;
import org.matsim.core.network.Link;

public class ExtendedRoad extends Road {

	// 0: first Zone
	// 1: second Zone...
	private int threadZoneId=0;
	
	private boolean borderZone=false;
	
	public ExtendedRoad(Scheduler scheduler, Link link) {
		super(scheduler, link);
	}

	public int getThreadZoneId() {
		return threadZoneId;
	}

	public void setThreadZoneId(int threadZoneId) {
		this.threadZoneId = threadZoneId;
	}

	public boolean isBorderZone() {
		return borderZone;
	}

	public void setBorderZone(boolean borderZone) {
		this.borderZone = borderZone;
	}
	

}
