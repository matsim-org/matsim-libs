package playground.sergioo.EventAnalysisTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class LinkData {
	
	private List<TimeIntervalData> timeIntervals;
	private Map<Id,Double> insideTimesVehicles;
	private Link link;
	private boolean used=false;
	private double lastTime = 0;
	
	public LinkData(Link link) {
		super();
		timeIntervals = new ArrayList<TimeIntervalData>();
		insideTimesVehicles = new HashMap<Id, Double>();
		this.link = link;
		addTimeInterval();
	}
	public void addTimeInterval() {
		timeIntervals.add(new TimeIntervalData());
		double time = timeIntervals.size()*TimeSpaceDistribution.TIME_INTERVAL;
		timeIntervals.get(timeIntervals.size()-1).sumQuantity((time-lastTime)*insideTimesVehicles.size());
		lastTime = time;
	}
	public void addEnterVehicle(Id personId, Double time) {
		timeIntervals.get(timeIntervals.size()-1).sumQuantity((time-lastTime)*insideTimesVehicles.size());
		lastTime = time;
		insideTimesVehicles.put(personId, time);
	}
	public void addExitVehicle(Id personId, Double time) {
		used = true;
		Double enterTime = insideTimesVehicles.get(personId);
		if(enterTime!=null) {
			timeIntervals.get(timeIntervals.size()-1).sumSpeeds(link.getLength()/(time-enterTime));
			timeIntervals.get(timeIntervals.size()-1).sumTravelTimes(time-enterTime);
			timeIntervals.get(timeIntervals.size()-1).addExitVehicle();
			timeIntervals.get(timeIntervals.size()-1).sumQuantity((time-lastTime)*insideTimesVehicles.size());
			lastTime = time;
			insideTimesVehicles.remove(personId);
		}
	}
	public void addStartActivity(Id personId, Double time) {
		timeIntervals.get(timeIntervals.size()-1).sumQuantity((time-lastTime)*insideTimesVehicles.size());
		lastTime = time;
		insideTimesVehicles.remove(personId);
	}
	public void addEndActivity(Id personId, Double time) {
		
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
		return timeIntervals.get(timeInterval).getAvgQuantity()/(link.getLength()*link.getNumberOfLanes());
	}
	public double getDensity() {
		double density = 0;
		for(TimeIntervalData timeIntervalData:timeIntervals)
			density+=timeIntervalData.getAvgQuantity()/(link.getLength()*link.getNumberOfLanes());
		return density/timeIntervals.size();
	}
	public double getAvgSpeeds(int timeInterval) {
		return timeIntervals.get(timeInterval).getAvgSpeeds();
	}
	public double getAvgSpeedsGraphs(int timeInterval) {
		double speed = timeIntervals.get(timeInterval).getAvgSpeeds();
		return Double.isNaN(speed)?link.getLength()<link.getFreespeed()?link.getLength():link.getFreespeed():speed;
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
	public double getAvgTravelTimesGraphs(int timeInterval) {
		double travelTime = timeIntervals.get(timeInterval).getAvgTravelTimes();
		return Double.isNaN(travelTime)?link.getLength()/link.getFreespeed()<1?1:link.getLength()/link.getFreespeed():travelTime;
	}
	public double getConcentration(int timeInterval) {
		double k = timeIntervals.get(timeInterval).getConcentration();
		return Double.isNaN(k)?0:k;
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
