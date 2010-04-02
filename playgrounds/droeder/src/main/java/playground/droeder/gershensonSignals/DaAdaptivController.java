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
public class DaAdaptivController extends
	AdaptiveSignalSystemControlerImpl implements EventHandler, SimulationBeforeSimStepListener {

		private static final Logger log = Logger.getLogger(GershensonAdaptiveTrafficLightController.class);

		protected int tGreenMin =  0; // time in seconds
		protected int minCarsTime = 0; //
		protected double capFactor = 0;
		protected double maxRedTime ;
		
		private boolean interim = false;
		private double interimTime;
		private SignalGroupDefinition interimGroup;

		protected boolean outLinkJam;
		protected boolean maxRedTimeActive = false;
		protected double compGreenTime;
		protected double approachingRed;
		protected double approachingGreenLink;
		protected double approachingGreenLane;
		protected double carsOnRefLinkTime;
		protected boolean compGroupsGreen;
		protected SignalGroupState oldState;

		protected CarsOnLinkLaneHandler handler;

		private double switchedGreen = 0;
		private Map<Id, Double> switchedToRed;
		
		private Map<Id, SortedMap<Double, Double>> demandOnRefLink = new HashMap<Id, SortedMap<Double,Double>>();
		
		

		protected Map<Id, List<SignalGroupDefinition>> corrGroups;
		protected Map<Id, Id> mainOutLinks;

		protected QNetwork net;


		public DaAdaptivController(AdaptiveSignalSystemControlInfo controlInfo) {
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
			switchedToRed  = new HashMap<Id, Double>();
			for (Entry<Id, List<SignalGroupDefinition>> ee : corrGroups.entrySet()){
				switchedToRed.put(ee.getKey(), 21600.0);
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
			this.corrGroups = corrGroups;
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

			// check if competing groups are green
			out : 
			for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
				if(!(e.getValue().contains(signalGroup))){
					for (SignalGroupDefinition sd : e.getValue()){
						if(this.getSignalGroupStates().get(sd).equals(SignalGroupState.GREEN)){
							compGroupsGreen = true;
							break out;
						}else{
							compGroupsGreen = false;
						}
					}
				}
			}
			
			// calculate outlinkCapacity for the mainOutlink and check if there is a trafficJam
			if (!(mainOutLinks.get(signalGroup.getLinkRefId()) ==  null)){
				double outLinkCapacity = 0;
				double actStorage = 0;
				for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
					if(e.getValue().contains(signalGroup)){
						for (SignalGroupDefinition sd : e.getValue()){
							if (!(mainOutLinks.get(sd.getLinkRefId()) ==  null)){
								outLinkCapacity += net.getLinks().get(mainOutLinks.get(sd.getLinkRefId())).getSpaceCap();
								actStorage += handler.getVehOnLink(mainOutLinks.get(sd.getLinkRefId()));
							}
						}
					}
				}
				if((outLinkCapacity*capFactor)< actStorage){
					outLinkJam = true;
				}
			} else {
				outLinkJam = false;
			}
			
			// check competing links for trafficJam
//			compLinkJam = true;
//			out : 
//				for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
//					if(!(e.getValue().contains(signalGroup))){
//						for (SignalGroupDefinition sd : e.getValue()){
//							double compCap = net.getLinks().get(sd.getLinkRefId()).getSpaceCap();
//							double compStorage = handler.getVehOnLink(sd.getLinkRefId());
//							if ((compCap*capFactor)>compStorage){
//								compLinkJam = false;
//								break out;
//							}
//						}
//					}
//				}

			//set number of cars, approaching a competing Link in a short distance, if it is green
			if (compGroupsGreen == true){
				for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
					if(!(e.getValue().contains(signalGroup))){
						for (SignalGroupDefinition sd : e.getValue()){
							approachingGreenLane = handler.getVehOnLinkLanes(sd.getLinkRefId());
							approachingGreenLink += handler.getVehInD(time, sd.getLinkRefId());
//							if((compGreenTime< (time-switchedToGreen.get(sd.getId())))){
//								compGreenTime = (time - switchedToGreen.get(sd.getId()));
//							}
							if((compGreenTime< (time-switchedGreen))){
								compGreenTime = (time - switchedGreen);
							}
						}
					}
				}
			}

			// set number of cars on refLink of signalGroup
			for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
				if(e.getValue().contains(signalGroup)){
					for (SignalGroupDefinition sd : e.getValue()){
						this.carsOnRefLinkTime += handler.getVehInD(time, sd.getLinkRefId());
					}
				}
			}
			this.carsOnRefLinkTime = this.carsOnRefLinkTime * compGreenTime;

			// 	number of cars approaching a red light
			if (this.oldState.equals(SignalGroupState.RED)){
				for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
					if(e.getValue().contains(signalGroup)){
						for (SignalGroupDefinition sd : e.getValue()){
							this.approachingRed += handler.getVehInD(time, sd.getLinkRefId());
						}
					}
				}
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
			
			//check if this group was switched in this timestep. if so, return oldstate
			if (this.switchedGreen == e.getSimulationTime()){
				return;
			}
			
			// disable algorithm if interim is active
			if (interim == true){
				this.initIsGreen(e.getSimulationTime(), interimGroup);
				this.switchInterim(interimGroup, e.getSimulationTime());
				return;
			}
			
			//switch RedLights first
			if (maxRedTimeActive == true){
				Id id = new IdImpl("null");
				double redTime = 0;
				for (Entry<Id, Double> ee : switchedToRed.entrySet()){
					if ((e.getSimulationTime() - ee.getValue()) > maxRedTime && ee.getValue()>redTime){
						id = ee.getKey();
						redTime = ee.getValue();
					}
				}
				if (!id.equals(new IdImpl("null"))){
					for (SignalGroupDefinition sd : corrGroups.get(id)){
						if(handler.getVehOnLink(sd.getLinkRefId())>0){
							this.startSwitching(sd, e.getSimulationTime());
						}
						// return if interim is true, because a group was switched in this timestep
						if (interim == true){
							return;
						}
					}
				}
			}
			
			//sort groups by demand
			double temp;
			HashMap<Id, Double> map = new HashMap<Id, Double>();
			ValueComparator bvc =  new ValueComparator(map);
			TreeMap<Id, Double> sorted_map = new TreeMap<Id, Double>(bvc);
			for (Entry <Id, List<SignalGroupDefinition>> ee : corrGroups.entrySet()){
				temp = 0;
				for (SignalGroupDefinition sd : ee.getValue()){
					temp += handler.getVehInD(e.getSimulationTime(), sd.getLinkRefId());
				}
				map.put(ee.getKey(), temp);
			}
			sorted_map.putAll(map);
			
			// iterate over groups sorted by demand.
			for (Entry<Id, Double> ee : sorted_map.entrySet()){
				for (SignalGroupDefinition sd : corrGroups.get(ee.getKey())){
					this.updateSignalGroupState(e.getSimulationTime(), this.getSignalGroups().get(sd.getId()));
					// return if interim is true, because a group was switched in this timestep
					if (interim == true){
						return;
					}
				}
			}
		}
		
		private void updateSignalGroupState(double time, SignalGroupDefinition signalGroup) {

			this.initIsGreen(time, signalGroup);
			
			
			// algorithm starts
			if ((this.outLinkJam == true) && this.oldState.equals(SignalGroupState.GREEN)){ //Rule 5 + 6
				this.startSwitching(signalGroup, time);
				return;
			} else if(this.outLinkJam == false ){
				if (this.compGroupsGreen == false && this.oldState.equals(SignalGroupState.RED)){ // Rule 6
					this.startSwitching(signalGroup, time);
				  return;
				}
				if (this.approachingRed > 0 && this.approachingGreenLink == 0){ // Rule 4
					this.startSwitching(signalGroup, time);
					return;
				}else if(!(this.approachingGreenLane > 0)){  //Rule 3
					if ((this.compGreenTime) > this.tGreenMin && this.carsOnRefLinkTime > this.minCarsTime){ // Rule 1 + 2
						this.startSwitching(signalGroup, time);
						return;
					}
				}
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
			
			if (oldState.equals(SignalGroupState.GREEN)|| oldState.equals(SignalGroupState.RED)){
				this.switchToYellow(group, time);
			} else if(oldState.equals(SignalGroupState.RED) && compGroupsGreen == false){
				switchToRedYellow(group, time);
				this.interimTime = 3;
			} 
			
		}
		
		
		private void switchInterim (SignalGroupDefinition group, double time){
			this.interimTime++;
			double temp = this.interimTime;
			
			if (temp == 3){
				if (oldState.equals(SignalGroupState.YELLOW)){
					interim = false;
					switchToRed(group, time);
					//start algorithm new?!
				}else if (oldState.equals(SignalGroupState.RED)){
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
						switchedToRed.put(e.getKey(), time);
					}
				}
				
//				if(e.getValue().contains(group)){
//					for (SignalGroupDefinition sd : e.getValue()){
//						fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.RED);
//						this.getSignalGroupStates().put(sd,SignalGroupState.RED);
//					}
//				}
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
							switchedToRed.put(e.getKey(), time);
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
//				if(e.getValue().contains(group)){
//					for (SignalGroupDefinition sd : e.getValue()){
//						fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.YELLOW);
//						this.getSignalGroupStates().put(sd,SignalGroupState.YELLOW);
//					}
//				}
			}
		}
		
		private void switchToGreen(SignalGroupDefinition group, double time){
			for (Entry<Id, List<SignalGroupDefinition>> e : corrGroups.entrySet()){
				if(e.getValue().contains(group)){
					for (SignalGroupDefinition sd : e.getValue()){
						fireChangeEvent(time, sd.getSignalSystemDefinitionId(), sd.getId(),SignalGroupState.GREEN);
						this.getSignalGroupStates().put(sd,SignalGroupState.GREEN);
						switchedToRed.put(e.getKey(), 99999.0);
					}
				}
			}
			this.switchedGreen = time;
		}
		
		/*
		 * use this method to set parameters minimumGreenTime u, minimum of the product cars and waitingTime n, the capacityFactor for trafficJam on the outlink
		 * and the maximumRedTime ( 0 == disable maximumRedTime)
		 */
		public void setParameters (int minCarsTime, int tGreenMin, double capFactor, int maxRedTime){
			this.minCarsTime = minCarsTime;
			this.tGreenMin = tGreenMin;
			this.capFactor = capFactor;
			this.maxRedTime = maxRedTime;
			
			if (maxRedTime == 0){
				maxRedTimeActive = false;
			}else{
				maxRedTimeActive = true;
			}
		}
		
		public Map<Id, SortedMap<Double, Double>> getDemandOnRefLink(){
			return demandOnRefLink;
		}

		public void reset(int iteration) {
			iteration=0;
		}


}

