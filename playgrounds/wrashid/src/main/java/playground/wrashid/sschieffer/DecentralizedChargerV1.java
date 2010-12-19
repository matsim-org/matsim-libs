/* *********************************************************************** *
 * project: org.matsim.*
 * DecentralizedChargerV1.java
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

package playground.wrashid.sschieffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.vehicles.Vehicles;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class DecentralizedChargerV1 {

	private LinkedListValueHashMap<Id, Double> energyConsumptionOfLegs;
	private LinkedListValueHashMap<Id, ParkingIntervalInfo> parkingTimeIntervals;
	//public ArrayList<Double> realChargingTimes;
	//TODO add method to return vehicles in energyConsumptionPlugin
	//LinkedListValueHashMap<Id, Vehicle> vehicles=energyConsumptionPlugin.
	
	
	//private double maxChargePerSlot=chargingSpeedPerSecond*Main.secondsPer15Min;
	
	public int statsInfeasible;
	public int statsTight;
	public int statsLoose;
	
	private double [][] parkingTimesCurrentAgent;
	private double [] parkingLengthCurrentAgent;
	private double [][] chargingTimesCurrentAgent;
	private double [][] drivingTimesCurrentAgent;
	
	private double [] realParkingLengthCurrentAgent;
	private double []  drivingConsumptionCurrentAgent;
	
	private DecentralizedChargerInfo myChargerInfo;
	
	private double populationTotal;
	private double noOfPHEVs;
	private double averagePHEVConsumptionInWatts;
	private double averagePHEVConsumptionInJoules;
	final Controler controler;
	
	
	
	
	/**
	 * Public constructor
	 */
	public DecentralizedChargerV1(Controler controler, EnergyConsumptionPlugin energyConsumptionPlugin, ParkingTimesPlugin parkingTimesPlugin){
		
		this.controler=controler;
		energyConsumptionOfLegs = energyConsumptionPlugin.getEnergyConsumptionOfLegs();
		parkingTimeIntervals = parkingTimesPlugin.getParkingTimeIntervals();
		
	}
	
	/**
	 * Output call of the Decentralized Charger!
	 * @param startChargingTime
	 * @param endChargingTime
	 * @param agentId
	 * @param linkId
	 */
	public void getElectricityFromGrid(double startChargingTime, double endChargingTime, Id agentId){
		System.out.println("Electricity for Agent:"+ agentId.toString() +" from "+startChargingTime +" to " +endChargingTime);
	}
	
	public double calcNumberOfPHEVs(Controler controler){
		populationTotal=controler.getPopulation().getPersons().size();
		return noOfPHEVs = populationTotal*Main.penetrationPercent;
	}
	
	
	public void performChargingAlgorithm() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException {
		
		noOfPHEVs = calcNumberOfPHEVs(controler);
		averagePHEVConsumptionInJoules= getAveragePHEVConsumptionInJoules();
		averagePHEVConsumptionInWatts= getAveragePHEVConsumptionInWatt();
		myChargerInfo = new DecentralizedChargerInfo(noOfPHEVs, averagePHEVConsumptionInJoules); 
		
		myChargerInfo.loadBaseLoadCurveFromTextFile();
		
		myChargerInfo.getFittedLoadCurve();
		
		myChargerInfo.findHighLowIntervals();
		myChargerInfo.findProbabilityDensityFunctions();
		myChargerInfo.findProbabilityRanges();
		myChargerInfo.writeSummary();
		
		/*Loop over all agents
		 * for each agent -make personal list of links and times
		 */
		statsInfeasible=0;
		statsTight=0;
		statsLoose=0;
		
		assignSlotsToAllAgents();
	
	}//end performChargingAlgorithm
	
	
	/**
	 * Loops over all agents
	 * - exctracts their schedules
	 * - checks the schedules with the valley times from myCharger
	 * - checks general and specific feasibilities
	 * - assigns slots
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
	public void assignSlotsToAllAgents() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{

		for (Id personId: controler.getPopulation().getPersons().keySet()){
			
			System.out.println("Start Check Agent:" + personId.toString());
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimeIntervals.get(personId);
			double [] parkingLengthsCurrentAgent=getParkingLengthsCurrentAgent(parkingIntervals);
											
			// get driving legs			
			LinkedList<Double> legEnergyConsumptionList = energyConsumptionOfLegs.get(personId);
			drivingConsumptionCurrentAgent=getdrivingConsumptionCurrentAgent(personId);
			
			drivingTimesCurrentAgent = getDrivingTimesCurrentAgent(parkingTimesCurrentAgent,parkingIntervals);
			
			double totalAgentConsumptionJoule=sumUpEntriesOf1DDoubleArray(drivingConsumptionCurrentAgent); // in Joules
			double totalParkingTimeAgentInSeconds=sumUpEntriesOf1DDoubleArray(parkingLengthsCurrentAgent);
			
			double potentialTotalChargingAgent=Main.chargingSpeedPerSecond*totalParkingTimeAgentInSeconds;
			
			performGeneralFeasibilityCheckAgentBehavior(drivingConsumptionCurrentAgent);
			
			double[] realParkingLengthCurrentAgent=new double[parkingIntervals.size()];
			
			
			System.out.println("Specific Feasibility Test for Agent:" + personId.toString());
			performSpecificFeasibilityCheckAgentBehavior(realParkingLengthCurrentAgent,  totalAgentConsumptionJoule);			
			
			for (int i=0; i< chargingTimesCurrentAgent.length; i++){
				getElectricityFromGrid(chargingTimesCurrentAgent[i][0], chargingTimesCurrentAgent[i][1], personId);
					
			}
			
		}
		
		System.out.println("Statistics:");
		System.out.println("Agent with Infeasible charging patterns: "+ statsInfeasible);
		System.out.println("Agent with Tight charging patterns: "+ statsTight);
		System.out.println("Agent with Loose charging patterns: "+ statsLoose);
	}
	
	
	public void	performSpecificFeasibilityCheckAgentBehavior(double[] realParkingLengthCurrentAgent, double totalAgentConsumptionJoule) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		//special check
		//new realParkingLengthArray - crosschecked with valleys
		
		for (int i=0; i<realParkingLengthCurrentAgent.length; i++){
			realParkingLengthCurrentAgent[i]=myChargerInfo.getFeasibleChargingTimeInInterval(parkingTimesCurrentAgent[i][0], parkingTimesCurrentAgent[i][1], myChargerInfo.getValleyTimes());
		}
		
		double totalRealChargingTimeAgentInSeconds=sumUpEntriesOf1DDoubleArray(realParkingLengthCurrentAgent);
		double realTotalCharging=Main.chargingSpeedPerSecond*totalRealChargingTimeAgentInSeconds;
		double maxChargeInJoulePerSlot=Main.slotLength*Main.chargingSpeedPerSecond; // sec*Joule/sec
		
		// number of slots to book dependent on consumption
		int noOfSlotsToBook= (int) Math.ceil(totalAgentConsumptionJoule/maxChargeInJoulePerSlot);
		//number of feasible slots to book is parkingTime within Valleys = totalRealCHargingTimeAgendInSeconds
		int feasibleNoOfSlotsToBook= (int) Math.ceil(totalRealChargingTimeAgentInSeconds/Main.slotLength);
		
		int noOfSlotInDay=(int)Math.ceil(Main.secondsPerDay/Main.slotLength);
		if (noOfSlotsToBook>noOfSlotInDay){
			System.out.println("infeasible numbers, change charging speed or consumption info.");
			System.out.println("necessary number of charging slots higher than slots in day!");
		}
		int slotsInPeakTime=noOfSlotsToBook-feasibleNoOfSlotsToBook;
		if (slotsInPeakTime<=0){
			slotsInPeakTime=0;
		}
		
		
		int systemConstraint=0;
		if (totalAgentConsumptionJoule>realTotalCharging){
			System.out.println("Special check: infeasible");
			systemConstraint=2;
			statsInfeasible++;
		}
		else if (totalAgentConsumptionJoule==realTotalCharging){
			System.out.println("Special check: tight");
			systemConstraint=1;
			statsTight++;
		}
		else{
			System.out.println("Special check: loose");
			systemConstraint=0;
			statsLoose++;
		}
		
		
		
		boolean feasibilityCheck=false;
		while (feasibilityCheck==false){
			if (systemConstraint>0){
				// tight - book all realCHargingSlotTImes
				System.out.println("get ALL Charging Slots for Agent");
				System.out.println("get "+ slotsInPeakTime +" Charging Slots in PeakTime for Agent");
				
				chargingTimesCurrentAgent=myChargerInfo.bookSlotsAll(parkingTimesCurrentAgent, myChargerInfo.getValleyTimes(),slotsInPeakTime);
				
				
			}
			else{
				// loose
				System.out.println("get"+ noOfSlotsToBook +"Charging Slots for Agent");
				chargingTimesCurrentAgent=myChargerInfo.bookSlots(parkingTimesCurrentAgent, noOfSlotsToBook, slotsInPeakTime);
				
			}
			System.out.println("get Actual Order");
			double [][] actualOrder=getActualOrder(chargingTimesCurrentAgent, drivingTimesCurrentAgent, drivingConsumptionCurrentAgent); 
			
			if (systemConstraint!=2){
				System.out.println("check Feasibility");
				feasibilityCheck=checkChargingOrderForFeasibility(actualOrder, totalAgentConsumptionJoule);
				if (!feasibilityCheck){
					System.out.println("not Feasible - REPEAT");
				}
			}
			else{
				System.out.println("infeasible by default");
				feasibilityCheck=true; //need to give negative feed back to agent planning
				// measure of severity ?
			}
			
		}
	}
	
	
	
	
	
	public double [] getdrivingConsumptionCurrentAgent(Id personId){
			LinkedList<Double> legEnergyConsumptionList = energyConsumptionOfLegs.get(personId);
			int noOfTrips=legEnergyConsumptionList.size();
			drivingConsumptionCurrentAgent = new double[noOfTrips];
			
			for (int i=0;i<legEnergyConsumptionList.size();i++) {
				drivingConsumptionCurrentAgent[i]=legEnergyConsumptionList.get(i);
			}
			return drivingConsumptionCurrentAgent;
	}
	
	
	public double[] getParkingLengthsCurrentAgent(LinkedList<ParkingIntervalInfo> parkingIntervals){
		
		// arrival- departure
		parkingTimesCurrentAgent = new double[parkingIntervals.size()][2];
		for (int i=0;i<parkingIntervals.size();i++) {
			ParkingIntervalInfo parkingIntervalInfo = parkingIntervals.get(i);
			parkingTimesCurrentAgent[i][1]=parkingIntervalInfo.getDepartureTime();
			parkingTimesCurrentAgent[i][0]=parkingIntervalInfo.getArrivalTime();
		}
		// parking lengths
		parkingLengthCurrentAgent = new double[parkingIntervals.size()];
		for (int i=0;i<parkingLengthCurrentAgent.length;i++){
			parkingLengthCurrentAgent[i]=parkingTimesCurrentAgent[i][1]-parkingTimesCurrentAgent[i][0];
			
		}
		// correction of first entry
		if (parkingLengthCurrentAgent[0]<0){
			parkingLengthCurrentAgent[0]=parkingTimesCurrentAgent[0][1]+Main.secondsPerDay-parkingTimesCurrentAgent[0][0];
		}
		return parkingLengthCurrentAgent;
}
	
	public double [][] getActualOrder(double [][] chargingTimesCurrentAgent, double [][]drivingTimesCurrentAgent, double [] drivingConsumptionCurrentAgent){
		double [][] actualOrder = new double [chargingTimesCurrentAgent.length+drivingTimesCurrentAgent.length][3];
		// add chargingtimes and driving times to actualOrder
		for (int i=0; i<chargingTimesCurrentAgent.length; i++){
			actualOrder[i][0]=chargingTimesCurrentAgent[i][0]; //start
			actualOrder[i][1]=chargingTimesCurrentAgent[i][1]; // end
			actualOrder[i][2]=(chargingTimesCurrentAgent[i][1]-chargingTimesCurrentAgent[i][0])*Main.chargingSpeedPerSecond; // charge
		}
		for (int i=0; i<drivingTimesCurrentAgent.length; i++){
			actualOrder[i+chargingTimesCurrentAgent.length][0]=drivingTimesCurrentAgent[i][0];
			actualOrder[i+chargingTimesCurrentAgent.length][1]=drivingTimesCurrentAgent[i][1];
			actualOrder[i+chargingTimesCurrentAgent.length][2]=-1*drivingConsumptionCurrentAgent[i];
			//double [] drivingConsumptionCurrentAgent=getdrivingConsumptionCurrentAgent(personId);
			
		}
		
		// sort actual order by time
		actualOrder= minAtStartMaxAtEnd(actualOrder, 3);
		return actualOrder;
		
	} 
	
	/**
	 * Recursive method to sort double array by first entry in row
	 * 
	 * @param d
	 * @param elementsPerRow
	 * @return
	 */
	public double[][] minAtStartMaxAtEnd(double [][]d, int elementsPerRow){
		int min=0;
		int max=0;
		for (int i=0; i<d.length; i++){
			if (d[i][0]<d[min][0]){
				min=i;
			}
			if(d[i][0]>d[max][0]){
				max=i;
			}
		}
		
		double[][] clone=d.clone();
		clone[0]=d[min];
		clone[d.length-1]=d[max];
		
		
		double [][] leftInMiddle=removeEntryIFromDoubleArray( d, min, elementsPerRow);
		if (min<max)
			{leftInMiddle=removeEntryIFromDoubleArray(leftInMiddle, max-1, elementsPerRow);}
		else
			{leftInMiddle=removeEntryIFromDoubleArray(leftInMiddle, max, elementsPerRow);}
		
		if(leftInMiddle.length>1){
			leftInMiddle=minAtStartMaxAtEnd(leftInMiddle, 3);
		}
		
		// insert middle back into clone
		//System.out.println(leftInMiddle.length);
		for (int i=0; i<leftInMiddle.length; i++){
			clone[i+1]=leftInMiddle[i];
		}
		return clone;
	}
	
	
	public double [][] removeEntryIFromDoubleArray(double [][] d, int i, int elementsPerRow){
		double [][] newD=new double [d.length-1][elementsPerRow];
		int count=0;
		for (int c=0; c<d.length; c++){
			if (c==i){}
			else{
				newD[count]=d[c];
				count++;
			}
		}
		return newD;
	}
	
	
	public boolean checkChargingOrderForFeasibility(double [][] actualOrder, double totalAgentConsumptionJoule){
		boolean check=true;
		// chronological  start end consumption negative / charging positive
		double SOC=Main.startSOCInWattSeconds; // Watt*Sec = Joule // max batteryCharge (max-min)
		double missingCharge=0;
		for (int i=0; i<actualOrder.length; i++){
			
			SOC+=actualOrder[i][2];
			if (SOC<0 ){
				System.out.println("SOC cant be negative");
				check=false;
				
			}
			if (SOC>Main.batteryCapacity)
			{
				System.out.println("SOC cant go over maximum");
				check=false;
			}
		}
		return check; 
	}
	
		
		
	
	public double sumUpEntriesOf1DDoubleArray(double[] array){
		double sum=0;
		for (int i=0; i<array.length; i++){
			sum+=array[i];
		}
		return sum;
	}
	
	public double [][] getDrivingTimesCurrentAgent(double [][] parkingTimesCurrentAgent,LinkedList<ParkingIntervalInfo> parkingIntervals){
		double [][]drivingTimesCurrentAgent = new double[parkingIntervals.size()][2];
		
		for (int i=1; i<parkingIntervals.size();i++){
			drivingTimesCurrentAgent[i-1][0]= parkingTimesCurrentAgent[i-1][1];
			drivingTimesCurrentAgent[i-1][1]= parkingTimesCurrentAgent[i][0];
		}
		// seperate: first entry corrected for overnight parking
		drivingTimesCurrentAgent[parkingIntervals.size()-1][0]= parkingTimesCurrentAgent[parkingIntervals.size()-1][1];
		drivingTimesCurrentAgent[parkingIntervals.size()-1][1]= parkingTimesCurrentAgent[0][0];
		return drivingTimesCurrentAgent;
	}
	
	
	public void performGeneralFeasibilityCheckAgentBehavior(double [] drivingConsumptionCurrentAgent){
		//Joules per second * second = Watt*s= Joule
		double totalAgentConsumptionJoule=sumUpEntriesOf1DDoubleArray(drivingConsumptionCurrentAgent); // in Joules
		double totalParkingTimeAgentInSeconds=sumUpEntriesOf1DDoubleArray(parkingLengthCurrentAgent);
		double potentialTotalChargingAgent=Main.chargingSpeedPerSecond*totalParkingTimeAgentInSeconds;
		
		
		if (totalAgentConsumptionJoule>potentialTotalChargingAgent){
			System.out.println("General check: infeasible");
		}
		else if (totalAgentConsumptionJoule==potentialTotalChargingAgent){
			System.out.println("General check: tight");
		}
		else{
			System.out.println("General check: loose");
		}
	}
	
		
	
	
	
	/**
	 * adds up the Consumptions of all PHEV owners and divides results by Penetration
	 * @return
	 */
		public double getAveragePHEVConsumptionInWatt(){
			double sumOfAllConsumptionsInWatt=0;
			for (Id personId : Main.vehicles.getKeySet()){
				double totalTripLengthInSecondsCurrentAgent=getTotalTripLengthOfPerson(personId);
				
				Vehicle one= Main.vehicles.getValue(personId);
				PlugInHybridElectricVehicle two= new PlugInHybridElectricVehicle(new IdImpl(1));
				
				if(areVehiclesSameClass(one, two)){ 
					// add persons consumption to totalPHEVConsumption - is a PHEV vehicle!
					double sumOfTotalAgentConsumptionInJoule=sumUpLinkedListEntries(energyConsumptionOfLegs.get(personId));
					//Joule/second=Watt
					sumOfAllConsumptionsInWatt+=sumOfTotalAgentConsumptionInJoule/totalTripLengthInSecondsCurrentAgent;
						
				}
			};
			// AverageWatt = SUm of All WattConsumptions/number of people with PHEV
			return sumOfAllConsumptionsInWatt/(populationTotal*Main.penetrationPercent);
		}
		
		/**
		 * adds up Consumption of all agents and divides by number of agents
		 * @return
		 */
			public double getAveragePHEVConsumptionInJoules(){
				double sumOfAllConsumptionsInJoules=0;
				for (Id personId : Main.vehicles.getKeySet()){
					
					Vehicle one= Main.vehicles.getValue(personId);
					PlugInHybridElectricVehicle two= new PlugInHybridElectricVehicle(new IdImpl(1));
					
					if(areVehiclesSameClass(one, two)){ 
						// add persons consumption to totalPHEVConsumption - is a PHEV vehicle!
						sumOfAllConsumptionsInJoules=sumUpLinkedListEntries(energyConsumptionOfLegs.get(personId));
							
					}
				};
				// AverageWatt = SUm of All WattConsumptions/number of people with PHEV
				return sumOfAllConsumptionsInJoules/(populationTotal*Main.penetrationPercent);
			}
		
		
		public double getTotalTripLengthOfPerson(Id personId){
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimeIntervals.get(personId);
			double [] parkingLengthsCurrentAgent=getParkingLengthsCurrentAgent(parkingIntervals);
			return sumUpEntriesOf1DDoubleArray(parkingLengthsCurrentAgent);
		}
		
		
		public double sumUpLinkedListEntries(LinkedList<Double> l){
			double sum=0;
			for (int i=0;i<l.size();i++) {
				sum += l.get(i);
			}
			return sum;
		}
		
		/**
		 * 
		 * @param one one Vehicle object
		 * @param two second Vehicle object
		 * @return boolean indicating whether objects have same class
		 */
		public boolean areVehiclesSameClass(Vehicle one, Vehicle two){
			return one.getClass().equals(two.getClass());
		}
		
	
		
		
	}

