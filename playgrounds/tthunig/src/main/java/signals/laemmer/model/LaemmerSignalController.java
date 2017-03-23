/* *********************************************************************** *
 * project: org.matsim.*
 * DgTaController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package signals.laemmer.model;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreenTimesData;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreensForSignalSystemData;
import org.matsim.contrib.signals.model.*;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.data.Lane;

import com.google.inject.Provider;

import playground.dgrether.signalsystems.LinkSensorManager;

import java.util.Map;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class LaemmerSignalController  extends AbstractSignalController implements SignalController {

	private static final Logger log = Logger.getLogger(LaemmerSignalController.class);
	
	public static final String IDENTIFIER = "LaemmerSignalControl";

	private Id<SignalGroup> currentGreenTimeGroupId = null;
	private double remainingInBetweenTime = 0;
	private double currentInBetweenTime = 0;
	
	public final static class SignalControlProvider implements Provider<SignalController> {
		private final LinkSensorManager sensorManager;
		private final SignalsData signalsData;
		private final Network network;

		public SignalControlProvider(LinkSensorManager sensorManager, SignalsData signalsData, Network network) {
			this.sensorManager = sensorManager;
			this.signalsData = signalsData;
			this.network = network;
		}
		
		@Override
		public SignalController get() {
			return new LaemmerSignalController(sensorManager, signalsData, network);
		}
	}	
	
	private LinkSensorManager sensorManager;
	private SignalsData signalsData;
	private Network network;

	
	private LaemmerSignalController(LinkSensorManager sensorManager, SignalsData signalsData, Network network){
		this.sensorManager = sensorManager;
		this.signalsData = signalsData;
		this.network = network;
	}
	
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		this.initializeSensoring();

		for (SignalGroup group : this.system.getSignalGroups().values()){
			this.system.scheduleDropping(simStartTimeSeconds, group.getId());
		}
		
	}
	
	@Override
	public void updateState(double timeSeconds) {

		double maxIdx = 0;
		Id<SignalGroup> grp = null;
		
		for (SignalGroup group : this.system.getSignalGroups().values()){
			double idx = this.calculatePriorityIndex(timeSeconds, group);
			if(idx > maxIdx) {
				maxIdx = idx;
				grp = group.getId();
			}
		}
		if(grp != null  ) {

			if(remainingInBetweenTime > 0) {
				remainingInBetweenTime--;
			}

			if( grp != currentGreenTimeGroupId) {
				if(currentGreenTimeGroupId != null) {
					this.system.scheduleDropping(timeSeconds, currentGreenTimeGroupId);
				}
				double intergreen = 0;
				double amberTime = signalsData.getAmberTimesData().getDefaultAmber();
				if(currentGreenTimeGroupId != null) {
					intergreen = signalsData.getIntergreenTimesData().getIntergreensForSignalSystemDataMap()
							.get(this.system.getId()).getIntergreenTime(currentGreenTimeGroupId, grp);
				}
				currentInBetweenTime = remainingInBetweenTime = intergreen + amberTime;
				currentGreenTimeGroupId = grp;
				this.system.scheduleOnset(timeSeconds + currentInBetweenTime, grp);
			}
		}
	}

	//TODO: add inbetweentime
	private double calculatePriorityIndex(double timeSeconds, SignalGroup group) {

		double index = 0;

		Id<Link> linkId = group.getSignals().values().iterator().next().getLinkId();
		double maxFlow = this.network.getLinks().get(linkId).getCapacity() / 3600.;

		if(group.getId().equals(currentGreenTimeGroupId)) {
			for(double i = remainingInBetweenTime ; i<= currentInBetweenTime; i++) {
				double n = this.getNumberOfExpectedVehicles(timeSeconds+i, linkId);
				double reqGreenTime = n / maxFlow;
				double tempIndex = n / (i + reqGreenTime);
				if(tempIndex > index) {
					index = tempIndex;
				}
			}
		} else {
			double intergreen = 0;
			double amberTime = signalsData.getAmberTimesData().getDefaultAmber();
			if(currentGreenTimeGroupId != null) {
			    intergreen = signalsData.getIntergreenTimesData().getIntergreensForSignalSystemDataMap().get(this.system.getId()).getIntergreenTime(currentGreenTimeGroupId, group.getId());
			}
			double inBetweenTime = intergreen + amberTime;
			double n = this.getNumberOfExpectedVehicles(timeSeconds + inBetweenTime, linkId);
			double reqGreenTime = n / maxFlow;
			double penalty = calculateAbortionPenalty(timeSeconds);
			index = n / (penalty + inBetweenTime + reqGreenTime);
		}
		return index;
	}

	private double calculateAbortionPenalty(double timeSeconds) {
		if(currentGreenTimeGroupId == null) {
			return 0;
		}
		Id<Link> linkId = this.system.getSignalGroups().get(currentGreenTimeGroupId).getSignals().values().iterator().next().getLinkId();
		double waitingTimeSum = 0;
		for(double i = remainingInBetweenTime; i <= currentInBetweenTime; i++) {
			waitingTimeSum += (waitingTimeSum + this.getNumberOfExpectedVehicles(timeSeconds + i, linkId));
		}
		double n = this.getNumberOfExpectedVehicles(timeSeconds + currentInBetweenTime, linkId);
		if(n==0) {
			return 0;
		}
		return waitingTimeSum / n ;
	}

	/**
	 * 
	 * @return \hat{n_i} (t)
	 */
	private int getNumberOfVehiclesForClearance(){
		
		return 0;
	}
	
	
	/**
	 * Zeitreihe der erwarteten Ankuenfte an der Haltelinie
	 * 
	 * N_i^{exp}(t + \hat(g)_i))
	 * 
	 */
	private int getNumberOfExpectedVehicles(double timeSeconds, Id<Link> linkId){
		Link link = network.getLinks().get(linkId);
		return this.sensorManager.getNumberOfCarsInDistance(linkId, 0., timeSeconds);
	}
	
	
	@Override
	public void reset(Integer iterationNumber) {
	}

	
	private void initializeSensoring(){
		for (SignalGroup group :  this.system.getSignalGroups().values() ){
			for (Signal signal : group.getSignals().values()) {
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
					//					log.error("system: " + this.system.getId() + " signal: " + signal.getId() + " has no lanes...");
					this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
				}
				else {
					for (Id<Lane> laneId : signal.getLaneIds()){
						//TODO check this part again concerning implementation of CarsOnLaneHandler
						this.sensorManager.registerNumberOfCarsMonitoringOnLane(signal.getLinkId(), laneId);
					}
				}
			}
		}
	}
	
	

}
