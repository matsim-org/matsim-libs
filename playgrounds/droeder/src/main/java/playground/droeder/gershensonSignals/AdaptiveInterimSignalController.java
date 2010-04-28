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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.signalsystems.config.AdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.systems.SignalGroupDefinition;

/**
 * 
 * Is this class needed? dg April 2010
 * @author droeder
 *
 */
public class AdaptiveInterimSignalController extends
		GershensonAdaptiveTrafficLightController implements EventHandler, SimulationBeforeSimStepListener {
	
	
	private static final Logger log = Logger
			.getLogger(AdaptiveInterimSignalController.class);
	
	protected Map<Id, Double> switchedToYellow =  new HashMap<Id, Double>();
	protected final double interGreenTime = 3;

	public AdaptiveInterimSignalController(AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
	}
	
	@Override
	public void init(Map<Id, Id> corrGroups,
			Map<Id, List<Id>> compGroups, Map<Id, Id> mainOutlinks, QNetwork net, CarsOnLinkLaneHandler handler){
		for(SignalGroupDefinition sd : this.getSignalGroups().values()){
			this.getSignalGroupStates().put(sd, SignalGroupState.RED);
			this.switchedToGreen.put(sd.getId(), 0.0);
			this.switchedToYellow.put(sd.getId(), 0.0);
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
		this.mainOutLinks = mainOutlinks;
		this.net = net;
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
	
	private void updateSignalGroupState(double time, SignalGroupDefinition signalGroup){
		
		if (signalGroup.getLinkRefId().equals(new IdImpl("89"))&& time == 22800){
			log.error(" ");
		}
		//check if this group was switched in this timestep. if so, return
		if (this.switchedToGreen.get(signalGroup.getId()).equals(time) || this.switchedToYellow.get(signalGroup.getId()).equals(time)){
			return;
		}
		
		// check interGreen
		if ((time-this.switchedToYellow.get(signalGroup.getId())) < interGreenTime){
			if (signalGroup.getLinkRefId().equals(new IdImpl("89"))&& time == 22800){
				log.error(" ");
			}
			return;
		}else if((time-this.switchedToYellow.get(signalGroup.getId())) == interGreenTime){
			if (signalGroup.getLinkRefId().equals(new IdImpl("89"))&& time ==22800){
				log.error(" ");
			}
			this.switchYellow(signalGroup, time);
			return;
		}else if((time-this.switchedToYellow.get(signalGroup.getId())) == (interGreenTime+1)){
			if (signalGroup.getLinkRefId().equals(new IdImpl("89"))&& time == 22800){
				log.error(" ");
			}
			this.switchRedYellow(signalGroup, time);
			return;
		}
		
		this.initIsGreen(time, signalGroup);
		if (signalGroup.getLinkRefId().equals(new IdImpl("89"))&& time == 22800){
			log.error(" ");
		}
		

		if (compGreenTime > 30 && compLinkJam == false && maxGreenTimeActive == true){
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
	}
	
	private void switchRedGreen(SignalGroupDefinition group, double time){
		if (this.oldState.equals(SignalGroupState.GREEN)){
			this.getSignalGroupStates().put(group, SignalGroupState.YELLOW);
			this.switchedToYellow.put(group.getId(), time);
			if (!(this.corrGroups.get(group.getId()) == null)){
				this.getSignalGroupStates().put(this.getSignalGroups().
							get(this.corrGroups.get(group.getId())),SignalGroupState.YELLOW);
				this.switchedToYellow.put(this.corrGroups.get(group.getId()), time);
			}
			
			if(!(this.compGroups.get(group.getId())== null)){
				for (Id i : this.compGroups.get(group.getId())){
					this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.RED);
					this.switchedToYellow.put(i, time);
				}
			}
		} else if(this.oldState.equals(SignalGroupState.RED)){
			this.switchedToYellow.put(group.getId(), time);
			this.getSignalGroupStates().put(group, SignalGroupState.RED);
			if (!(this.corrGroups.get(group.getId()) == null)){
				this.switchedToYellow.put(this.corrGroups.get(group.getId()), time);
				this.getSignalGroupStates().put(this.getSignalGroups().
						get(this.corrGroups.get(group.getId())),SignalGroupState.RED);
			}
			
			if(!(this.compGroups.get(group.getId())== null)){
				for (Id i : this.compGroups.get(group.getId())){
					this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.YELLOW);
					this.switchedToYellow.put(i, time);
				}
			}
		}
	}
	
	private void switchYellow(SignalGroupDefinition group, double time){
		if (this.oldState.equals(SignalGroupState.YELLOW)){
			this.getSignalGroupStates().put(group, SignalGroupState.RED);
			if (!(this.corrGroups.get(group.getId()) == null)){
				this.getSignalGroupStates().put(this.getSignalGroups().
						get(this.corrGroups.get(group.getId())),SignalGroupState.RED);
			}

			if(!(this.compGroups.get(group.getId())== null)){
				for (Id i : this.compGroups.get(group.getId())){
					this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.REDYELLOW);
				}
			}
		} else if (this.oldState.equals(SignalGroupState.RED)){ 
			this.getSignalGroupStates().put(group, SignalGroupState.REDYELLOW);
			if (!(this.corrGroups.get(group.getId()) == null)){
				this.getSignalGroupStates().put(this.getSignalGroups().
							get(this.corrGroups.get(group.getId())),SignalGroupState.REDYELLOW);
			}
			
			if(!(this.compGroups.get(group.getId())== null)){
				for (Id i : this.compGroups.get(group.getId())){
					this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.RED);
				}
			}
		}
	}
	
	private void switchRedYellow(SignalGroupDefinition group, double time){
		if (this.oldState.equals(SignalGroupState.RED)){
			this.getSignalGroupStates().put(group, SignalGroupState.RED);
			this.switchedToGreen.put(group.getId(), time);
			this.switchedToYellow.put(group.getId(), 0.0);

			if (!(this.corrGroups.get(group.getId()) == null)){
				this.getSignalGroupStates().put(this.getSignalGroups().
							get(this.corrGroups.get(group.getId())),SignalGroupState.RED);
				this.switchedToGreen.put(this.corrGroups.get(group.getId()), time);
				this.switchedToYellow.put(this.corrGroups.get(group.getId()), 0.0);
			}
			
			for (Id i : this.compGroups.get(group.getId())){
				this.switchedToGreen.put(i, time);
				this.switchedToYellow.put(i, 0.0);
				this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.GREEN);
			}

		} else if (oldState.equals(SignalGroupState.REDYELLOW)){
			this.getSignalGroupStates().put(group, SignalGroupState.GREEN);
			this.switchedToGreen.put(group.getId(), time);
			this.switchedToYellow.put(group.getId(), 0.0);

			if (!(this.corrGroups.get(group.getId()) == null)){
				this.getSignalGroupStates().put(this.getSignalGroups().
							get(this.corrGroups.get(group.getId())),SignalGroupState.GREEN);
				this.switchedToGreen.put(this.corrGroups.get(group.getId()), time);
				this.switchedToYellow.put(this.corrGroups.get(group.getId()), 0.0);
			}
			
			for (Id i : this.compGroups.get(group.getId())){
				this.switchedToGreen.put(i, time);
				this.switchedToYellow.put(i, 0.0);
				this.getSignalGroupStates().put(this.getSignalGroups().get(i),SignalGroupState.RED);
			}
		}
		
	}
}
