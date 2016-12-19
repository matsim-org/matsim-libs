package playground.sergioo.mixedtraffic2016;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

public class AnalizeLink implements LinkEnterEventHandler, LinkLeaveEventHandler{

	private static final Integer INTERVAL = 15*60;
	
	private class IntervalInfo {
		double sumSpeed = 0;
		double weightsSpeed = 0;
		double flow = 0;
		double sumDensity = 0;
		double weightsDensity = 0;
	}
	
	private Map<Integer, IntervalInfo> intervals = new HashMap<>();
	private Id<Link> linkId;
	private double length;
	
	private Map<Id<Vehicle>, Double> prevTimes = new HashMap<>();
	private double prevDTime = 0;
	private double numVehicles = 0;
	
	public AnalizeLink(Id<Link> linkId, Collection<String> modes, Network network, double totalTime) {
		this.linkId = linkId;
		this.length = network.getLinks().get(linkId).getLength();
		for(int time=0; time<totalTime/INTERVAL; time++)
			intervals.put(time, new IntervalInfo());
	}

	@Override
	public void reset(int iteration) {
		
	}

	public double getDensity(int time) {
		double weight = intervals.get(time).weightsDensity;
		return intervals.get(time).sumDensity/weight==0?1:weight;
	}
	
	public double getSpeed(int time) {
		double weight = intervals.get(time).weightsSpeed;
		return intervals.get(time).sumSpeed/weight==0?1:weight;
	}
	
	public double getFlow(int time) {
		return intervals.get(time).flow/INTERVAL;
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(event.getLinkId().equals(linkId)) {
			Double prevTime = prevTimes.get(event.getVehicleId());
			double speed = (event.getTime() - prevTime);
			double time = prevTime;
			for(int i=(int) (prevTime/INTERVAL); i<event.getTime()/INTERVAL; i++) {
				double weight = ((i+1)*INTERVAL - time);
				intervals.get(i).weightsSpeed += weight;
				intervals.get(i).sumSpeed += weight*length/(event.getTime()-prevTime);
				time = (i+1)*INTERVAL;
			}
			double weight = (event.getTime()-time);
			intervals.get((int)(event.getTime()/INTERVAL)).weightsSpeed += weight;
			intervals.get((int)(event.getTime()/INTERVAL)).sumSpeed += weight*length/(event.getTime()-prevTime);
			time = prevDTime;
			for(int i=(int) (prevDTime/INTERVAL); i<event.getTime()/INTERVAL; i++) {
				weight = ((i+1)*INTERVAL - time);
				intervals.get(i).weightsDensity += weight;
				intervals.get(i).sumDensity += weight*numVehicles/length;
				time = (i+1)*INTERVAL;
			}
			weight = (event.getTime() - time);
			intervals.get((int)(event.getTime()/INTERVAL)).weightsDensity += weight;
			intervals.get((int)(event.getTime()/INTERVAL)).sumDensity += weight*numVehicles/length;
			numVehicles--;
			prevDTime = event.getTime();
			intervals.get((int)(event.getTime()/INTERVAL)).flow++;
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getLinkId().equals(linkId)) {
			double time = prevDTime;
			for(int i=(int) (prevDTime/INTERVAL); i<event.getTime()/INTERVAL; i++) {
				double weight = ((i+1)*INTERVAL - time);
				intervals.get(i).weightsDensity += weight;
				intervals.get(i).sumDensity += weight*numVehicles/length;
				time = (i+1)*INTERVAL;
			}
			double weight = (event.getTime() - time);
			intervals.get((int)(event.getTime()/INTERVAL)).weightsDensity += weight;
			intervals.get((int)(event.getTime()/INTERVAL)).sumDensity += weight*numVehicles/length;
			numVehicles++;	
			prevDTime = event.getTime();
		}
	}

}
