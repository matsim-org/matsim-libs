/* *********************************************************************** *
 * project: org.matsim.*
 * HubLoadDistributionReader.java
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

package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;


import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.lib.obj.LinkedListValueHashMap;



public class HubLoadDistributionReader {
	
		
	private HubLinkMapping hubLinkMapping;
	
	LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution;
	LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution;
	LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution;
	LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;
	LinkedListValueHashMap<Integer, double [][]> originalDeterministicChargingDistribution;
	LinkedListValueHashMap<Integer, double [][]> loadAfterDeterministicChargingDecision;
	//double [time - seconds in day] = available free load in W on grid in Hub
	
	Controler controler;
	
	/**
	 * Reads in load data for all hubs and stores PolynomialFunctions 
	 * of load valleys and peak load times
	 * @throws IOException 
	 * @throws OptimizationException 
	 * @throws InterruptedException 
	 */
	public HubLoadDistributionReader(Controler controler, 
			HubLinkMapping hubLinkMapping,
			LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution,
			LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution,
			LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution) throws IOException, OptimizationException, InterruptedException{
		
		this.controler=controler;
		
		this.hubLinkMapping=hubLinkMapping;
		
		this.deterministicHubLoadDistribution=deterministicHubLoadDistribution; // continuous functions
		
		this.stochasticHubLoadDistribution=stochasticHubLoadDistribution; // continuous functions
		
		this.pricingHubDistribution=pricingHubDistribution; // continuous functions with same intervals as deterministic HubLoadDistribution!!!
		
		if (false==checkIfPricingAndDeterministicHaveSameTimeIntervals()){
			System.out.println("WRONG INPUT: Deterministic Load Distribution " +
					"does not have same time intervals as pricing Distribution");
			controler.wait();
		}
		
		initializeLoadAfterDeterministicChargingDecision();
				
	}
	
	
	public boolean checkIfPricingAndDeterministicHaveSameTimeIntervals(){
		boolean isSame=false;
		
		if(pricingHubDistribution.getKeySet().size()!= 
			deterministicHubLoadDistribution.getKeySet().size()){
			return isSame;
		}else{
			
			for(Integer i: pricingHubDistribution.getKeySet()){
				isSame=pricingHubDistribution.getValue(i).sameTimeIntervalsInThisSchedule(
						deterministicHubLoadDistribution.getValue(i));				
				
			}
		}
		return isSame;
		
	}
	
	
	
	
	
	public int getHubForLinkId(Id idLink){
		
		int hubNumber= (int) hubLinkMapping.getHubNumber(idLink.toString()); //returns Number
		
		return hubNumber;
	}
	
	
	public PolynomialFunction getDeterministicLoadPolynomialFunctionAtLinkAndTime(Id idLink, TimeInterval t){
		
		int hub= getHubForLinkId(idLink);
		
		Schedule hubLoadSchedule = deterministicHubLoadDistribution.getValue(hub);
		int interval = hubLoadSchedule.intervalIsInWhichTimeInterval(t);
				
		LoadDistributionInterval l1= (LoadDistributionInterval) hubLoadSchedule.timesInSchedule.get(interval);
		
		return l1.getPolynomialFunction();
		
	}
	

	public PolynomialFunction getPricingPolynomialFunctionAtLinkAndTime(Id idLink, TimeInterval t){
		
		int hub= getHubForLinkId(idLink);
		
		Schedule hubLoadSchedule = pricingHubDistribution.getValue(hub);
		int interval = hubLoadSchedule.intervalIsInWhichTimeInterval(t);
				
		LoadDistributionInterval l1= (LoadDistributionInterval) hubLoadSchedule.timesInSchedule.get(interval);
		
		return l1.getPolynomialFunction();
		
	}
	
	
	public Schedule getLoadDistributionScheduleForHubId(Id idLink){
		int hub= getHubForLinkId(idLink);
		return deterministicHubLoadDistribution.getValue(hub);
	}
	
	
	
	
	public PolynomialFunction fitCurve(double [][] data) throws OptimizationException{
		
		for (int i=0;i<data.length;i++){
			DecentralizedSmartCharger.polyFit.addObservedPoint(1.0, data[i][0], data[i][1]);
			
		  }		
		
		 PolynomialFunction poly = DecentralizedSmartCharger.polyFit.fit();
		
		return poly;
	}
	
	
	
	
	private void  initializeLoadAfterDeterministicChargingDecision(){
		loadAfterDeterministicChargingDecision= new LinkedListValueHashMap<Integer, double [][]>();
		originalDeterministicChargingDistribution= new LinkedListValueHashMap<Integer, double [][]>();
		
		for(Integer i : deterministicHubLoadDistribution.getKeySet()){
			Schedule s= deterministicHubLoadDistribution.getValue(i);
			
			double [][] loadBefore= new double [ (int)DecentralizedSmartCharger.MINUTESPERDAY ][2];
			double [][] loadAfter= new double [ (int)DecentralizedSmartCharger.MINUTESPERDAY ][2];
			for(int j=0; j<DecentralizedSmartCharger.MINUTESPERDAY; j++){
				
				double second= j*DecentralizedSmartCharger.SECONDSPERMIN;
				
				int interval= s.timeIsInWhichInterval(second);
				LoadDistributionInterval l= (LoadDistributionInterval) s.timesInSchedule.get(interval);
				PolynomialFunction func= l.getPolynomialFunction();
				
				loadBefore[j][0]=second; //time in second
				loadBefore[j][1]=func.value(second); // Watt at second
				
				loadAfter[j][0]=second; //time in second
				loadAfter[j][1]=func.value(second); // Watt at second
			}
			loadAfterDeterministicChargingDecision.put(i, loadAfter);
			originalDeterministicChargingDistribution.put(i, loadBefore);
		}
		
		
		
	}
	
	
	public void updateLoadAfterDeterministicChargingDecision(Id linkId, int minInDay, double wattReduction){
		
		
		int hubId= getHubForLinkId(linkId);
		
		/*double [][]check1= loadAfterDeterministicChargingDecision.getValue(hubId);
		double [][]check2= originalDeterministicChargingDistribution.getValue(hubId);
		System.out.println("BEFORE new: "+ check1[minInDay][0]+", "+check1[minInDay][1]);
		System.out.println("BEFORE old: "+ check2[minInDay][0]+", "+check2[minInDay][1]);*/
		
		double [][]loadAfter=loadAfterDeterministicChargingDecision.getValue(hubId);
		
		loadAfter[minInDay][0]=minInDay*DecentralizedSmartCharger.SECONDSPERMIN;
		loadAfter[minInDay][1]=loadAfter[minInDay][1]-wattReduction;
		                    
		loadAfterDeterministicChargingDecision.put(hubId, loadAfter);
		
		/*check1= loadAfterDeterministicChargingDecision.getValue(hubId);
		check2= originalDeterministicChargingDistribution.getValue(hubId);
		System.out.println("AFTER new: "+ check1[minInDay][0]+", "+check1[minInDay][1]);
		System.out.println("AFTER old: "+ check2[minInDay][0]+", "+check2[minInDay][1]);
		System.out.println();*/
		
	}
	
	
	
}
