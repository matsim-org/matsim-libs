package playground.sergioo.EventAnalysisTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class LinkData {
	
	private List<TimeIntervalData> timeIntervals;
	private Map<Id,Double> insideTimesVehicles;
	private double length;
	private int numLanes;
	private boolean used=false;
	private double lastTime = 0;
	
	public LinkData(double length, int numLanes) {
		super();
		timeIntervals = new ArrayList<TimeIntervalData>();
		insideTimesVehicles = new HashMap<Id, Double>();
		this.length = length;
		this.numLanes = numLanes;
		addTimeInterval();
	}
	public void addTimeInterval() {
		timeIntervals.add(new TimeIntervalData());
		double time = timeIntervals.size()*TimeSpaceDistribution.TIME_INTERVAL;
		timeIntervals.get(timeIntervals.size()-1).sumQuantity((time-lastTime)*insideTimesVehicles.size());
		lastTime = time;
	}
	public void addEnterVehicle(Id id, Double time) {
		timeIntervals.get(timeIntervals.size()-1).sumQuantity((time-lastTime)*insideTimesVehicles.size());
		lastTime = time;
		insideTimesVehicles.put(id, time);
	}
	public void addExitVehicle(Id id, Double time) {
		Double enterTime = insideTimesVehicles.get(id);
		if(enterTime!=null) {
			used = true;
			if(time-enterTime>0) {
				timeIntervals.get(timeIntervals.size()-1).sumQuantity((time-lastTime)*insideTimesVehicles.size());
				lastTime = time;
				timeIntervals.get(timeIntervals.size()-1).sumSpeeds(length/(time-enterTime));
				timeIntervals.get(timeIntervals.size()-1).sumTravelTimes(time-enterTime);
				timeIntervals.get(timeIntervals.size()-1).addExitVehicle();
			}
			insideTimesVehicles.remove(id);
		}
	}
	public boolean isUsed() {
		return used;
	}
	public int getTimeSize() {
		return timeIntervals.size();
	}
	public int getSize() {
		return insideTimesVehicles.size();
	}
	public double getFlow(int timeInterval) {
		return timeIntervals.get(timeInterval).getFlow();
	}
	public double getFlow() {
		double flow = 0;
		for(TimeIntervalData timeIntervalData:timeIntervals)
			flow+=timeIntervalData.getFlow();
		return flow/timeIntervals.size();
	}
	public double getDensity(int timeInterval) {
		return timeIntervals.get(timeInterval).getAvgQuantity()/(length*numLanes);
	}
	public double getDensity() {
		double density = 0;
		for(TimeIntervalData timeIntervalData:timeIntervals)
			density+=timeIntervalData.getAvgQuantity()/(length*numLanes);
		return density/timeIntervals.size();
	}
	public double getAvgSpeeds(int timeInterval) {
		return timeIntervals.get(timeInterval).getAvgSpeeds();
	}
	public double getSpeed() {
		double speed = 0;
		int num = 0;
		for(TimeIntervalData timeIntervalData:timeIntervals) {
			if(!Double.isNaN(timeIntervalData.getAvgSpeeds())) {
				speed+=timeIntervalData.getAvgSpeeds();
				num++;
			}
		}
		return speed/num;
	}
	public double getAvgTravelTimes(int timeInterval) {
		return timeIntervals.get(timeInterval).getAvgTravelTimes();
	}
	public double getConcentration(int timeInterval) {
		return timeIntervals.get(timeInterval).getConcentration();
	}
	public double getConcentration() {
		double concentration = 0;
		int num=0;
		for(TimeIntervalData timeIntervalData:timeIntervals)
			if(!Double.isNaN(timeIntervalData.getConcentration())) {
				concentration+=timeIntervalData.getConcentration();
				num++;
			}
		return num==0?0:concentration/num;
	}
	
}
