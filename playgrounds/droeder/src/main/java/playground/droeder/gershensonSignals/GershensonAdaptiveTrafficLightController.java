/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.gershensonSignals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.signalsystems.config.AdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControlerImpl;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.systems.SignalGroupDefinition;

import playground.droeder.ValueComparator;

/**
 * @author droeder
 *
 */
public class GershensonAdaptiveTrafficLightController extends
			AdaptiveSignalSystemControlerImpl implements EventHandler, SimulationBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(GershensonAdaptiveTrafficLightController.class);

	protected int tGreenMin =  0; // time in seconds
	protected int minCarsTime = 0; //
	protected double capFactor = 0;
	protected double maxGreenTime = 0;

	protected boolean outLinkJam;
	protected boolean compLinkJam;
	protected boolean maxGreenTimeActive = false;
	protected double compGreenTime;
	protected double approachingRed;
	protected double approachingGreenLink;
	protected double approachingGreenLane;
	protected double carsOnRefLinkTime;
	protected boolean compGroupsGreen;
	protected SignalGroupState oldState;

	protected CarsOnLinkLaneHandler handler;

	protected Map<Id, Double> switchedToGreen = new HashMap<Id, Double>();

	protected SortedMap<Id, List<Id>> compGroups;
	protected SortedMap<Id, Id> corrGroups;
	protected Map<Id, Id> mainOutLinks;

	protected QNetwork net;


	public GershensonAdaptiveTrafficLightController(AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
	}

	/**initializes the adaptive controller. set defaultParameters for minGreenTime u = 15, 
	 * min (CarsApproaching*waitingTime) n = 300 and capFactor = 0.9
	 *
	 * Parameters could be changed with setParameters
	 *
	 * @param groups
	 * @param corrGroups
	 * @param compGroups
	 * @param net
	 * @param handler
	 */
	public void init(Map<Id, Id> corrGroups,Map<Id, List<Id>> compGroups, 
			Map<Id, Id> mainOutLinks, QNetwork net, CarsOnLinkLaneHandler handler){
		for(SignalGroupDefinition sd : this.getSignalGroups().values()){
			this.getSignalGroupStates().put(sd, SignalGroupState.RED);
			switchedToGreen.put(sd.getId(), 0.0);
		}

		if(this.tGreenMin == 0){
			this.tGreenMin = 15;
		}
		if(this.minCarsTime == 0){
			this.minCarsTime = 300;
		}
		if(this.capFactor == 0){
			this.capFactor = 0.9;
		}

		this.handler  = handler;
		this.compGroups = (SortedMap<Id, List<Id>>) compGroups;
		this.corrGroups = (SortedMap<Id, Id>) corrGroups;
		this.mainOutLinks = mainOutLinks;
		this.net = net;
	}

	protected void initIsGreen(double time, SignalGroupDefinition signalGroup){
		this.outLinkJam = false;
		this.compGreenTime = 0;
		this.approachingRed = 0;
		this.approachingGreenLink = 0;
		this.approachingGreenLane = 0;
		this.carsOnRefLinkTime = 0;
		this.compGroupsGreen = true;
		this.oldState = this.getSignalGroupStates().get(signalGroup);

		Map<Id, SignalGroupDefinition> groups = this.getSignalGroups();

		// check if competing groups are green
		for (Id i : compGroups.get(signalGroup.getId())){
			if(this.getSignalGroupStates().get(groups.get(i)).equals(SignalGroupState.GREEN)){
				compGroupsGreen = true;
				break;
			}else{
				compGroupsGreen = false;
			}
		}
		
		// calculate outlinkCapacity for the mainOutlink and check if there is a trafficJam
		double outLinkCapacity = net.getLinks().get(mainOutLinks.get(signalGroup.getLinkRefId())).getSpaceCap();
		double actStorage = handler.getVehOnLink(mainOutLinks.get(signalGroup.getLinkRefId()));
		if((outLinkCapacity*capFactor)< actStorage){
			outLinkJam = true;
		}
		
		// check competing links for trafficJam
		compLinkJam = true;
		for (Id i : compGroups.get(signalGroup.getId())){
			double compCap = net.getLinks().get(groups.get(i).getLinkRefId()).getSpaceCap();
			double compStorage = handler.getVehOnLink(groups.get(i).getLinkRefId());
			if ((compCap*capFactor)>compStorage){
				compLinkJam = false;
				break;
			}
			
		}

		//set number of cars, approaching a competing Link in a short distance, if it is green
		if (compGroupsGreen == true){
			for (Id i : compGroups.get(signalGroup.getId())){
				approachingGreenLane = handler.getVehOnLinkLanes(this.getSignalGroups().get(i).getLinkRefId());
				approachingGreenLink += handler.getVehInD(time, groups.get(i).getLinkRefId());
				if((compGreenTime< (time-switchedToGreen.get(i)))){
					compGreenTime = (time - switchedToGreen.get(i));
				}
			}
		}

		// set number of cars on refLink of signalGroup
		this.carsOnRefLinkTime = handler.getVehInD(time, signalGroup.getLinkRefId())*(compGreenTime);

		// 	number of cars approaching a red light
		if (this.oldState.equals(SignalGroupState.RED)){
			this.approachingRed = handler.getVehInD(time, signalGroup.getLinkRefId());
		}else{
			this.approachingRed = 0;
		}
	}
	
	@Override
	public SignalGroupState getSignalGroupState(double seconds,
		SignalGroupDefinition signalGroup) {
		return this.getSignalGroupStates().get(signalGroup);
	}

	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
	  for (SignalGroupDefinition sg : this.getSignalGroups().values()){
			this.updateSignalGroupState(e.getSimulationTime(), sg);
	  }
	  
	  
