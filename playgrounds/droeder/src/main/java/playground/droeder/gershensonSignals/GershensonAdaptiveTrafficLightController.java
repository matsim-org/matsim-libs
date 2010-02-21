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

	private final double tGreenMin = 20; // time in seconds
	private final double minCars = 8; //

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


		boolean compGroupsGreen = false;
		boolean outLinkJam = false;
		double compGreenTime = 0;
		double approachingRed = 0; 
		int approachingGreenLink = 0;
		int approachingGreenLane = 0;
		double cars = 0;
		

		Map<Id, SignalGroupDefinition> groups = this.getSignalGroups();
		SignalGroupState oldState = this.getSignalGroupStates().get(signalGroup);

		// --------- initialize
		for (Id i : compGroups.get(signalGroup.getId())){
			if(this.getSignalGroupStates().get(groups.get(i)).equals(SignalGroupState.GREEN)){
				compGroupsGreen = true;
				break;
			}else{
				compGroupsGreen = false;
			}
		}
		
		for (Id i : signalGroup.getToLinkIds()){
			double outLinkDensity = net.getLinks().get(i).getSpaceCap();
			double actDensity = (vehOnLink.get(i)/outLinkDensity);
			log.error(outLinkDensity + " " + actDensity);
			if((outLinkDensity * 0.95)< actDensity){
				outLinkJam = true;
				break;
			}
		}

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
		
		cars = vehOnLink.get(signalGroup.getLinkRefId());

		if (oldState.equals(SignalGroupState.RED)){
			approachingRed = vehOnLink.get(groups.get(signalGroup.getId()).getLinkRefId()) * compGreenTime;
		}else{
			approachingRed = 0;
		}
		
		
		
		// calculate outLinkSpeed still missing

		//------- end of initializing

		//check if corresponding group is switched to green in this timestep. if so and no
		//compGroup shows green, switch to green --- doesn't work
//		if (!(switchedToGreen.get(corrGroups.get(signalGroup.getId())).equals(null)) &&
//				switchedToGreen.get(corrGroups.get(signalGroup.getId())).equals(time) &&
//				compGroupsGreen == false && oldState.equals(SignalGroupState.RED)){
//			return switchLight(signalGroup, oldState, time);
//		}
//		for (Id i : compGroups.get(signalGroup.getId())){
//			if (switchedToGreen.get(i).equals(time)){
//				if (oldState.equals(SignalGroupState.GREEN)){
//					return switchLight(signalGroup, oldState, compGreenTime);
//				}else{
//					return false;
//				}
//			}
//		}
		

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
		if(oldState.equals(SignalGroupState.GREEN)){ // if no condition fits for switching lights, return oldstate
			if ( signalGroup.getId().equals(new IdImpl("200")) || signalGroup.getId().equals(new IdImpl("14500")) || signalGroup.getId().equals(new IdImpl("12100"))){
				log.error(signalGroup.getId()+ " " + oldState);
			}
			return true;
		}else{
			if ( signalGroup.getId().equals(new IdImpl("200")) || signalGroup.getId().equals(new IdImpl("14500")) || signalGroup.getId().equals(new IdImpl("12100"))){
				log.error(signalGroup.getId()+ " " + oldState);
			}
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
			if ( group.getId().equals(new IdImpl("200")) || group.getId().equals(new IdImpl("14500")) || group.getId().equals(new IdImpl("12100"))){
				log.fatal(group.getId() + " was " + oldState + " for " + switchedToGreen.get(group.getId()) + " switched to Red");
			}
			switchedToGreen.put(group.getId(), 0.0);
			return false;
		}else if (oldState.equals(SignalGroupState.RED)){
			this.getSignalGroupStates().put(group, SignalGroupState.GREEN);
			if ( group.getId().equals(new IdImpl("200")) || group.getId().equals(new IdImpl("14500")) || group.getId().equals(new IdImpl("12100"))){
				log.error(group.getId() + " was " + oldState + " for " + switchedToGreen.get(group.getId()) + " switched to Green");
			}
			switchedToGreen.put(group.getId(), time);
			return true;
		}
		return false;

	}

}
