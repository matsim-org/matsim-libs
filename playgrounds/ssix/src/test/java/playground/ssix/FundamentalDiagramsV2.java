/* *********************************************************************** *
 * project: org.matsim.*
 * LangeStreckeSzenario													   *
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

package playground.ssix;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;

import playgrounds.ssix.FundamentalDiagrams;

public class FundamentalDiagramsV2 implements LinkEnterEventHandler{
	
	private static final Logger log = Logger.getLogger(FundamentalDiagrams.class);
	
	//private Scenario scenario;
	
	private Id studiedMeasuringPointLinkId = new IdImpl(0);
	private boolean permanentRegime;
	private int permanentRegimeTour;
	private double permanentDensity;
	private double permanentFlow;
	private double permanentAverageVelocity;
	private Map<Id,Integer> personTour;
	private Map<Id,Double> lastSeenOnStudiedLinkEnter;
	private Map<Integer,Tuple<Integer,Double>> tourNumberSpeed;
	
	private Integer[] flowTable;//Flows on node 0 for the last 36s (counted every second)
	private double flowTime;
	
	
	public FundamentalDiagramsV2(Scenario sc){
		//this.scenario = sc;
		this.permanentRegime = false;
		this.personTour = new TreeMap<Id,Integer>();
		this.lastSeenOnStudiedLinkEnter = new TreeMap<Id,Double>();
		this.tourNumberSpeed = new TreeMap<Integer,Tuple<Integer,Double>>();
		this.permanentDensity = 0.;
		this.permanentFlow = 0.;
		this.permanentAverageVelocity = 0.;
		this.flowTable = new Integer[36];
		for (int i=0; i<36; i++){
			this.flowTable[i] = 0;
		}
		this.flowTime=0.;
	}
	
	public void reset(int iteration){
		this.permanentRegime=false;
		this.personTour.clear();
		this.tourNumberSpeed.clear();
		this.lastSeenOnStudiedLinkEnter.clear();
		this.flowTime = 0.;
	}
	
	public void handleEvent(LinkEnterEvent event){
		Id personId = event.getPersonId();
		int tourNumber;
		double nowTime = event.getTime();
		double networkLength = DreieckStreckeSzenario.length * 3;
		
		if (event.getLinkId().equals(studiedMeasuringPointLinkId)){
			if (this.personTour.containsKey(personId)){
				tourNumber = personTour.get(personId);
				//Saving the speed
				double lastSeenTime = lastSeenOnStudiedLinkEnter.get(personId);
				double speed = networkLength / (nowTime-lastSeenTime);//in m/s!!
				Tuple<Integer,Double> NumberSpeed = this.tourNumberSpeed.get(tourNumber);
				int n = NumberSpeed.getFirst();
				double sn = NumberSpeed.getSecond();//=average speed with n people
				//encountered a few calculatory problems here for still mysterious reasons, hence the very unnecessary details
				double first = n*sn/(n+1);
				double second = speed/(n+1);
				//System.out.println("n "+n+"  sn "+sn+"  speed "+speed+"  first "+first+"  second "+second);
				Tuple<Integer,Double> newNumberSpeed = new Tuple<Integer,Double>(n+1,first + second/*average speed with n+1 people*/);
				this.tourNumberSpeed.put(tourNumber, newNumberSpeed);
				
				//Checking for permanentRegime
				if (tourNumber>2){
					double previousLapSpeed = this.tourNumberSpeed.get(tourNumber-1).getSecond();
					double theOneBefore = this.tourNumberSpeed.get(tourNumber-2).getSecond();
					if ((almostEqualDoubles(speed, previousLapSpeed, 0.05)) && (almostEqualDoubles(previousLapSpeed, theOneBefore, 0.05))){
						if (!(permanentRegime)){
							this.permanentRegimeTour=tourNumber;
							this.permanentRegime=true;
						}
					//System.out.println(permanentRegime);
					}
				}
				
				//Updating other data sources
				tourNumber++;
				this.personTour.put(personId,tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId,nowTime);
				
				//Initializing new Map for next Tour
				if (!(this.tourNumberSpeed.containsKey(tourNumber)))
					this.tourNumberSpeed.put(tourNumber,new Tuple<Integer,Double>(0,0.));
			} else {
				//First tour handling
				tourNumber = 1;
				this.personTour.put(personId, tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId, nowTime);
				if (!(this.tourNumberSpeed.containsKey(tourNumber))){
					this.tourNumberSpeed.put(tourNumber,new Tuple<Integer,Double>(0,0.));
					this.flowTime = nowTime;
				}
			}
			
			//Updating Flow. NB: Flow is also measured on studiedMeasuringPointLinkId
			if (nowTime == this.flowTime){//Still measuring the flow of the same second
				int nowFlow = this.flowTable[0];
				nowFlow++;
				this.flowTable[0] = nowFlow;
			} else {//Need to offset the current data in order to update
				double timeDifference = nowTime-this.flowTime;
				if (timeDifference<36){
					for (int i=35-(int)timeDifference; i==0; i--){
						this.flowTable[i+(int)timeDifference] = this.flowTable[i];
					}
					if (timeDifference>1){
						for (int i = 1; i<(int)timeDifference; i ++){
							this.flowTable[i] = 0;
						}
					}
				} else {
					flowTableReset();
				}
				this.flowTable[0] = 1;
				this.flowTime = nowTime;
			}
			
		
			if (permanentRegime){
				tourNumber = this.personTour.get(personId);
				
				this.permanentFlow = getActualFlow();
				
				if (tourNumber == (this.permanentRegimeTour+2)){//Let the simulation go another turn around to eventually fill data gaps
					
					int numberOfDrivingAgents = this.tourNumberSpeed.get(this.permanentRegimeTour).getFirst();
					
					this.permanentDensity = numberOfDrivingAgents/networkLength;
					this.permanentAverageVelocity = this.tourNumberSpeed.get(this.permanentRegimeTour).getSecond();
					
					//Setting all agents to abort:
					//TODO
					
					log.info("Simulation successful. Density: "+this.permanentDensity+"  Flow: "+this.permanentFlow+"  Average Speed: "+this.permanentAverageVelocity);
				}
			}
		
		}
		
	}
	
	private double getActualFlow(){
		double flowOverLast36s = 0;
		for (int i=0; i<36; i++){
			flowOverLast36s += this.flowTable[i];
		}
		return flowOverLast36s*100;//extrapolated hour flow in veh/h
	}
	
	private void flowTableReset(){
		for (int i=0; i<36; i++){
			this.flowTable[i] = 0;
		}
	}
	
	private boolean almostEqualDoubles(double d1, double d2, double relativeMaximumAcceptedDeviance){
		if (((d1-d2)/d2)<relativeMaximumAcceptedDeviance)
			return true;
		return false;
	}
	
}
