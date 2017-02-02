package org.matsim.contrib.carsharing.runExample;

import java.util.ArrayList;

public class ComputationTime {
	
	private long time = 0;
	private ArrayList<Long> startTimes = new ArrayList<Long>();
	private ArrayList<Long> endTimes = new ArrayList<Long>();

	public void reset() {
		time = 0;
		startTimes = new ArrayList<Long>();
		endTimes = new ArrayList<Long>();
	}
	
	public void addTIme (long time) {
		
		this.time += time;
	}
	
	public long getTime() {
		return this.time;
	}
	
	public void addStartTime(long time) {
		
		this.startTimes.add(time);
	}
	
	public void addEndTime(long time) {
		
		this.endTimes.add(time);
	}
	

	public ArrayList<Long> getStartTimes() {
		return this.startTimes;
		
	}
	
	public ArrayList<Long> getEndTimes() {
		return this.endTimes;
	}
}
