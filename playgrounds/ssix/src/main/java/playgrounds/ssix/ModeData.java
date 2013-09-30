/* *********************************************************************** *
 * project: org.matsim.*
 * ModeData													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playgrounds.ssix;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.vehicles.VehicleType;

/**
 * @author ssix
 * A class intended to contain "static" mode-dependent data (vehicle type, speed etc.)
 * as well as dynamic data used in the mobsim (speed, flow of the mode)
 * as well as methods to store and update this data.
 */

public class ModeData {
	
	private Id modeId;
	private VehicleType vehicleType;//TODO Ensure all methods can work without a specific vehicle type (needed for storing global data)
									//      Maybe keeping global data in the EventHandler can be smart (ssix, 25.09.13)
									//		Sofar programmed to contain also global data (ssix, 30.09.13)
	public int numberOfAgents;
	
	private Map<Id,Double> lastSeenOnStudiedLinkEnter;//records last entry time for every person, but also useful for getting actual number of people in the simulation
	private int speedTableSize;
	private List<Double> speedTable;
	private Double flowTime;
	private List<Double> flowTable;
	private boolean speedStability;
	
	public ModeData(){}
	
	public ModeData(Id id, VehicleType vT){
		this.modeId = id;
		this.vehicleType = vT;
	}
	
	public void handle(LinkEnterEvent event){
		//TODO
		if (event.getLinkId().equals(FundamentalDiagramsNmodes.studiedMeasuringPointLinkId)){
			Id personId = event.getPersonId();
			double nowTime = event.getTime();
			
			//Updating flow, speed
			this.updateFlow(nowTime, this.vehicleType.getPcuEquivalents());
			this.updateSpeed(nowTime, personId);
			
			//Checking for speed stability
			this.checkSpeedStability();
		}
	}
	
	//Utility methods
	public void updateFlow(double nowTime, double pcu_person) {
		if (nowTime == this.flowTime.doubleValue()){//Still measuring the flow of the same second
			Double nowFlow = this.flowTable.get(0);
			this.flowTable.set(0, nowFlow.doubleValue()+pcu_person);
		} else {//Need to offset the current data in order to update
			int timeDifference = (int) (nowTime-this.flowTime.doubleValue());
			if (timeDifference<3600){
				for (int i=3599-timeDifference; i>=0; i--){
					this.flowTable.set(i+timeDifference, this.flowTable.get(i).doubleValue());
				}
				if (timeDifference > 1){
					for (int i = 1; i<timeDifference; i++){
						this.flowTable.set(i, 0.);
					}
				}
				this.flowTable.set(0, pcu_person);
			} else {
				flowTableReset();
			}
			this.flowTime = new Double(nowTime);
		}
	}
	
	public void updateSpeed(double nowTime, Id personId){
		if (this.lastSeenOnStudiedLinkEnter.containsKey(personId)){
			double lastSeenTime = lastSeenOnStudiedLinkEnter.get(personId);
			double speed = DreieckNmodes.length * 3 / (nowTime-lastSeenTime);//in m/s!!
			for (int i=speedTableSize-2; i>=0; i--){
				this.speedTable.set(i+1, this.speedTable.get(i).doubleValue());
			}
			this.speedTable.set(0, speed);
			
			this.lastSeenOnStudiedLinkEnter.put(personId,nowTime);
		} else {
			this.lastSeenOnStudiedLinkEnter.put(personId, nowTime);
		}
	}
	
	public void checkSpeedStability(){
		double relativeDeviances = 0.;
		double averageSpeed = 0;
		for (int i=0; i<this.speedTableSize; i++){
			averageSpeed += this.speedTable.get(i).doubleValue();
		}
		averageSpeed /= this.speedTableSize;
		for (int i=0; i<this.speedTableSize; i++){
			relativeDeviances += Math.pow( ((this.speedTable.get(i).doubleValue() - averageSpeed) / averageSpeed) , 2);
		}
		relativeDeviances /= DreieckNmodes.NUMBER_OF_MODES;//taking dependence on number of modes away
		if (relativeDeviances < 0.0001){
			this.speedStability = true;
		}
		this.speedStability = false;
	}
	
	public boolean isSpeedStable(){
		if (speedStability){
			return true;
		}
		return false;
	}
	
	public void initDynamicVariables() {
		//numberOfAgents for each mode should be initialized at this point
		this.decideSpeedTableSize();
		this.speedTable = new LinkedList<Double>();
		for (int i=0; i<this.speedTableSize; i++){
			this.speedTable.add(0.);
		}
		
		this.flowTime = 0.;
		this.flowTable = new LinkedList<Double>();
		for (int i=0; i<3600; i++){
			this.flowTable.add(0.);
		}
		this.speedStability = false;
		this.lastSeenOnStudiedLinkEnter = new TreeMap<Id,Double>();
	}

	private void decideSpeedTableSize() {
		//Ensures a significant speed sampling for every mode size
		if (this.numberOfAgents >= 100) {
			this.speedTableSize = 50;
		} else if (this.numberOfAgents >= 60) {
			this.speedTableSize = 30;
		} else if (this.numberOfAgents >= 20) {
			this.speedTableSize = 20;
		} else {
			this.speedTableSize = this.numberOfAgents;
		}
	}
	
	@Override
	public String toString(){
		VehicleType vT = this.vehicleType;
		String str = "(id="+this.modeId+", max_v="+vT.getMaximumVelocity()+", pcu="+vT.getPcuEquivalents()+")";
		return str;
	}

	private void flowTableReset() {
		for (int i=0; i<3600; i++){
			this.flowTable.set(i, 0.);
		}
	}
	
	//Getters/Setters
	public VehicleType getVehicleType(){
		return this.vehicleType;
	}
	
	public Id getModeId(){
		return this.modeId;
	}
	
	public void setnumberOfAgents(int n){
		this.numberOfAgents = n;
	}
}