//	  //experimental, switch links with higher demand first
//	  HashMap<Id, Double> map = new HashMap<Id, Double>();
//	  ValueComparator bvc =  new ValueComparator(map);
//	  TreeMap<Id, Double> sorted_map = new TreeMap<Id, Double>(bvc);
//	  for (SignalGroupDefinition sg : this.getSignalGroups().values()){
//			map.put(sg.getId(), handler.getVehInD(e.getSimulationTime(), sg.getLinkRefId()));
//	  }
//	  sorted_map.putAll(map);
//	  for (Entry<Id, Double> ee : sorted_map.entrySet()){
//		  this.updateSignalGroupState(e.getSimulationTime(), this.getSignalGroups().get(ee.getKey()));
//	  }
	}


	private void updateSignalGroupState(double time, SignalGroupDefinition signalGroup) {

		this.initIsGreen(time, signalGroup);
		
		//check if this group was switched in this timestep. if so, return oldstate
		if (this.switchedToGreen.get(signalGroup.getId()).equals(time)){
			return;
		}
		
		if (compGreenTime > maxGreenTime && compLinkJam == false && maxGreenTimeActive == true){
			this.switchRedGreen(signalGroup, time);
		}
		

		// algorithm starts
		if ((this.outLinkJam == true) && this.oldState.equals(SignalGroupState.GREEN)){ //Rule 5 + 6
			this.switchRedGreen(signalGroup, time);
			return;
		} else if(this.outLinkJam == false ){
			if (this.compGroupsGreen == false && this.oldState.equals(SignalGroupState.RED)){ // Rule 6
			  this.switchRedGreen(signalGroup, time);
			  return;
			}
			if (this.approachingRed > 0 && this.approachingGreenLink == 0){ // Rule 4
				switchRedGreen(signalGroup, time);
				return;
			}else if(!(this.approachingGreenLane > 0)){  //Rule 3
				if ((this.compGreenTime) > this.tGreenMin && this.carsOnRefLinkTime > this.minCarsTime){ // Rule 1 + 2
				  this.switchRedGreen(signalGroup, time);
				  return;
				}
			}
		}
		
		// algorithm ends

		// if no condition fits for switching lights, return oldstate
//		if(oldState.equals(SignalGroupState.GREEN)){
//			if (signalGroup.getId().equals(new IdImpl("12100")) || signalGroup.getId().equals(new IdImpl("14500")) ||
//					signalGroup.getId().equals(new IdImpl("200"))){
//				log.debug(signalGroup.getId());
//			}
//		  this.getSignalGroupStates().put(signalGroup, SignalGroupState.GREEN);
//		  return;
//		}
//		else{
//			if (signalGroup.getId().equals(new IdImpl("12100")) || signalGroup.getId().equals(new IdImpl("14500")) ||
//					signalGroup.getId().equals(new IdImpl("200"))){
//				log.debug(signalGroup.getId());
//			}
//		  this.getSignalGroupStates().put(signalGroup, SignalGroupState.RED);
//		  return;
//		}
	}

	private void switchRedGreen (SignalGroupDefinition group, double time){
		if (this.oldState.equals(SignalGroupState.GREEN)){
			if (!(this.corrGroups.get(group.getId()) == null)){
				this.getSignalGroupStates().put(this.getSignalGroups().
						get(this.corrGroups.get(group.getId())),SignalGroupState.RED);
				this.switchedToGreen.put(this.corrGroups.get(group.getId()), time);
			}
			for (Id i : this.compGroups.get(group.getId())){
				this.switchedToGreen.put(i, time);
				this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.GREEN);
			}
			this.getSignalGroupStates().put(group, SignalGroupState.RED);
			this.switchedToGreen.put(group.getId(), time);

		} else { //if (oldState.equals(SignalGroupState.RED)){
			if (!(this.corrGroups.get(group.getId()) == null)){
				this.getSignalGroupStates().put(this.getSignalGroups().
						get(this.corrGroups.get(group.getId())),SignalGroupState.GREEN);
				this.switchedToGreen.put(this.corrGroups.get(group.getId()), time);
			}
			for (Id i : this.compGroups.get(group.getId())){
				this.switchedToGreen.put(i, time);
				this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.RED);
			}
			this.getSignalGroupStates().put(group, SignalGroupState.GREEN);
			this.switchedToGreen.put(group.getId(), time);
		}
	}
	
	/*
	 * use this method to set parameters minimumGreenTime u, minimum of the product cars and waitingTime n, the capacityFactor for trafficJam on the outlink
	 * and the maximumGreenTime ( 0 == disable maximumGreenTime)
	 */
	

	public void setParameters (int minCarsTime, int tGreenMin, double capFactor, int maxGreenTime){
		this.minCarsTime = minCarsTime;
		this.tGreenMin = tGreenMin;
		this.capFactor = capFactor;
		this.maxGreenTime = maxGreenTime;
		
		if (maxGreenTime == 0){
			maxGreenTimeActive = false;
		}else{
			maxGreenTimeActive = true;
		}
	}

	public void reset(int iteration) {
		iteration=0;
	}

}


