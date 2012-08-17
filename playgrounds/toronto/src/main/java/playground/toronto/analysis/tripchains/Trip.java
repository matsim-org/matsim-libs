package playground.toronto.analysis.tripchains;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

public class Trip {
	private List<TripComponent> components;
	public Integer zone_o;
	public Integer zone_d;
	private List<String> routeSet;
	private Id pid;
	
	public Trip(Id personId){
		this.components = new ArrayList<TripComponent>();
		this.routeSet = new ArrayList<String>();
		this.pid = personId;
	}
	
	public void addComponent(TripComponent tc){
		this.components.add(tc);
		if (tc instanceof InTransitVehicleComponent){
			this.routeSet.add(((InTransitVehicleComponent) tc).getFullRouteName());
		}
	}
	
	public List<String> getTransitRoute(){
		return this.routeSet;
	}
	
	public Id getPersonId(){
		return this.pid;
	}
	
	public double getWalkTime(){
		double time = 0;
		for (TripComponent tc : this.components){
			if (tc instanceof WalkComponent)
				time += (tc.getEndtime() - tc.getStartTime());
		}
		return time;
	}
	
	public double getWaitTime(){
		double time = 0;
		for (TripComponent tc : this.components){
			if (tc instanceof WaitForTransitComponent)
				time += (tc.getEndtime() - tc.getStartTime());
		}
		return time;
	}
	
	/**
	 * Get in-vehicle-travel-time (IVTT)
	 * @return
	 */
	public double getIVTT(){
		double time = 0;
		for (TripComponent tc : this.components){
			if (tc instanceof InTransitVehicleComponent)
				time += (tc.getEndtime() - tc.getStartTime());
		}
		return time;
	}
	
	public double getAutoDriveTime(){
		double time = 0;
		for (TripComponent tc : this.components){
			if (tc instanceof AutoDriveComponent)
				time += (tc.getEndtime() - tc.getStartTime());
		}
		return time;
	}
	
	public double getTotalTripTime(){
		double time = 0;
		for (TripComponent tc : this.components){
			time += (tc.getEndtime() - tc.getStartTime());
		}
		return time;
	}
}
