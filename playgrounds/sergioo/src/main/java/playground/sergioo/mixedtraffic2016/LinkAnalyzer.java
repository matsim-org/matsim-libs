package playground.sergioo.mixedtraffic2016;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

public class LinkAnalyzer implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler {
	
	private class IntervalInfo {
		double sumSpeed = 0;
		double weightsSpeed = 0;
		double flow = 0;
		double sumDensity = 0;
		double weightsDensity = 0;
		Map<String, Double> sumSpeedM = new HashMap<>();
		Map<String, Double> weightsSpeedM = new HashMap<>();
		Map<String, Double> flowM = new HashMap<>();
		Map<String, Double> sumDensityM = new HashMap<>();
		Map<String, Double> weightsDensityM = new HashMap<>();
		
		private IntervalInfo(Collection<String> modes) {
			for(String mode:modes) {
				sumSpeedM.put(mode, 0.0);
				weightsSpeedM.put(mode, 0.0);
				flowM.put(mode, 0.0);
				sumDensityM.put(mode, 0.0);
				weightsDensityM.put(mode, 0.0);
			}
		}
	}
	
	private Double interval = 15.0*60;
	private Map<Integer, IntervalInfo> intervals = new HashMap<>();
	private Id<Link> linkId;
	private double length;
	
	private Map<Id<Vehicle>, Double> prevTimes = new HashMap<>();
	private double prevDTime = 0;
	private double numVehicles = 0;
	private Map<String, Double> numVehiclesM = new HashMap<>();
	private Map<Id<Vehicle>, String> modes = new HashMap<>();
	public double maxNumVehicles;
	
	public LinkAnalyzer(Id<Link> linkId, Double interval, Collection<String> modes, Network network, double totalTime) {
		this.linkId = linkId;
		this.length = network.getLinks().get(linkId).getLength();
		if(interval != null && interval>0)
			this.interval = interval;
		for(int time=0; time<totalTime/interval; time++)
			intervals.put(time, new IntervalInfo(modes));
		for(String mode:modes)
			numVehiclesM.put(mode, 0.0);
	}

	@Override
	public void reset(int iteration) {
		
	}

	public double getDensity(int time) {
		double weight = intervals.get(time).weightsDensity;
		return intervals.get(time).sumDensity/(weight==0?1:weight);
	}
	
	public double getSpeed(int time) {
		double weight = intervals.get(time).weightsSpeed;
		return intervals.get(time).sumSpeed/(weight==0?1:weight);
	}
	
	public double getFlow(int time) {
		return intervals.get(time).flow/interval;
	}
	
	public double getDensity(String mode, int time) {
		double weight = intervals.get(time).weightsDensityM.get(mode);
		return intervals.get(time).sumDensityM.get(mode)/(weight==0?1:weight);
	}
	
	public double getSpeed(String mode, int time) {
		double weight = intervals.get(time).weightsSpeedM.get(mode);
		return intervals.get(time).sumSpeedM.get(mode)/(weight==0?1:weight);
	}
	
