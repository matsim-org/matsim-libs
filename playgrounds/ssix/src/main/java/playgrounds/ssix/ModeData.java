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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * @author ssix
 * A class intended to contain "static" mode-dependent data (vehicle type, speed etc.)
 * as well as dynamic data used in the mobsim (speed, flow of the mode)
 * as well as methods to store and update this data.
 */

public class ModeData {
	
	private String modeId;
	private VehicleType vehicleType=null;//      Maybe keeping global data in the EventHandler can be smart (ssix, 25.09.13)
									     //	     So far programmed to contain also global data, i.e. data without a specific vehicleType (ssix, 30.09.13)
	public int numberOfAgents;
	//private int numberOfDrivingAgents;//dynamic variable counting live agents on the track
	private double permanentDensity;
	private double permanentAverageVelocity;
	private double permanentFlow;
		
	private Map<Id<Vehicle>,Double> lastSeenOnStudiedLinkEnter;//records last entry time for every person, but also useful for getting actual number of people in the simulation
	private int speedTableSize;
	private List<Double> speedTable;
	private Double flowTime;
	private List<Double> flowTable;
	private List<Double> flowTable900;
	private List<Double> lastXFlows;//recording a number of flows to ensure stability
	private List<Double> lastXFlows900;
	private boolean speedStability;
	private boolean flowStability;
	
	public ModeData(){}
	
	public ModeData(String id, VehicleType vT){
		this.modeId = id;
		this.vehicleType = vT;
	}
	
	public void handle(LinkEnterEvent event){
		if (event.getLinkId().equals(FundamentalDiagramsNmodes.studiedMeasuringPointLinkId)){
			Id<Vehicle> personId = event.getVehicleId();
			double nowTime = event.getTime();
			
			//Updating flow, speed
			//this.updateFlow(nowTime, this.vehicleType.getPcuEquivalents());
			this.updateFlow900(nowTime, this.vehicleType.getPcuEquivalents());
			this.updateSpeedTable(nowTime, personId);
			
			//Checking for stability
			//Making sure all agents are on the track before testing stability
			//Also waiting half an hour to let the database build itself.
			
			//System.out.println(nowTime);
			if ((this.getNumberOfDrivingAgents() == this.numberOfAgents) && (nowTime > 1800)){//TODO empirical factor
				if (!(this.speedStability)){
					this.checkSpeedStability();
				}
				if (!(this.flowStability)){
					this.checkFlowStability900();
				}
			}
		}
	}
	
	//Utility methods
	/*public void updateFlow(double nowTime, double pcu_person) {
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
		updateLastXFlows();
	}*/
	
	public void updateFlow900(double nowTime, double pcu_person){
		if (nowTime == this.flowTime.doubleValue()){//Still measuring the flow of the same second
			Double nowFlow = this.flowTable900.get(0);
			this.flowTable900.set(0, nowFlow.doubleValue()+pcu_person);
		} else {//Need to offset the current data in order to update
			int timeDifference = (int) (nowTime-this.flowTime.doubleValue());
			if (timeDifference<900){
				for (int i=899-timeDifference; i>=0; i--){
					this.flowTable900.set(i+timeDifference, this.flowTable900.get(i).doubleValue());
				}
				if (timeDifference > 1){
					for (int i = 1; i<timeDifference; i++){
						this.flowTable900.set(i, 0.);
					}
				}
				this.flowTable900.set(0, pcu_person);
			} else {
				flowTableReset();
			}
			this.flowTime = new Double(nowTime);
		}
		updateLastXFlows900();
	}
	
	/*private void updateLastXFlows(){
		Double nowFlow = new Double(this.getActualFlow());
		for (int i=DreieckNmodes.NUMBER_OF_MEMORIZED_FLOWS-2; i>=0; i--){
			this.lastXFlows.set(i+1, this.lastXFlows.get(i).doubleValue());
		}
		this.lastXFlows.set(0, nowFlow);
	}*/
	
