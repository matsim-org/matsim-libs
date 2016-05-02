package playground.dhosse.scenarios.generic.population.io.mid;

import org.matsim.contrib.minibus.genericUtils.RecursiveStatsContainer;

public class MiDStatsContainer {

	private RecursiveStatsContainer distanceDelegate;
	private RecursiveStatsContainer durationDelegate;
	private RecursiveStatsContainer startTimeDelegate;
	private RecursiveStatsContainer endTimeDelegate;
	
	public MiDStatsContainer(){
		
		this.distanceDelegate = new RecursiveStatsContainer();
		this.durationDelegate = new RecursiveStatsContainer();
		this.startTimeDelegate = new RecursiveStatsContainer();
		this.endTimeDelegate = new RecursiveStatsContainer();
		
	}
	
	public RecursiveStatsContainer getDistances(){
		return this.distanceDelegate;
	}
	
	public RecursiveStatsContainer getDurations(){
		return this.durationDelegate;
	}
	
	public RecursiveStatsContainer getStartTimes(){
		return this.startTimeDelegate;
	}
	
	public RecursiveStatsContainer getEndTimes(){
		return this.endTimeDelegate;
	}
	
	public void handleDistance(double distance){
		
		if(distance != Double.NEGATIVE_INFINITY){
			this.distanceDelegate.handleNewEntry(distance);
		}
		
	}
	
	public void handleDuration(double duration){
		
		if(duration != Double.NEGATIVE_INFINITY){
			this.durationDelegate.handleNewEntry(duration);
		}
		
	}
	
	public void handleStartTime(double startTime){
		
		if(startTime != Double.NEGATIVE_INFINITY){
			this.startTimeDelegate.handleNewEntry(startTime);
		}
		
	}
	
	public void handleEndTime(double endTime){
		
		if(endTime != Double.NEGATIVE_INFINITY){
			this.endTimeDelegate.handleNewEntry(endTime);
		}
		
	}
	
}
