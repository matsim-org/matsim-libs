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
	//TODO add method to return vehicles in energyConsumptionPlugin
	//LinkedListValueHashMap<Id, Vehicle> vehicles=energyConsumptionPlugin.
	
	private double priceBase=0.13;
	private double pricePeak=0.2;
	private double peakLoad=Math.pow(10, 4); // adjust max peakLoad in Joule
	private double chargingSpeedPerSecond=11000; // Joule/second
	//private double maxChargePerSlot=chargingSpeedPerSecond*Main.secondsPer15Min;
	
	private double [][] parkingTimesCurrentAgent;
	private double [] parkingLengthCurrentAgent;
	private double [][] chargingTimesCurrentAgent;
	private double [][] drivingTimesCurrentAgent;
	
	private double [] realParkingLengthCurrentAgent;
	private double []  drivingConsumptionCurrentAgent;
	
	private DecentralizedChargerInfo myChargerInfo;
	
	private double populationTotal;
	private double noOfPHEVs;
	private double averagePHEVConsumption;
	final Controler controler;
	
	
	public double getPeakLoad(){
		return peakLoad;
	}
	
	public double getPricePeak(){
		return pricePeak;
	}
	
	public double getPriceBase(){
		return priceBase;
	}
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
	public void getElectricityFromGrid(double startChargingTime, double endChargingTime, Id agentId, Id linkId){
		System.out.println();
	}
	
	public double calcNumberOfPHEVs(Controler controler){
		populationTotal=controler.getPopulation().getPersons().size();
		return noOfPHEVs = populationTotal*Main.penetrationPercent;
	}
	
	
	public void performChargingAlgorithm() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException {
		
		noOfPHEVs = calcNumberOfPHEVs(controler);
		averagePHEVConsumption= getAveragePHEVConsumption();
		myChargerInfo = new DecentralizedChargerInfo(peakLoad, noOfPHEVs, averagePHEVConsumption, priceBase, pricePeak); 
		
		myChargerInfo.loadBaseLoadCurveFromTextFile();
		
		myChargerInfo.getFittedLoadCurve();
		myChargerInfo.findHighLowIntervals();
		myChargerInfo.findProbabilityDensityFunctions();
		myChargerInfo.findProbabilityRanges();
		myChargerInfo.writeSummary();
		
		/*Loop over all agents
		 * for each agent -make personal list of links and times
		 */
		
		for (Id personId: controler.getPopulation().getPersons().keySet()){
			
			// get parking Intervals
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimeIntervals.get(personId);
			
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
				parkingLengthCurrentAgent[0]=parkingTimesCurrentAgent[0][1]+Main.secondsPerDay-parkingTimesCurrentAgent[parkingTimesCurrentAgent.length-1][0];
			}
			
								
			// get driving legs			
			LinkedList<Double> legEnergyConsumptionList = energyConsumptionOfLegs.get(personId);
			int noOfTrips=legEnergyConsumptionList.size();
			double [] drivingConsumptionCurrentAgent = new double[legEnergyConsumptionList.size()];
						
			for (int i=0;i<legEnergyConsumptionList.size();i++) {
				drivingConsumptionCurrentAgent[i]=legEnergyConsumptionList.get(i);
			}
			
			//same number parking as driving
			//TODO validate order... first tour is after first parking
			
			drivingTimesCurrentAgent = new double[parkingIntervals.size()][2];
			for (int i=1; i<parkingIntervals.size();i++){
				drivingTimesCurrentAgent[i-1][0]= parkingTimesCurrentAgent[i-1][1];
				drivingTimesCurrentAgent[i-1][1]= parkingTimesCurrentAgent[i][0];
			}
			drivingTimesCurrentAgent[parkingIntervals.size()-1][0]= parkingTimesCurrentAgent[parkingIntervals.size()-1][1];
			drivingTimesCurrentAgent[parkingIntervals.size()-1][1]= parkingTimesCurrentAgent[0][0];
			
			//general check
			double totalConsumption=calculateTotalConsumption(); // in Joules
			//Joules per second * second = Joule
			double potentialTotalCharging=chargingSpeedPerSecond*calculateTotalParkingTime();
			
			if (totalConsumption>potentialTotalCharging){
				System.out.println("General check: infeasible");
			}
			else if (totalConsumption==potentialTotalCharging){
				System.out.println("General check: tight");
			}
			else{
				System.out.println("General check: loose");
			}
				
			
			//special check
			// new realParkingLengthArray - crosschecked with valleys
			realParkingLengthCurrentAgent=new double[parkingIntervals.size()];
			for (int i=0; i<realParkingLengthCurrentAgent.length; i++){
				realParkingLengthCurrentAgent[i]=myChargerInfo.getFeasibleChargingTimeInInterval(parkingTimesCurrentAgent[i][0], parkingTimesCurrentAgent[i][1]);
			}
			
			double realTotalCharging=chargingSpeedPerSecond*calculateRealParkingTime();
			
			int noOfSlotsToBook= (int) Math.ceil(totalConsumption/(Main.slotLength*chargingSpeedPerSecond));
			int feasibleNoOfSlotsToBook= (int) Math.ceil(calculateRealParkingTime()/Main.slotLength);
			
			int slotsInPeakTime=noOfSlotsToBook-feasibleNoOfSlotsToBook;
			if (slotsInPeakTime<=0){
				slotsInPeakTime=0;
			}
			
			if (totalConsumption>realTotalCharging){
				System.out.println("Special check: infeasible");
				
			}
			else if (totalConsumption==realTotalCharging){
				System.out.println("Special check: tight");
				
			}
			else{
				System.out.println("Special check: loose");
			}
			
			
			boolean feasibilityCheck=false;
			while (feasibilityCheck==false){
				chargingTimesCurrentAgent=myChargerInfo.bookSlots(parkingTimesCurrentAgent, noOfSlotsToBook, slotsInPeakTime);
				
				double [][] actualOrder=getActualOrder(chargingTimesCurrentAgent, drivingTimesCurrentAgent, drivingConsumptionCurrentAgent); 
					
				feasibilityCheck=checkChargingOrderForFeasibility(actualOrder);
			}
			
			//getElectricityFromGrid(double startChargingTime, double endChargingTime, Id agentId, Id linkId){
			
		}// end agent loop
	}//end performChargingAlgorithm
		
	
	
	public double [][] getActualOrder(double [][] chargingTimesCurrentAgent, double [][]drivingTimesCurrentAgent, double [] drivingConsumptionCurrentAgent){
		double [][] actualOrder = new double [chargingTimesCurrentAgent.length+drivingTimesCurrentAgent.length][3];
		
		for (int i=0; i<chargingTimesCurrentAgent.length; i++){
			actualOrder[i][0]=chargingTimesCurrentAgent[i][0];
			actualOrder[i][1]=chargingTimesCurrentAgent[i][1];
			actualOrder[i][2]=(chargingTimesCurrentAgent[i][1]-chargingTimesCurrentAgent[i][0])*chargingSpeedPerSecond;
		}
		for (int i=0; i<drivingTimesCurrentAgent.length; i++){
			actualOrder[i+chargingTimesCurrentAgent.length][0]=drivingTimesCurrentAgent[i][0];
			actualOrder[i+chargingTimesCurrentAgent.length][1]=drivingTimesCurrentAgent[i][1];
			actualOrder[i+chargingTimesCurrentAgent.length][2]=drivingConsumptionCurrentAgent[i];
		}
		
		// TODO find an awesome way to sort a multidimensional array effectively by first column
		
		return actualOrder;
		
	} 
	
	
	public boolean checkChargingOrderForFeasibility(double [][] actualOrder){
		boolean check=true;
		// TODO
		return check; 
	}
	
		
	public double calculateTotalParkingTime(){
		double sum=0;
		for (int i=0; i<parkingLengthCurrentAgent.length; i++){
			sum+=parkingLengthCurrentAgent[i];
		}
		return sum;
	}
	
	public double calculateTotalConsumption(){
		double sum=0;
		for (int i=0; i<drivingConsumptionCurrentAgent.length; i++){
			sum+=drivingConsumptionCurrentAgent[i];
		}
		return sum;
	}
	
	public double calculateRealParkingTime(){
		double sum=0;
		for (int i=0; i<realParkingLengthCurrentAgent.length; i++){
			sum+=realParkingLengthCurrentAgent[i];
		}
		return sum;
	}
	
	/**
	 * adds up the Consumptions of all PHEV owners and divides results by Penetration
	 * @return
	 */
		public double getAveragePHEVConsumption(){
			double sumOfAllConsumptions=0;
			for (Id personId : Main.vehicles.getKeySet()){
				Vehicle one= Main.vehicles.getValue(personId);
				PlugInHybridElectricVehicle two= new PlugInHybridElectricVehicle(new IdImpl(1));
				
				if(areVehiclesSameClass(one, two)){ 
					// add persons consumption to totalPHEVConsumption - is a PHEV vehicle!
					LinkedList<Double> legEnergyConsumptionList = energyConsumptionOfLegs.get(personId);
					sumOfAllConsumptions+=sumUpLinkedListEntries(legEnergyConsumptionList);
				}
			};
			return (sumOfAllConsumptions/calcNumberOfPHEVs(controler));
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