	private void updateLastXFlows900(){
		Double nowFlow = new Double(this.getActualFlow900());
		for (int i=DreieckNmodes.NUMBER_OF_MEMORIZED_FLOWS-2; i>=0; i--){
			this.lastXFlows900.set(i+1, this.lastXFlows900.get(i).doubleValue());
		}
		this.lastXFlows900.set(0, nowFlow);
	}
	
	public void updateSpeedTable(double nowTime, Id<Vehicle> personId){
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
		//this.numberOfDrivingAgents = this.lastSeenOnStudiedLinkEnter.size();
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
		if (relativeDeviances < 0.0005){
			this.speedStability = true;
			System.out.println("Reaching a certain speed stability in mode: "+modeId);
		} else {
			this.speedStability = false;
		}
	}
	
	/*public void checkFlowStability(){
		///*Method 1: Relative Deviances
		double relativeDeviances = 0.;
		double averageFlow = 0;
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MEMORIZED_FLOWS; i++){
			averageFlow += this.lastXFlows.get(i).doubleValue();
		}
		averageFlow /= DreieckNmodes.NUMBER_OF_MEMORIZED_FLOWS;
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MEMORIZED_FLOWS; i++){
			relativeDeviances += Math.pow( ((this.lastXFlows.get(i).doubleValue() - averageFlow) / averageFlow) , 2);
		}
		relativeDeviances /= DreieckNmodes.NUMBER_OF_MEMORIZED_FLOWS;//Taking away dependence on list size.
		if (relativeDeviances < 0.000001){TO DO Here a VERY empirical factor
			this.flowStability = true;
			System.out.println("Reaching a certain flow stability in mode: "+modeId);
		} else {
			this.flowStability = false;
		}//*//*
		//Method 2: absolute deviances
		double absoluteDeviances = this.lastXFlows.get(this.lastXFlows.size()-1) - this.lastXFlows.get(0);
		if (Math.abs(absoluteDeviances) < 2){
			this.flowStability = true;
			System.out.println("Reaching a certain flow stability in mode: "+modeId);
		} else {
			this.flowStability = false;
		}
		
	}*/
	
	public void checkFlowStability900(){
		//Method 2: absolute deviances
		//System.out.println("lastXflows for "+this.modeId+" = "+this.lastXFlows900);
		double absoluteDeviances = this.lastXFlows900.get(this.lastXFlows900.size()-1) - this.lastXFlows900.get(0);
		if (Math.abs(absoluteDeviances) < 1){
			this.flowStability = true;
			System.out.println("Reaching a certain flow stability in mode: "+modeId);
		} else {
			this.flowStability = false;
		}
		
	}
	
