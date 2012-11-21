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

package main.java.playgrounds.ssix;

import java.util.LinkedList;
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

import main.java.playgrounds.ssix.FundamentalDiagrams;

/* A class supposed to go attached to the DreieckStreckeSzenario class.
 * It aims at analyzing the flow of events in order to detect:
 * The permanent regime of the system and the following searched values:
 * the permanent flow, the permanent density and the permanent average 
 * velocity.
 * Is only usable to get aggregated values. See FunDiagramsWithPassing for
 * disaggregated analysis.
 * 
 * */

public class FunDiagramsDreieck implements LinkEnterEventHandler{
	
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
	
	private List<Integer> flowTable;//Flows on node 0 for the last 3600s (counted every second)
	private Double flowTime;
	
	
	public FunDiagramsDreieck(Scenario sc){
		//this.scenario = sc;
		this.permanentRegime = false;
		this.personTour = new TreeMap<Id,Integer>();
		this.lastSeenOnStudiedLinkEnter = new TreeMap<Id,Double>();
		this.tourNumberSpeed = new TreeMap<Integer,Tuple<Integer,Double>>();
		this.permanentDensity = 0.;
		this.permanentFlow = 0.;
		this.permanentAverageVelocity = 0.;
		this.flowTable = new LinkedList<Integer>();
		for (int i=0; i<3600; i++){
			this.flowTable.add(0);
		}
		this.flowTime = new Double(0.);
	}
	
	public void reset(int iteration){
		this.permanentRegime=false;
		this.personTour.clear();
		this.tourNumberSpeed.clear();
		this.lastSeenOnStudiedLinkEnter.clear();
		this.flowTime = new Double(0.);
		this.flowTable.clear();
	}
	
	public void handleEvent(LinkEnterEvent event){
		Id personId = event.getPersonId();
		int tourNumber;
		double nowTime = event.getTime();
		double networkLength = DreieckStreckeSzenario.length * 3;
		
		if (event.getLinkId().equals(studiedMeasuringPointLinkId)){
			if (this.personTour.containsKey(personId)){
				tourNumber = personTour.get(personId);
				//Saving the speed by updating the previous average speed
				double lastSeenTime = lastSeenOnStudiedLinkEnter.get(personId);
				double speed = networkLength / (nowTime-lastSeenTime);//in m/s!!
				Tuple<Integer,Double> NumberSpeed = this.tourNumberSpeed.get(tourNumber);
				int n = NumberSpeed.getFirst();
				double sn = NumberSpeed.getSecond();//average speed for n people
				//encountered a few calculatory problems here for still mysterious reasons, 
				//hence the normally very unnecessary detailed programming
				double first = n*sn/(n+1);
				double second = speed/(n+1);
				Tuple<Integer,Double> newNumberSpeed = new Tuple<Integer,Double>(n+1,first + second/*average speed with n+1 people*/);
				this.tourNumberSpeed.put(tourNumber, newNumberSpeed);
				//Checking for permanentRegime
				if (tourNumber>2){
					double previousLapSpeed = this.tourNumberSpeed.get(tourNumber-1).getSecond();
					double theOneBefore = this.tourNumberSpeed.get(tourNumber-2).getSecond();
					if ((almostEqualDoubles(speed, previousLapSpeed, 0.02)) && (almostEqualDoubles(previousLapSpeed, theOneBefore, 0.02))){
						if (!(permanentRegime)){
							this.permanentRegimeTour=tourNumber;
							this.permanentRegime=true;
						}
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
					this.flowTime = new Double(nowTime);
				}
			}
			
			//Updating Flow. NB: Flow is also measured on studiedMeasuringPointLinkId
			if (nowTime == this.flowTime.doubleValue()){//Still measuring the flow of the same second
				Integer nowFlow = this.flowTable.get(0);
				this.flowTable.set(0, nowFlow.intValue()+1);
			} else {//Need to offset the current data in order to update
				int timeDifference = (int) (nowTime-this.flowTime.doubleValue());
				//log.info("timeDifference is: "+timeDifference);
				if (timeDifference<3600){
					for (int i=3599-timeDifference; i>=0; i--){
						this.flowTable.set(i+timeDifference, this.flowTable.get(i).intValue());
					}
					if (timeDifference > 1){
						for (int i = 1; i<timeDifference; i++){
							this.flowTable.set(i, 0);
						}
					}
					this.flowTable.set(0, 1);
				} else {
					flowTableReset();
				}
				this.flowTime = new Double(nowTime);
			}
			
		
			if (permanentRegime){
				tourNumber = this.personTour.get(personId);
				
				this.permanentFlow = getActualFlow();//veh/h
				
				if (tourNumber >= (this.permanentRegimeTour+2)){//Let the simulation go another turn around to eventually fill data gaps
					
					int numberOfDrivingAgents = this.tourNumberSpeed.get(this.permanentRegimeTour).getFirst();
					
					this.permanentDensity = numberOfDrivingAgents/networkLength*1000;//veh/km
					this.permanentAverageVelocity = this.tourNumberSpeed.get(this.permanentRegimeTour).getSecond();//m/s
					
					//Setting all agents to abort:
					//TODO
					
					log.info("Simulation successful. Density: "+this.permanentDensity+"  Flow: "+this.permanentFlow+"  Average Speed: "+this.permanentAverageVelocity);
				}
			}
		
		}
		
	}
	
	private double getActualFlow(){
		double flowOverLast3600s = 0;
		for (int i=0; i<3600; i++){
			flowOverLast3600s += this.flowTable.get(i).intValue();
		}
		return flowOverLast3600s;//extrapolated hour flow in veh/h
	}
	
	private void flowTableReset(){
		for (int i=0; i<3600; i++){
			this.flowTable.set(i, 0);
		}
	}
	
	private boolean almostEqualDoubles(double d1, double d2, double relativeMaximumAcceptedDeviance){
		if (((d1-d2)/d2)<relativeMaximumAcceptedDeviance)
			return true;
		return false;
	}
	
}