	public double getFlow(String mode, int time) {
		return intervals.get(time).flowM.get(mode)/interval;
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(event.getLinkId().equals(linkId) || event.getLinkId().toString().equals("20M")) {
			String mode = modes.get(event.getVehicleId());
			Double prevTime = prevTimes.get(event.getVehicleId());
			double time = prevTime;
			for(int i=(int) (prevTime/interval); i<(int) (event.getTime()/interval); i++) {
				double weight = ((i+1)*interval - time);
				intervals.get(i).weightsSpeed += weight;
				Map<String, Double> map = intervals.get(i).weightsSpeedM;
				map.put(mode, map.get(mode) + weight);
				intervals.get(i).sumSpeed += weight*length/(event.getTime()-prevTime);
				map = intervals.get(i).sumSpeedM;
				map.put(mode, map.get(mode) + weight*length/(event.getTime()-prevTime));
				time = (i+1)*interval;
			}
			double weight = (event.getTime()-time);
			intervals.get((int)(event.getTime()/interval)).weightsSpeed += weight;
			Map<String, Double> map = intervals.get((int)(event.getTime()/interval)).weightsSpeedM;
			map.put(mode, map.get(mode) + weight);
			intervals.get((int)(event.getTime()/interval)).sumSpeed += weight*length/(event.getTime()-prevTime);
			map = intervals.get((int)(event.getTime()/interval)).sumSpeedM;
			map.put(mode, map.get(mode) + weight*length/(event.getTime()-prevTime));
			time = prevDTime;
			for(int i=(int) (prevDTime/interval); i<(int) (event.getTime()/interval); i++) {
				weight = ((i+1)*interval - time);
				intervals.get(i).weightsDensity += weight;
				map = intervals.get(i).weightsDensityM;
				map.put(mode, map.get(mode) + weight);
				intervals.get(i).sumDensity += weight*numVehicles/length;
				map = intervals.get(i).sumDensityM;
				map.put(mode, map.get(mode) + weight*numVehiclesM.get(mode)/length);
				time = (i+1)*interval;
			}
			weight = (event.getTime() - time);
			intervals.get((int)(event.getTime()/interval)).weightsDensity += weight;
			map = intervals.get((int)(event.getTime()/interval)).weightsDensityM;
			map.put(mode, map.get(mode) + weight);
			intervals.get((int)(event.getTime()/interval)).sumDensity += weight*numVehicles/length;
			map = intervals.get((int)(event.getTime()/interval)).sumDensityM;
			map.put(mode, map.get(mode) + weight*numVehiclesM.get(mode)/length);
			numVehicles--;
			numVehiclesM.put(mode, numVehiclesM.get(mode)-1);
			prevDTime = event.getTime();
			intervals.get((int)(event.getTime()/interval)).flow++;
			map = intervals.get((int)(event.getTime()/interval)).flowM;
			map.put(mode, map.get(mode) + 1);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getLinkId().equals(linkId) || event.getLinkId().toString().equals("20M")) {
			String mode = modes.get(event.getVehicleId());
			double time = prevDTime;
			for(int i=(int) (prevDTime/interval); i<(int) (event.getTime()/interval); i++) {
				double weight = ((i+1)*interval - time);
				intervals.get(i).weightsDensity += weight;
				Map<String, Double> map = intervals.get(i).weightsDensityM;
				map.put(mode, map.get(mode) + weight);
				intervals.get(i).sumDensity += weight*numVehicles/length;
				map = intervals.get(i).sumDensityM;
				map.put(mode, map.get(mode) + weight*numVehiclesM.get(mode)/length);
				time = (i+1)*interval;
			}
			double weight = (event.getTime() - time);
			intervals.get((int)(event.getTime()/interval)).weightsDensity += weight;
			Map<String, Double> map = intervals.get((int)(event.getTime()/interval)).weightsDensityM;
			map.put(mode, map.get(mode) + weight);
			intervals.get((int)(event.getTime()/interval)).sumDensity += weight*numVehicles/length;
			map = intervals.get((int)(event.getTime()/interval)).sumDensityM;
			map.put(mode, map.get(mode) + weight*numVehiclesM.get(mode)/length);
			numVehicles++;
			if(numVehicles>maxNumVehicles)
				maxNumVehicles = numVehicles;
			numVehiclesM.put(mode, numVehiclesM.get(mode)+1);
			prevDTime = event.getTime();
			prevTimes.put(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		modes.put(event.getVehicleId(), event.getNetworkMode());
		if(event.getLinkId().equals(linkId) || event.getLinkId().toString().equals("20M")) {
			//TODO
			numVehicles++;
			numVehiclesM.put(event.getNetworkMode(), numVehiclesM.get(event.getNetworkMode())+1);
			
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if(event.getLinkId().equals(linkId) || event.getLinkId().toString().equals("20M")) {
			//TODO
			numVehicles--;
			numVehiclesM.put(event.getNetworkMode(), numVehiclesM.get(event.getNetworkMode())-1);
		}
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		// TODO Auto-generated method stub
		System.out.println();
	}

}
