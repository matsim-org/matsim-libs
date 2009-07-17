package org.matsim.core.mobsim.jdeqsim.parallel;

import org.matsim.core.mobsim.jdeqsim.Road;
import org.matsim.core.mobsim.jdeqsim.Scheduler;
import org.matsim.core.network.LinkImpl;

public class ExtendedRoad extends Road {

	// 0: first Zone
	// 1: second Zone...
	private int threadZoneId=0;
	
	private boolean borderZone=false;
	
	public ExtendedRoad(Scheduler scheduler, LinkImpl link) {
		super(scheduler, link);
		// TODO Auto-generated constructor stub
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