	public void initDynamicVariables() {
		//numberOfAgents for each mode should be initialized at this point
		this.decideSpeedTableSize();
		this.speedTable = new LinkedList<Double>();
		for (int i=0; i<this.speedTableSize; i++){
			this.speedTable.add(0.);
		}
		this.flowTime = 0.;
		/*
		this.flowTable = new LinkedList<Double>();
		for (int i=0; i<3600; i++){
			this.flowTable.add(0.);
		}*/
		this.flowTable900 = new LinkedList<Double>();
		for (int i=0; i<900; i++){
			this.flowTable900.add(0.);
		}
		/*this.lastXFlows = new LinkedList<Double>();
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MEMORIZED_FLOWS; i++){
			this.lastXFlows.add(0.);
		}*/
		this.lastXFlows900 = new LinkedList<Double>();
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MEMORIZED_FLOWS; i++){
			this.lastXFlows900.add(0.);
		}
		this.speedStability = false;
		this.flowStability = false;
		this.lastSeenOnStudiedLinkEnter = new TreeMap<>();
		this.permanentDensity = 0.;
		this.permanentAverageVelocity =0.;
		this.permanentFlow = 0.;
	}
	
	public void reset(){
		this.speedTable.clear();
		this.flowTableReset();
		this.lastXFlows.clear();
		this.speedStability = false;
		this.flowStability = false;
	}

	private void decideSpeedTableSize() {
		//Ensures a significant speed sampling for every mode size
		//Is pretty empirical and can be changed if necessary (ssix, 16.10.13)
		if (this.numberOfAgents >= 500) {
			this.speedTableSize = 50;
		} else if (this.numberOfAgents >= 100) {
			this.speedTableSize = 20;
		} else if (this.numberOfAgents >= 10) {
			this.speedTableSize = 10;
		} else if (this.numberOfAgents >  0) {
			this.speedTableSize = this.numberOfAgents;
		} else { //case no agents in mode
			this.speedTableSize = 1;
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
	
	public void saveDynamicVariables(){
		//NB: Should not be called upon a modeData without a vehicleType, as this.vehicleType will be null and will throw an exception.
		this.permanentDensity = this.numberOfAgents / (DreieckNmodes.length*3) *1000 * this.vehicleType.getPcuEquivalents();
		this.permanentAverageVelocity = this.getActualAverageVelocity();
		System.out.println("Calculated permanent Speed from "+modeId+"'s lastXSpeeds : "+speedTable+"\nResult is : "+this.permanentAverageVelocity);
		//this.permanentFlow = this.getActualFlow();
		this.permanentFlow = /*this.getActualFlow900();*///Done: Sliding average instead of taking just the last value (seen to be sometimes farther from the average than expected)
							   this.getSlidingAverageLastXFlows900();
		System.out.println("Calculated permanent Flow from "+modeId+"'s lastXFlows900 : "+lastXFlows900+"\nResult is :"+this.permanentFlow);	
	}
	
	//Getters/Setters
	public VehicleType getVehicleType(){
		return this.vehicleType;
	}
	
	public String getModeId(){
		return this.modeId;
	}
	
	public double getActualAverageVelocity(){
		double nowSpeed = 0.;
		for (int i=0; i<this.speedTableSize; i++){
			nowSpeed += this.speedTable.get(i);
		}
		nowSpeed /= this.speedTableSize;
		return nowSpeed;
	}
	
	/*public double getActualFlow(){
		double nowFlow = 0.;
		for (int i=0; i<3600; i++){
			nowFlow += this.flowTable.get(i);
		}
		return nowFlow;
	}*/
	
	public double getActualFlow900(){
		double nowFlow = 0.;
		for (int i=0; i<900; i++){
			nowFlow += this.flowTable900.get(i);
		}
		return nowFlow*4;
	}
	
	public double getSlidingAverageLastXFlows900(){
		double average = 0;
		for (double flow : this.lastXFlows900){ average += flow; }
		return average / DreieckNmodes.NUMBER_OF_MEMORIZED_FLOWS;
	}
	
	public boolean isSpeedStable(){
		return this.speedStability;
	}
	
	public boolean isFlowStable(){
		return this.flowStability;
	}
	
	public void setnumberOfAgents(int n){
		this.numberOfAgents = n;
	}

	public double getPermanentDensity(){
		return this.permanentDensity;
	}

	public void setPermanentDensity(double permanentDensity) {
		this.permanentDensity = permanentDensity;
	}

	public double getPermanentAverageVelocity(){
		return this.permanentAverageVelocity;
	}
	
	public void setPermanentAverageVelocity(double permanentAverageVelocity) {
		this.permanentAverageVelocity = permanentAverageVelocity;
	}

	public double getPermanentFlow(){
		return this.permanentFlow;
	}
	
	public void setPermanentFlow(double permanentFlow) {
		this.permanentFlow = permanentFlow;
	}

	public int getNumberOfDrivingAgents() {
		return this.lastSeenOnStudiedLinkEnter.size();
	}
	
	public Map<Id<Vehicle>, Double> getLastSeenOnStudiedLinkEnter() {
		return lastSeenOnStudiedLinkEnter;
	}

	public List<Double> getSpeedTable() {
		return this.speedTable;
	}
}