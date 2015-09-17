package playground.dhosse.gap.scenario.mid;

import java.util.LinkedList;

import org.matsim.core.utils.misc.Time;

public class MiDTravelChain {
	
	private LinkedList<MiDTravelStage> stages = new LinkedList<>();
	
	public MiDTravelChain(String pId,String[] legs, String[] acts, String[] times){
		
		//acts.length() = legs.length + 1 (because of first activity: home / other)
		for(int i = 0; i < legs.length; i++){
			
			this.stages.addLast(new MiDTravelStage(legs[i], acts[i], acts[i+1], Time.parseTime(times[i].split("-")[0]), Time.parseTime(times[i].split("-")[1])));
			
		}
		
	}
	
	public LinkedList<MiDTravelStage> getStages(){
		return this.stages;
	}
	
	public static class MiDTravelStage{
		
		private String legMode;
		private String previousActType;
		private String nextActType;
		private double departureTime;
		private double arrivalTime;
		
		public MiDTravelStage(String legMode, String previousActType, String nextActType, double departureTime, double arrivalTime){
			
			this.legMode = legMode;
			this.previousActType = previousActType;
			this.nextActType = nextActType;
			this.departureTime = departureTime;
			this.arrivalTime = arrivalTime;
			if(this.arrivalTime < this.departureTime){
				this.arrivalTime += 24 * 3600;
			}
			
		}

		public String getLegMode() {
			return legMode;
		}

		public void setLegMode(String legMode) {
			this.legMode = legMode;
		}

		public String getPreviousActType() {
			return previousActType;
		}

		public void setPreviousActType(String previousActType) {
			this.previousActType = previousActType;
		}

		public String getNextActType() {
			return nextActType;
		}

		public void setNextActType(String nextActType) {
			this.nextActType = nextActType;
		}

		public double getDepartureTime() {
			return departureTime;
		}

		public void setDepartureTime(double departureTime) {
			this.departureTime = departureTime;
		}

		public double getArrivalTime() {
			return arrivalTime;
		}

		public void setArrivalTime(double arrivalTime) {
			this.arrivalTime = arrivalTime;
		}
		
	}

}
