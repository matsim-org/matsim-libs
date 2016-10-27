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

package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.VehicleType;

import playground.agarwalamit.utils.ListUtils;

/**
 * @author amit after ssix
 */ 
/**
 * A class intended to contain "static" mode-dependent data (vehicle type, speed etc.)
 * as well as dynamic data used in the mobsim (speed, flow of the mode)
 * as well as methods to store and update this data.
 */

class TravelModesFlowDynamicsUpdator {

	private final int NUMBER_OF_MEMORIZED_FLOWS = 10;
	private Id<VehicleType> modeId;
	private VehicleType vehicleType=null;//      Maybe keeping global data in the EventHandler can be smart (ssix, 25.09.13)
	//	     So far programmed to contain also global data, i.e. data without a specific vehicleType (ssix, 30.09.13)
	public int numberOfAgents;
	//private int numberOfDrivingAgents;//dynamic variable counting live agents on the track
	private double permanentDensity;
	private double permanentAverageVelocity;
	private double permanentFlow;

	private Map<Id<Person>,Double> lastSeenOnStudiedLinkEnter;//records last entry time for every person, but also useful for getting actual number of people in the simulation
	private int speedTableSize;
	private List<Double> speedTable;
	private Double flowTime;
	private List<Double> flowTable900;
	private List<Double> lastXHourlyFlows;//recording a number of flows to ensure stability
	private boolean speedStability;
	private boolean flowStability;
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	private final int noOfModes;

	TravelModesFlowDynamicsUpdator(int noOfModes){
		this.noOfModes = noOfModes;
	}

	TravelModesFlowDynamicsUpdator(VehicleType vT, int noOfModes){
		this.vehicleType = vT;
		this.modeId = this.vehicleType.getId();
		this.noOfModes = noOfModes;
	}

	void handle(LinkEnterEvent event){
		if (event.getLinkId().equals(GlobalFlowDynamicsUpdator.FLOW_DYNAMICS_UPDATE_LINK)){
			Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
			double nowTime = event.getTime();

			this.updateFlow900(nowTime, this.vehicleType.getPcuEquivalents());
			this.updateSpeedTable(nowTime, personId);

			//Checking for stability
			//Making sure all agents are on the track before testing stability
			//Also waiting half an hour to let the database build itself.

			if ((this.getNumberOfDrivingAgents() == this.numberOfAgents) && (nowTime > InputsForFDTestSetUp.MAX_ACT_END_TIME * 2)){
				if (!(this.speedStability)){
					this.checkSpeedStability();
				}
				if (!(this.flowStability)){
					this.checkFlowStability900();
				}
			}
		}
	}

	public void handle(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	public void handle(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}
	
	void updateFlow900(double nowTime, double pcuPerson){
		if (nowTime == this.flowTime.doubleValue()){//Still measuring the flow of the same second
			Double nowFlow = this.flowTable900.get(0);
			this.flowTable900.set(0, nowFlow.doubleValue()+pcuPerson);
		} else {//Need to offset the new flow table from existing flow table.
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
				this.flowTable900.set(0, pcuPerson);
			} else {
				flowTableReset();
			}
			this.flowTime = new Double(nowTime);
		}
		updateLastXFlows900();
	}

	private void updateLastXFlows900(){
		Double nowFlow = new Double(this.getCurrentHourlyFlow());
		for (int i=NUMBER_OF_MEMORIZED_FLOWS-2; i>=0; i--){
			this.lastXHourlyFlows.set(i+1, this.lastXHourlyFlows.get(i).doubleValue());
		}
		this.lastXHourlyFlows.set(0, nowFlow);
	}

	void updateSpeedTable(double nowTime, Id<Person> personId){
		if (this.lastSeenOnStudiedLinkEnter.containsKey(personId)){
			double lastSeenTime = lastSeenOnStudiedLinkEnter.get(personId);
			double speed = InputsForFDTestSetUp.LINK_LENGTH * 3 / (nowTime-lastSeenTime);//in m/s!!
			for (int i=speedTableSize-2; i>=0; i--){
				this.speedTable.set(i+1, this.speedTable.get(i).doubleValue());
			}
			this.speedTable.set(0, speed);

			this.lastSeenOnStudiedLinkEnter.put(personId,nowTime);
		} else {
			this.lastSeenOnStudiedLinkEnter.put(personId, nowTime);
		}
	}

	void checkSpeedStability(){
		double relativeDeviances = 0.;
		double averageSpeed = ListUtils.doubleMean(this.speedTable);
		for (int i=0; i<this.speedTableSize; i++){
			relativeDeviances += Math.pow( (this.speedTable.get(i).doubleValue() - averageSpeed) / averageSpeed, 2);
		}
		relativeDeviances /= this.noOfModes;//taking dependence on number of modes away
		if (relativeDeviances < 0.0005){
			this.speedStability = true;
			GenerateFundamentalDiagramData.LOG.info("========== Reaching a certain speed stability in mode: "+modeId);
		} else {
			this.speedStability = false;
		}
	}

