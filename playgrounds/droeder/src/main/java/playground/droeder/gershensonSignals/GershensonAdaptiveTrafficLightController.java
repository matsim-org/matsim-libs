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
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.core.utils.collections.Tuple;
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
			AdaptiveSignalSystemControlerImpl implements LaneEnterEventHandler, LaneLeaveEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
			AgentDepartureEventHandler, AgentArrivalEventHandler{

	private static final Logger log = Logger.getLogger(GershensonAdaptiveTrafficLightController.class);

	private int tGreenMin = 0; // time in seconds
	private int minCars = 0; //
	private double capFactor = 0.99;
	private boolean compGroupsGreen = false;

	private Map<Id, Integer> vehOnLink = new HashMap<Id, Integer>();
	private Map<Id, Map<Id, Integer>> vehOnLinkLanes = new HashMap<Id, Map<Id, Integer>>();
	private Map<Id, Double> switchedToGreen = new HashMap<Id, Double>();

	private SortedMap<Id, List<Id>> compGroups;
	private SortedMap<Id, Id> corrGroups;
	private SortedMap<Id, SignalGroupState> oldState;
	
	private QNetwork net;

	/**
	 * dg hack this field should disappear in the near future
	 */
	private Tuple<SignalGroupDefinition, Double> lastCallOfIsGreen;


	public GershensonAdaptiveTrafficLightController(AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
	}

	public void init(Map<Id, SignalGroupDefinition> groups, QNetwork qNetwork){
		for(SignalGroupDefinition sd : this.getSignalGroups().values()){
			this.getSignalGroupStates().put(sd, SignalGroupState.RED);
			switchedToGreen.put(sd.getId(), 0.0);
		}
		
		for (SignalGroupDefinition sd : groups.values()){
			vehOnLink.put(sd.getLinkRefId(), 0);
			for (Id id : sd.getToLinkIds()){
				if (!vehOnLink.containsKey(id)){
					vehOnLink.put(id, 0);
				}
			}
			Map<Id, Integer> m = new HashMap<Id, Integer>();
			for (Id id : sd.getLaneIds()){
				m.put(id, 0);
			}
			vehOnLinkLanes.put(sd.getId(), m);
		}
		setNetwork(qNetwork);
	}
		
	@Override
	public void reset(int iteration) {
		iteration = 0;
	}
	
	@Override
	public void handleEvent(LaneEnterEvent e) {
		Map<Id, Integer> m = vehOnLinkLanes.get(e.getLinkId());
		if (m != null && m.containsKey(e.getLaneId())){
			int i = m.get(e.getLaneId()).intValue();
			i =  i + 1;
			m.put(e.getLaneId(), Integer.valueOf(i));
			vehOnLinkLanes.put(e.getLinkId(), m);
		}
	}

	@Override
	public void handleEvent(LaneLeaveEvent e) {
		Map<Id, Integer> m = vehOnLinkLanes.get(e.getLinkId());
		if (m != null && m.containsKey(e.getLaneId())){
			int i = m.get(e.getLaneId()).intValue();
			i =  i - 1;
			m.put(e.getLaneId(), Integer.valueOf(i));
			vehOnLinkLanes.put(e.getLinkId(), m);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent e) {
		if (vehOnLink.containsKey(e.getLinkId())){
			int i = vehOnLink.get(e.getLinkId()).intValue();
			i = i + 1;
			vehOnLink.put(e.getLinkId(), Integer.valueOf(i));
		}
	}
	@Override
	public void handleEvent(LinkLeaveEvent e) {
		if (vehOnLink.containsKey(e.getLinkId())){
			int i = vehOnLink.get(e.getLinkId()).intValue();
			i = i - 1;
			vehOnLink.put(e.getLinkId(), Integer.valueOf(i));
		}
	}
	@Override
	public void handleEvent(AgentDepartureEvent e) {
		if (vehOnLink.containsKey(e.getLinkId())){
			int i = vehOnLink.get(e.getLinkId()).intValue();
			i = i + 1;
			vehOnLink.put(e.getLinkId(), Integer.valueOf(i));
		}
	}
	@Override
	public void handleEvent(AgentArrivalEvent e){
		if (vehOnLink.containsKey(e.getLinkId())){
			int i = vehOnLink.get(e.getLinkId()).intValue();
			i = i - 1;
			vehOnLink.put(e.getLinkId(), Integer.valueOf(i));
		}
	}
	

	@Override
	public boolean givenSignalGroupIsGreen(double time, SignalGroupDefinition signalGroup) {
		//start dg hack
		if (this.lastCallOfIsGreen != null && (this.lastCallOfIsGreen.getFirst().equals(signalGroup) && this.lastCallOfIsGreen.getSecond().doubleValue() == time)){
			if (this.getSignalGroupStates().get(signalGroup).equals(SignalGroupState.GREEN)){
				return true;
			}
			else if (this.getSignalGroupStates().get(signalGroup).equals(SignalGroupState.RED)){
				return false;
			}
		}
		else {
			this.lastCallOfIsGreen = new Tuple<SignalGroupDefinition, Double>(signalGroup, time);
		}
		//end dg hack


		boolean outLinkJam = false;
		double compGreenTime = 0;
		double approachingRed = 0; 
		int approachingGreenLink = 0;
		int approachingGreenLane = 0;
		double cars = 0;
		
		compGroupsGreen = true;
		

		Map<Id, SignalGroupDefinition> groups = this.getSignalGroups();
		SignalGroupState oldState = this.getSignalGroupStates().get(signalGroup);

	// --------- initialize ---------------
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
			double actStorage = vehOnLink.get(i);
			if((outLinkCapacity*capFactor)< actStorage){
				outLinkJam = true;
				break;
			}
		}
		
		//set number of cars, approaching a competing Link in a short distance, if it is green
		if (compGroupsGreen == true){
			for (Id i : compGroups.get(signalGroup.getId())){
				approachingGreenLink += vehOnLink.get((groups.get(i).getLinkRefId())).intValue();
				for (Entry<Id, Integer> e : vehOnLinkLanes.get(i).entrySet()){
					approachingGreenLane += e.getValue().intValue();
				}
				if(compGreenTime < (time - switchedToGreen.get(i))){
						compGreenTime = switchedToGreen.get(i);
				}
			}
		}
		
		// set number of cars on refLink of signalGroup
		cars = vehOnLink.get(signalGroup.getLinkRefId());

		// 	set number of cars approaching a Red light
		if (oldState.equals(SignalGroupState.RED)){
			approachingRed = vehOnLink.get(groups.get(signalGroup.getId()).getLinkRefId());
		}else{
			approachingRed = 0;
		}
		
		//------- end of initializing

		//check if this group was switched in this timestep. if so, return oldstate
		if (switchedToGreen.get(signalGroup.getId()).equals(time)){
			if(oldState.equals(SignalGroupState.GREEN)){
				return true;
			}else{
				return false;
			}
		}
		

		// start algorithm
		if ((outLinkJam == true) && oldState.equals(SignalGroupState.GREEN)){ //Rule 5 + 6
			return switchLight(signalGroup, oldState, time);
		}else if(outLinkJam == false ){ // 12
			if (compGroupsGreen == false && oldState.equals(SignalGroupState.RED)){ // Rule 6
				return switchLight(signalGroup, oldState, time);
			}
			if (approachingRed > 0 && approachingGreenLink == 0){ // Rule 4
				return switchLight(signalGroup, oldState, time);
			}else if(!(approachingGreenLane > 0)){  //Rule 3
				if ((time - compGreenTime) > tGreenMin && cars > minCars){ // Rule 1 + 2
						return switchLight(signalGroup, oldState, time);
				}
			}

		}
		 // if no condition fits for switching lights, return oldstate
		if(oldState.equals(SignalGroupState.GREEN)){
			return true;
		}else{
			return false;
		}
	}


	public void setCompGroups(Map<Id, List<Id>> compGroups){
		this.compGroups = (SortedMap<Id, List<Id>>) compGroups;
	}
	
	public void setCorrGroups(Map<Id, Id> corrGroups){
		this.corrGroups = (SortedMap<Id, Id>) corrGroups;
	}
	
	public void setNetwork (QNetwork qNetwork){
		this.net = qNetwork;
	}

	public boolean switchLight (SignalGroupDefinition group, SignalGroupState oldState, double time){
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
			return false;
		}else { //if (oldState.equals(SignalGroupState.RED)){
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
			return true;
		}
//		return false;

	}
	
	public void setMinCar (int minCars){
		this.minCars = minCars;
	}
	
	public void setGreenMin (int tGreenMin){
		this.tGreenMin = tGreenMin;
	}

}
