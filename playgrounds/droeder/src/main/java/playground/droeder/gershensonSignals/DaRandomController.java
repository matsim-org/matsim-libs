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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.SignalGroupStateChangedEventImpl;
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
public class DaRandomController extends
AdaptiveSignalSystemControlerImpl implements EventHandler, SimulationBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(GershensonAdaptiveTrafficLightController.class);

	
	private boolean interim = false;
	private double interimTime;
	private SignalGroupDefinition interimGroup;
	
	private double randomTime;
	private double switched;

	protected CarsOnLinkLaneHandler handler;

	private Map<Id, SortedMap<Double, Double>> demandOnRefLink = new HashMap<Id, SortedMap<Double,Double>>();
	protected Map<Id, List<SignalGroupDefinition>> corrGroups;
	protected Map<Id, Id> mainOutLinks;
	protected QNetwork net;

	public DaRandomController(AdaptiveSignalSystemControlInfo controlInfo) {
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
	public void init(Map<Id, List<SignalGroupDefinition>> corrGroups, Map<Id, Id> mainOutLinks, QNetwork net, CarsOnLinkLaneHandler handler){
		SortedMap<Double, Double> temp;
		for(SignalGroupDefinition sd : this.getSignalGroups().values()){
			this.getSignalGroupStates().put(sd, SignalGroupState.RED);
			fireChangeEvent(21600.0, sd.getSignalSystemDefinitionId(), sd.getId(), SignalGroupState.RED);
			temp = new TreeMap<Double, Double>();
			demandOnRefLink.put(sd.getId(), temp);
		}

		this.handler  = handler;
		this.corrGroups = corrGroups;
		this.net = net;
	}

	
	@Override
	public SignalGroupState getSignalGroupState(double seconds,
		SignalGroupDefinition signalGroup) {
		return this.getSignalGroupStates().get(signalGroup);
	}

	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		
		// disable algorithm if interim is active
		if (interim == true){
			this.switchInterim(interimGroup, e.getSimulationTime());
			return;
		}
		
		if (switched + randomTime < e.getSimulationTime()){
			SignalGroupDefinition sd = null;
			do{
				for (SignalGroupDefinition sd2 : this.getSignalGroups().values()){
					if (Math.random()< 1/Math.pow(10, this.getSignalGroups().size())){
						sd = sd2;
					}
				}
			}while (sd == null);
			
			if (this.getSignalGroupState(e.getSimulationTime(), sd).equals(SignalGroupState.GREEN)){
				randomTime = 0;
			}else{
				randomTime = (Math.random()*80) + 10;
			}
			switched = e.getSimulationTime();
			startSwitching(sd, e.getSimulationTime());
			
		}
		
		
		
	}
	
	
	private void fireChangeEvent(double time, Id signalSystem, Id signalgroup, SignalGroupState newState){
		this.getSignalEngine().getEvents().processEvent(
	              new SignalGroupStateChangedEventImpl(time, signalSystem, 
	                  signalgroup, newState));
	}

	private void startSwitching(SignalGroupDefinition group, double time){
		this.interim = true;
		this.interimTime = 0;
		this.interimGroup = group;
		
		for (SignalGroupDefinition sd : this.getSignalGroups().values()){
			demandOnRefLink.get(sd.getId()).put(time, handler.getVehInD(time, sd.getLinkRefId()));
		}
		
		if (getSignalGroupState(time, group).equals(SignalGroupState.GREEN)|| getSignalGroupState(time, group).equals(SignalGroupState.RED)){
			this.switchToYellow(group, time);
		} 
		
	}
	
	
	private void switchInterim (SignalGroupDefinition group, double time){
		this.interimTime++;
		double temp = this.interimTime;
		
		if (temp == 3){
			if (getSignalGroupState(time, group).equals(SignalGroupState.YELLOW)){
				interim = false;
				switchToRed(group, time);
				//start algorithm new?!
			}else if (getSignalGroupState(time, group).equals(SignalGroupState.RED)){
				switchToRedYellow(group, time);
			}
		}else if (temp >3){
			this.interim = false;
			switchToGreen(group, time);
		}
	}
	
	private void switchToRed(SignalGroupDefinition group, double time){
		for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
			for (SignalGroupDefinition sd : e.getValue()){
				if (getSignalGroupState(time, sd).equals(SignalGroupState.YELLOW)){
					fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.RED);
					this.getSignalGroupStates().put(sd,SignalGroupState.RED);
				}
			}
			
//			if(e.getValue().contains(group)){
//				for (SignalGroupDefinition sd : e.getValue()){
//					fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.RED);
//					this.getSignalGroupStates().put(sd,SignalGroupState.RED);
//				}
//			}
		}
	}
	
	private void switchToRedYellow(SignalGroupDefinition group, double time){
		for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
			
			if (e.getValue().contains(group)){
				for (SignalGroupDefinition sd : e.getValue()){
					fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.REDYELLOW);
					this.getSignalGroupStates().put(sd,SignalGroupState.REDYELLOW);
				}
			} else {
				for (SignalGroupDefinition sd : e.getValue()){
					if (getSignalGroupState(time, sd).equals(SignalGroupState.YELLOW)){
						fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.RED);
						this.getSignalGroupStates().put(sd,SignalGroupState.RED);
					}
				}
			}
		}
	}
	

	private void switchToYellow(SignalGroupDefinition group, double time){
		for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
			for (SignalGroupDefinition sd : e.getValue()){
				if(getSignalGroupState(time, sd).equals(SignalGroupState.GREEN)){
					fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.YELLOW);
					this.getSignalGroupStates().put(sd,SignalGroupState.YELLOW);
				}
			}
//			if(e.getValue().contains(group)){
//				for (SignalGroupDefinition sd : e.getValue()){
//					fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.YELLOW);
//					this.getSignalGroupStates().put(sd,SignalGroupState.YELLOW);
//				}
//			}
		}
	}
	
	private void switchToGreen(SignalGroupDefinition group, double time){
		for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
			if(e.getValue().contains(group)){
				for (SignalGroupDefinition sd : e.getValue()){
					fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.GREEN);
					this.getSignalGroupStates().put(sd,SignalGroupState.GREEN);
				}
			}
		}
	}
	
	/*
	 * use this method to set parameters minimumGreenTime u, minimum of the product cars and waitingTime n, the capacityFactor for trafficJam on the outlink
	 * and the maximumRedTime ( 0 == disable maximumRedTime)
	 */
	
	public Map<Id, SortedMap<Double, Double>> getDemandOnRefLink(){
		return demandOnRefLink;
	}

	public void reset(int iteration) {
		iteration=0;
	}
}
