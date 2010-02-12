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

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.core.utils.collections.Tuple;
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
			AgentDepartureEventHandler{

	private static final Logger log = Logger.getLogger(GershensonAdaptiveTrafficLightController.class);

	private final double vMin = 8; // minimum Speed in km/h
	private final double tMin = 15; // time in seconds
	private final double minVehicles = 10; //

	private Map<Id, Double> vehOnLink = new TreeMap<Id, Double>();
	private Map<Id, Double> vehOnLanes = new TreeMap<Id, Double>();
	private Map<Id, Double> agentLinkEnterTime = new TreeMap<Id, Double>();
	private Map<Id, Double> averageLinkTravelTime = new TreeMap<Id, Double>();
	private Map<Id, Double> switchedToGreen = new TreeMap<Id, Double>();

	private SortedMap<Id, Id> corrGroups;
	private SortedMap<Id, List<Id>> compGroups;
	private SortedMap<Id, SignalGroupState> oldState;

	/**
	 * dg hack this field should disappear in the near future
	 */
	private Tuple<SignalGroupDefinition, Double> lastCallOfIsGreen;


	public GershensonAdaptiveTrafficLightController(AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
	}

	public void init(Network net, Population pop){
		for(SignalGroupDefinition sd : this.getSignalGroups().values()){
			this.getSignalGroupStates().put(sd, SignalGroupState.RED);
			switchedToGreen.put(sd.getId(), 0.0);
		}
		for(Link l : net.getLinks().values()){
			vehOnLanes.put(l.getId(), 0.0);
			vehOnLink.put(l.getId(), 0.0);
			averageLinkTravelTime.put(l.getId(), 0.0);
		}
		initLinkEnterTime(pop);
	}

	@Override
	public void reset(int iteration) {
		iteration = 0;
		vehOnLanes.clear();
		vehOnLink.clear();
		agentLinkEnterTime.clear();
		averageLinkTravelTime.clear();
	}

	@Override
	public void handleEvent(LaneEnterEvent e) {
			vehOnLanes.put(e.getLinkId(), vehOnLanes.get(e.getLinkId())+1);
	}

	@Override
	public void handleEvent(LaneLeaveEvent e) {
			vehOnLanes.put(e.getLinkId(), vehOnLanes.get(e.getLinkId())-1);
	}

	@Override
	public void handleEvent(LinkEnterEvent e) {
		vehOnLink.put(e.getLinkId(), vehOnLink.get(e.getLinkId())+1);
		agentLinkEnterTime.put(e.getPersonId(),e.getTime());
	}
	@Override
	public void handleEvent(LinkLeaveEvent e) {
		vehOnLink.put(e.getLinkId(), vehOnLink.get(e.getLinkId())-1);
		averageLinkTravelTime.put(e.getLinkId(), (averageLinkTravelTime.get(e.getLinkId()) +
				e.getTime()- agentLinkEnterTime.get(e.getPersonId())) / 2);
		// Problem, Zeit wird erst gemessen, wenn Agent Link verlaesst. Stau existiert dann uU schon?!
	}
	@Override
	public void handleEvent(AgentDepartureEvent e) {
		vehOnLink.put(e.getLinkId(), vehOnLink.get(e.getLinkId())+1);
		agentLinkEnterTime.put(e.getPersonId(),e.getTime());
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


		double avSpeedOut = 10;
		boolean compGroupsGreen = false;
		double compGreenTime = 0;
		double approachingRed = 0; // product of time and approaching cars
		double approachingGreenLink = 0;
		double approachingGreenLane = 0;

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

		if (compGroupsGreen == true){
			for (Id i : compGroups.get(signalGroup.getId())){
				approachingGreenLink += vehOnLink.get((groups.get(i).getLinkRefId()));
				approachingGreenLane += vehOnLanes.get((groups.get(i).getLinkRefId()));
				if(compGreenTime < (time - switchedToGreen.get(i))){
						compGreenTime = switchedToGreen.get(i);
				}
			}
		}

		if (oldState.equals(SignalGroupState.RED)){
			approachingRed = vehOnLink.get(groups.get(signalGroup.getId()).getLinkRefId()) * compGreenTime;
		}else{
			approachingRed = 0;
		}

		//------- end of initializing

		//check if corresponding group is switched to green in this timestep. if so and no
		//compGroup shows green, switch to green
		if (!(switchedToGreen.get(corrGroups.get(signalGroup.getId())).equals(null)) &&
				switchedToGreen.get(corrGroups.get(signalGroup.getId())).equals(time) &&
				compGroupsGreen == false && oldState.equals(SignalGroupState.RED)){
			return switchLight(signalGroup, oldState, time);
		}

		// start algorithm
		if (avSpeedOut < vMin && oldState.equals(SignalGroupState.GREEN)){ //4
			return switchLight(signalGroup, oldState, time);
		}
		else if(avSpeedOut > vMin ){ // 12
			if (compGroupsGreen == false && oldState.equals(SignalGroupState.RED)){ //13
				return switchLight(signalGroup, oldState, time);
			}
			if (approachingRed > 0 && approachingGreenLink == 0){ //16
				return switchLight(signalGroup, oldState, time);
			}else {
				if ((time - compGreenTime) > tMin && vehOnLink.get(signalGroup.getLinkRefId()) > minVehicles){
						return switchLight(signalGroup, oldState, time);
				}
			}

		}
		if(this.getSignalGroupStates().get(signalGroup).equals(SignalGroupState.GREEN)){
				return true;
			}
		else{
		log.error("This should never happen! Mistake in adaptiveTrafficLightAlgorithm, no condition fits!");
		return false;
		}
	}

	public void setCorrGroups(Map<Id,Id> corrGroups){
		this.corrGroups = (SortedMap<Id, Id>) corrGroups;
	}

	public void setCompGroups(Map<Id, List<Id>> compGroups){
		this.compGroups = (SortedMap<Id, List<Id>>) compGroups;
	}
	public void initLinkEnterTime(Population pop){
		for (Person p : pop.getPersons().values()){
			agentLinkEnterTime.put(p.getId(), 0.0);
		}
	}

	public boolean switchLight (SignalGroupDefinition group, SignalGroupState oldState, double time){
		if (oldState.equals(SignalGroupState.GREEN)){
			this.getSignalGroupStates().put(group, SignalGroupState.RED);
			switchedToGreen.put(group.getId(), 0.0);
			return false;
		}else if (oldState.equals(SignalGroupState.RED)){
			this.getSignalGroupStates().put(group, SignalGroupState.GREEN);
			switchedToGreen.put(group.getId(), time);
			return true;
		}

		return false;
	}

}
