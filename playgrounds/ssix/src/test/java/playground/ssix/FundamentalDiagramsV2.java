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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;

import playgrounds.ssix.FundamentalDiagrams;

public class FundamentalDiagramsV2 implements LinkEnterEventHandler{
	
	private static final Logger log = Logger.getLogger(FundamentalDiagrams.class);
	
	private Scenario scenario;
	
	private Id studiedMeasuringPointLinkId = new IdImpl(0);
	private boolean permanentRegime;
	private double permanentDensity;
	private double permanentFlow;
	private Map<Id,Integer> personTour;
	private Map<Id,Double> lastSeenOnStudiedLinkEnter;
	private Map<Integer,Map<Id,Double>> tourPersonSpeed;
	
	
	public FundamentalDiagramsV2(Scenario sc){
		this.scenario = sc;
		this.permanentRegime = false;
		this.personTour = new TreeMap<Id,Integer>();
		this.lastSeenOnStudiedLinkEnter = new TreeMap<Id,Double>();
		this.tourPersonSpeed = new TreeMap<Integer,Map<Id,Double>>();
		this.permanentDensity = 0.;
		this.permanentFlow =0.;
	}
	
	public void reset(int iteration){
		this.permanentRegime=false;
		this.personTour.clear();
		this.tourPersonSpeed.clear();
		this.lastSeenOnStudiedLinkEnter.clear();
	}
	
	public void handleEvent(LinkEnterEvent event){
		if (event.getLinkId().equals(studiedMeasuringPointLinkId)){
			Id personId = event.getPersonId();
			int tourNumber = 1;
			double nowTime = event.getTime();
			double networkLength = DreieckStreckeSzenario.length * 3;
			if (this.personTour.containsKey(personId)){
				//before updating tourNumber, màj tourPersonSpeed
				double lastSeenTime = lastSeenOnStudiedLinkEnter.get(personId);
				double speed = networkLength / (nowTime-lastSeenTime);//in m/s!!
				if (this.tourPersonSpeed.equals(null))
					System.out.println("got a null instead of the wnted intialized map");
				System.out.println(this.tourPersonSpeed);/*put(personId, speed);*///TODO:fix this!only returns an empty map
				
				tourNumber = this.personTour.get(personId) + 1;
				this.personTour.put(personId,tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId,nowTime);
			} else {
				this.personTour.put(personId, tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId, nowTime);
				Map<Id, Double> initiatingNewPersonSpeedMap = this.tourPersonSpeed.get(tourNumber);//TODO:fix this! this way it initiates too many times, and btw it doesnt work.
				initiatingNewPersonSpeedMap = new HashMap<Id,Double>();
			}
		}
	}
}
