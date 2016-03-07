package playground.dhosse.scenarios.generic.population.io.mid;

import java.util.LinkedList;

import org.matsim.core.utils.misc.Time;

import playground.dhosse.gap.Global;

public class MiDTravelChain {
	
	static String TYPE_EXTERIOR = "ext";
	static String TYPE_INTERIOR = "int";
	
	private LinkedList<MiDTravelStage> stages = new LinkedList<>();
	
	private double weight = 0.;
	private double c = 0.;
	private double maxD = 0.;
	
	public MiDTravelChain(String pId,String[] legs, String[] acts, String[] times, String[] lengths, String weight, String type){
		
		//acts.length() = legs.length + 1 (because of first activity: home / other)
		for(int i = 0; i < legs.length; i++){
			
			if(isPrimaryActType(acts[i + 1])){
			if(!lengths[i].equals("NULL")){

				double d = 1000*Double.parseDouble(lengths[i].replace(",", "."));
				if(d > this.c)
				this.c = d;
				
			}
			} else{
				
				if(!lengths[i].equals("NULL")){
					
					double d = 1000*Double.parseDouble(lengths[i].replace(",", "."));
					if(d > maxD){
						maxD = d;
					}
					
				}
				
			}
			
			this.weight = Double.parseDouble(weight);
			this.stages.addLast(new MiDTravelStage(legs[i], acts[i], acts[i+1], Time.parseTime(times[i].split("-")[0]), Time.parseTime(times[i].split("-")[1]),
					lengths[i], type.split("_")[i]));
			
		}
		
	}
	
	private boolean isPrimaryActType(String actType){
		
		return(actType.equals(Global.ActType.home.name()) || actType.equals(Global.ActType.work.name()) || actType.equals(Global.ActType.education.name()));
		
	}
	
	public double getWeight(){
		return this.weight;
	}
	
	public double getC(){
		return this.c;
	}
	
	public double getMaxD(){
		return this.maxD;
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
		private double distance;
		private String type;
		
		public MiDTravelStage(String legMode, String previousActType, String nextActType, double departureTime, double arrivalTime, String distance, String type){
			
			this.legMode = legMode;
			this.previousActType = previousActType;
			this.nextActType = nextActType;
			this.departureTime = departureTime;
			this.arrivalTime = arrivalTime;
			if(this.arrivalTime < this.departureTime){
				this.arrivalTime += 24 * 3600;
			}
			if(!distance.equals("NULL")){
				this.distance = Double.parseDouble(distance.replace(",", "."));
			}
			if(type.equals("4")){
				this.type = TYPE_EXTERIOR;
			} else{
				this.type = TYPE_INTERIOR;
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

		public double getDistance() {
			return distance;
		}

		public void setDistance(double distance) {
			this.distance = distance;
		}
		
		public String getType(){
			return this.type;
		}
		
	}

}