	void checkFlowStability900(){
//		double relativeDeviances = 0.;
//		double avgFlow = ListUtils.doubleMean(this.lastXHourlyFlows);
//		for(int i=0;i<this.NUMBER_OF_MEMORIZED_FLOWS;i++){
//			relativeDeviances  += Math.pow( (this.lastXHourlyFlows.get(i).doubleValue() - avgFlow)/avgFlow , 2);
//		}
//		relativeDeviances /= this.NUMBER_OF_MEMORIZED_FLOWS;
		
		double absoluteDeviances = this.lastXHourlyFlows.get(this.lastXHourlyFlows.size()-1) - this.lastXHourlyFlows.get(0);
		if (Math.abs(absoluteDeviances) < 1){
//		if(relativeDeviances < 0.05){
			this.flowStability = true;
			if(modeId==null) GenerateFundamentalDiagramData.LOG.info("========== Reaching a certain flow stability for global flow.");
			else GenerateFundamentalDiagramData.LOG.info("========== Reaching a certain flow stability in mode: "+modeId.toString());
		} else {
			this.flowStability = false;
		}
	}

	//ZZ_TODO : following two methods can be combined together ?? amit nov 15
	void initDynamicVariables() {
		//numberOfAgents for each mode should be initialized at this point
		this.decideSpeedTableSize();
		this.speedTable = new LinkedList<>();
		for (int i=0; i<this.speedTableSize; i++){
			this.speedTable.add(0.);
		}
		this.flowTime = 0.;
		this.flowTable900 = new LinkedList<>();

		flowTableReset();

		this.lastXHourlyFlows = new LinkedList<>();
		for (int i=0; i<NUMBER_OF_MEMORIZED_FLOWS; i++){
			this.lastXHourlyFlows.add(0.);
		}
		this.speedStability = false;
		this.flowStability = false;
		this.lastSeenOnStudiedLinkEnter = new TreeMap<>();
		this.permanentDensity = 0.;
		this.permanentAverageVelocity =0.;
		this.permanentFlow = 0.;
	}

	void reset(){
		this.speedTable.clear();
		this.speedStability = false;
		this.flowStability = false;
	}

	private void decideSpeedTableSize() {
		//Ensures a significant speed sampling for every mode size
		//Is pretty empirical and can be changed if necessary (ssix, 16.10.13)
		if (this.numberOfAgents >= 500) this.speedTableSize = 50;
		else if (this.numberOfAgents >= 100) this.speedTableSize = 20;
		else if (this.numberOfAgents >= 10) this.speedTableSize = 10;
		else if (this.numberOfAgents >  0) this.speedTableSize = this.numberOfAgents;
		else { //case no agents in mode
			this.speedTableSize = 1;
		}
	}

	private void flowTableReset() {
		for (int i=0; i<900; i++){
			this.flowTable900.add(0.);
		}
	}

	void saveDynamicVariables(){
		//NB: Should not be called upon a modeData without a vehicleType, as this.vehicleType will be null and will throw an exception.
		this.permanentDensity = this.numberOfAgents / (InputsForFDTestSetUp.LINK_LENGTH*3) *1000 * this.vehicleType.getPcuEquivalents();
		this.permanentAverageVelocity = this.getActualAverageVelocity();
		GenerateFundamentalDiagramData.LOG.info("Calculated permanent Speed from "+modeId+"'s lastXSpeeds : "+speedTable+"\nResult is : "+this.permanentAverageVelocity);
		this.permanentFlow = this.getSlidingAverageOfLastXHourlyFlows();
		GenerateFundamentalDiagramData.LOG.info("Calculated permanent Flow from "+modeId+"'s lastXFlows900 : "+lastXHourlyFlows+"\nResult is :"+this.permanentFlow);	
	}

	VehicleType getVehicleType(){
		return this.vehicleType;
	}

	Id<VehicleType> getModeId(){
		return this.modeId;
	}

	double getActualAverageVelocity(){
		return ListUtils.doubleMean(this.speedTable); 
	}

	double getCurrentHourlyFlow(){
		double nowFlow = ListUtils.doubleSum(this.flowTable900);
		return nowFlow*4;
	}

	double getSlidingAverageOfLastXHourlyFlows(){
		return ListUtils.doubleMean(this.lastXHourlyFlows);
	}

	boolean isSpeedStable(){
		return this.speedStability;
	}

	boolean isFlowStable(){
		return this.flowStability;
	}

	int getnumberOfAgents(){
		return this.numberOfAgents ;
	}
	
	void setnumberOfAgents(int n){
		this.numberOfAgents = n;
	}

	double getPermanentDensity(){
		return this.permanentDensity;
	}

	void setPermanentDensity(double permanentDensity) {
		this.permanentDensity = permanentDensity;
	}

	double getPermanentAverageVelocity(){
		return this.permanentAverageVelocity;
	}

	void setPermanentAverageVelocity(double permanentAverageVelocity) {
		this.permanentAverageVelocity = permanentAverageVelocity;
	}

	double getPermanentFlow(){
		return this.permanentFlow;
	}

	void setPermanentFlow(double permanentFlow) {
		this.permanentFlow = permanentFlow;
	}

	int getNumberOfDrivingAgents() {
		return this.lastSeenOnStudiedLinkEnter.size();
	}
}