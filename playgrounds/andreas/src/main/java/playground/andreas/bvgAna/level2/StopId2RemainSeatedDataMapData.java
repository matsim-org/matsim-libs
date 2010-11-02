/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.andreas.bvgAna.level2;

import org.matsim.core.events.VehicleArrivesAtFacilityEvent;

/**
 * Collects occupancy at arrival and number of agents leaving the vehicle for on <code>VehicleArrivesAtFacilityEvent</code> 
 * 
 * @author aneumann
 *
 */
public class StopId2RemainSeatedDataMapData {
	
	private VehicleArrivesAtFacilityEvent arrivalEvent;
	private final int occupancyAtArrival;
	private int agentsLeaving = 0;
	private int agentsEntering = 0;
	
	public StopId2RemainSeatedDataMapData(VehicleArrivesAtFacilityEvent event, int occupancyAtArrival){
		this.arrivalEvent = event;
		this.occupancyAtArrival = occupancyAtArrival;
	}
	
	/**
	 * @return Returns number of agents which did not leave the vehicle at that stop
	 */
	public int getNumberOfAgentsRemainedSeated(){
		return this.occupancyAtArrival - this.agentsLeaving;
	}
	
	/**
	 * @return Returns the fraction of agents which did not leave the vehicle at the stop based on the occupancy at arrival
	 */
	public double getFractionRemainedSeated(){
		return (double) this.getNumberOfAgentsRemainedSeated() / (double) this.occupancyAtArrival;
	}
	
	/**
	 * @return Returns number of agents which entered the vehicle at that stop
	 */
	public int getNumberOfAgentsEntering(){
		return this.agentsEntering;
	}
	
	/**
	 * @return Returns the fraction of agents which entered the vehicle at the stop based on the occupancy at arrival
	 */
	public double getFractionEntering(){
		return (double) this.getNumberOfAgentsEntering() / (double) this.occupancyAtArrival;
	}
	
	/**
	 * @return Returns number of agents which left the vehicle at that stop
	 */
	public int getNumberOfAgentsLeaving(){
		return this.agentsLeaving;
	}
	
	/**
	 * @return Returns the fraction of agents which left the vehicle at the stop based on the occupancy at arrival
	 */
	public double getFractionLeaving(){
		return (double) this.getNumberOfAgentsLeaving() / (double) this.occupancyAtArrival;
	}
	
	/**
	 * @return Returns the <code>VehicleArrivesAtFacilityEvent</code>
	 */
	public VehicleArrivesAtFacilityEvent getVehicleArrivesAtFacilityEvent(){
		return this.arrivalEvent;
	}

	protected void addAgentLeaving() {
		this.agentsLeaving++;		
	}
	
	protected void addAgentEntering() {
		this.agentsEntering++;		
	}
	
	@Override
	public String toString() {
		return "Event: " + this.getVehicleArrivesAtFacilityEvent() + " - Remained: " + this.getNumberOfAgentsRemainedSeated() + " (" + this.getFractionRemainedSeated() * 100 + "%) - Left: " + 
		this.getNumberOfAgentsLeaving() + " (" + this.getFractionLeaving() * 100 + "%) - Entered: " + this.getNumberOfAgentsEntering() + " (" + this.getFractionEntering() * 100 + "%)";
	}

}
