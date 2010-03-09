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

/**
 * @author droeder
 *
 */
public class GershensonAdaptiveTrafficLightController extends
			AdaptiveSignalSystemControlerImpl implements EventHandler, SimulationBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(GershensonAdaptiveTrafficLightController.class);

	private int tGreenMin =  0; // time in seconds
	private int minCarsTime = 0; //
	private double capFactor = 0;

	private boolean outLinkJam;
	private double compGreenTime;
	private double approachingRed;
	private int approachingGreenLink;
	private int approachingGreenLane;
	private double carsOnRefLinkTime;
	private boolean compGroupsGreen;
	private SignalGroupState oldState;

	private CarsOnLinkLaneHandler handler;

	private Map<Id, Integer> vehOnLink = new HashMap<Id, Integer>();
	private Map<Id, Map<Id, Integer>> vehOnLinkLanes = new HashMap<Id, Map<Id, Integer>>();
	private Map<Id, Double> switchedToGreen = new HashMap<Id, Double>();

	private SortedMap<Id, List<Id>> compGroups;
	private SortedMap<Id, Id> corrGroups;

	private QNetwork net;


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
	public void init(Map<Id, Id> corrGroups,
			Map<Id, List<Id>> compGroups, QNetwork net, CarsOnLinkLaneHandler handler){
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
		this.net = net;
	}

	private void initIsGreen(double time, SignalGroupDefinition signalGroup){
		this.outLinkJam = false;
		this.compGreenTime = 0;
		this.approachingRed = 0;
		this.approachingGreenLink = 0;
		this.approachingGreenLane = 0;
		this.carsOnRefLinkTime = 0;
		this.compGroupsGreen = true;
		this.oldState = this.getSignalGroupStates().get(signalGroup);

		Map<Id, SignalGroupDefinition> groups = this.getSignalGroups();
		vehOnLink = handler.getVehOnLink();
		vehOnLinkLanes = handler.getVehOnLinkLanes();

		// check if competing groups are green
		for (Id i : compGroups.get(signalGroup.getId())){
			if(this.getSignalGroupStates().get(groups.get(i)).equals(SignalGroupState.GREEN)){
				compGroupsGreen = true;
				break;
			}else{
				compGroupsGreen = false;
			}
		}
		// calculate outlinkCapacity --- should be changed, problem is that one Signalgroup
		// coordinates the Trafficlight for many toLinks and you don't know Agents Destination
		for (Id i : signalGroup.getToLinkIds()){
			double outLinkCapacity = net.getLinks().get(i).getSpaceCap();
//			double actStorage = vehOnLink.get(i);
			double actStorage = handler.getVehInD(time, i);
			if((outLinkCapacity*capFactor)< actStorage){
				outLinkJam = true;
				break;
			}
		}

		//set number of cars, approaching a competing Link in a short distance, if it is green
		if (compGroupsGreen == true){
			for (Id i : compGroups.get(signalGroup.getId())){
//				approachingGreenLink += vehOnLink.get((groups.get(i).getLinkRefId())).intValue();
				approachingGreenLink += handler.getVehInD(time, groups.get(i).getLinkRefId());
				for (Entry<Id, Integer> e : vehOnLinkLanes.get(i).entrySet()){
					approachingGreenLane += e.getValue().intValue();
				}
				if(compGreenTime < (time - switchedToGreen.get(i))){
					compGreenTime = switchedToGreen.get(i);
				}
			}
		}

		// set number of cars on refLink of signalGroup
//		this.carsOnRefLinkTime = vehOnLink.get(signalGroup.getLinkRefId())*compGreenTime;
		this.carsOnRefLinkTime = handler.getVehInD(time, signalGroup.getLinkRefId())*compGreenTime;

		// 	product of the number of cars approaching a Red light and the time a light is red
		if (this.oldState.equals(SignalGroupState.RED)){
//			approachingRed = vehOnLink.get(signalGroup.getLinkRefId());
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
	}


	private void updateSignalGroupState(double time, SignalGroupDefinition signalGroup) {
		this.initIsGreen(time, signalGroup);
		//check if this group was switched in this timestep. if so, return oldstate
		if (switchedToGreen.get(signalGroup.getId()).equals(time)){
			if(oldState.equals(SignalGroupState.GREEN)){
				this.getSignalGroupStates().put(signalGroup, SignalGroupState.GREEN);
				return;
			} else{
			  this.getSignalGroupStates().put(signalGroup, SignalGroupState.RED);
			  return;
			}
		}

		// algorithm starts
		if ((outLinkJam == true) && oldState.equals(SignalGroupState.GREEN)){ //Rule 5 + 6
			this.switchLight(signalGroup, oldState, time);
			return;
		} else if(outLinkJam == false ){ // 12
			if (compGroupsGreen == false && oldState.equals(SignalGroupState.RED)){ // Rule 6
			  this.switchLight(signalGroup, oldState, time);
			  return;
			}
			if (approachingRed > 0 && approachingGreenLink == 0){ // Rule 4
				switchLight(signalGroup, oldState, time);
				return;
			}else if(!(approachingGreenLane > 0)){  //Rule 3
				if ((time - compGreenTime) > tGreenMin && carsOnRefLinkTime > minCarsTime){ // Rule 1 + 2
				  this.switchLight(signalGroup, oldState, time);
				  return;
				}
			}
		}
		// algorithm ends

		// if no condition fits for switching lights, return oldstate
		if(oldState.equals(SignalGroupState.GREEN)){
		  this.getSignalGroupStates().put(signalGroup, SignalGroupState.GREEN);
		  return;
		}
		else{
		  this.getSignalGroupStates().put(signalGroup, SignalGroupState.RED);
		  return;
		}
	}

	public void switchLight (SignalGroupDefinition group, SignalGroupState oldState, double time){
		if (oldState.equals(SignalGroupState.GREEN)){
			this.getSignalGroupStates().put(group, SignalGroupState.RED);
			if (!(corrGroups.get(group.getId()) == null)){
				this.getSignalGroupStates().put(this.getSignalGroups().
						get(corrGroups.get(group.getId())),SignalGroupState.RED);
				switchedToGreen.put(corrGroups.get(group.getId()), 0.0);
			}
			for (Id i : compGroups.get(group.getId())){
				switchedToGreen.put(i, time);
				this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.GREEN);
			}
			switchedToGreen.put(group.getId(), 0.0);

		} else { //if (oldState.equals(SignalGroupState.RED)){
			this.getSignalGroupStates().put(group, SignalGroupState.GREEN);
			if (!(corrGroups.get(group.getId()) == null)){
				this.getSignalGroupStates().put(this.getSignalGroups().
						get(corrGroups.get(group.getId())),SignalGroupState.GREEN);
				switchedToGreen.put(corrGroups.get(group.getId()), time);
			}
			for (Id i : compGroups.get(group.getId())){
				switchedToGreen.put(i, 0.0);
				this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.RED);
			}
			switchedToGreen.put(group.getId(), time);
		}
//		return false;

	}

	public void setParameters (int minCars, int tGreenMin, double capFactor){
		this.minCarsTime = minCars;
		this.tGreenMin = tGreenMin;
		this.capFactor = capFactor;
	}

	public void reset(int iteration) {
		iteration=0;
	}

}
