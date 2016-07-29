/* *********************************************************************** *
 * project: org.matsim.*
 * DgSylviaController
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
package playground.dgrether.signalsystems.sylvia.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.DatabasedSignalPlan;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.data.v20.Lane;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.data.DgSylviaPreprocessData;
import playground.dgrether.signalsystems.utils.DgAbstractSignalController;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;


/**
 * @author dgrether
 *
 */
public class DgSylviaController extends DgAbstractSignalController implements SignalController {

	private static final Logger log = Logger.getLogger(DgSylviaController.class);
	
	public final static String CONTROLLER_IDENTIFIER = "SylviaSignalControl";

	private static int sylviaPlanDumpCount = 0;
	
	private DgSylviaSignalPlan activeSylviaPlan = null;
	private boolean extensionActive = false;
	private boolean forcedExtensionActive = false;
	private int secondInSylviaCycle = -1;
	private Map<Integer, DgExtensionPoint> extensionPointMap = null;
	private Map<Integer, DgExtensionPoint> forcedExtensionPointMap = null;
	private Map<Id<SignalGroup>, Double> greenGroupId2OnsetMap = null;
	private int extensionTime = 0;
	private int secondInCycle = -1; //used for debug output

	private DgSylviaConfig sylviaConfig;
	
	private DgExtensionPoint currentExtensionPoint = null;

	private DgSensorManager sensorManager = null;

	public DgSylviaController(DgSylviaConfig sylviaConfig, DgSensorManager sensorManager) {
		this.sylviaConfig = sylviaConfig;
		this.sensorManager = sensorManager;
		this.init();
	}
	
	private void init() {
		this.currentExtensionPoint = null;
		this.activeSylviaPlan = null;
		this.extensionActive = false;
		this.forcedExtensionActive = false;
		this.greenGroupId2OnsetMap = new HashMap<>();
		this.extensionPointMap = new HashMap<>();
		this.forcedExtensionPointMap = new HashMap<>();
		this.initCylce();
	}


	private void initCylce() {
		this.secondInSylviaCycle = -1; //as this is incremented before use
		this.secondInCycle = -1;
		this.extensionTime = 0;
	}
	
	
	@Override
	public void updateState(double timeSeconds) {
		int secondInFixedTimeCycle = (int) (timeSeconds % this.activeSylviaPlan.getFixedTimeCycle());
		if (this.secondInSylviaCycle == this.activeSylviaPlan.getCycleTime() - 1) {
			this.initCylce();
		}
		this.secondInCycle++;
		
		if (this.forcedExtensionActive){
			this.forcedExtensionActive = this.checkForcedExtensionCondition();
			return;
		}
		else if (this.extensionActive){
			this.extensionTime++;
			if (! this.checkExtensionCondition(timeSeconds, this.currentExtensionPoint)) {
				this.stopExtension();
			}
			return;
		}
		else {
			this.secondInSylviaCycle++;
			//check for forced extension trigger
			if (this.forcedExtensionPointMap.containsKey(this.secondInSylviaCycle)){
				if (this.checkForcedExtensionCondition()){
					this.forcedExtensionActive = true;
					return;
				}
			}
			//check for extension trigger
			else if (this.extensionPointMap.containsKey(this.secondInSylviaCycle)){
				this.currentExtensionPoint = this.extensionPointMap.get(this.secondInSylviaCycle);
				if (this.checkExtensionCondition(timeSeconds, this.currentExtensionPoint)){
					this.extensionActive = true;
					return;
				}
				else { // disable all used state variables of a extension
					this.stopExtension();
				}
			}
			//else no extension...
			List<Id<SignalGroup>> droppings = this.activeSylviaPlan.getDroppings(this.secondInSylviaCycle);
			if (droppings != null){
				for (Id<SignalGroup> groupId : droppings){
					this.system.scheduleDropping(timeSeconds, groupId);
					this.greenGroupId2OnsetMap.remove(groupId);
				}
			}
			List<Id<SignalGroup>> onsets = this.activeSylviaPlan.getOnsets(this.secondInSylviaCycle);
			if (onsets != null){
				for (Id<SignalGroup> groupId : onsets){
					this.system.scheduleOnset(timeSeconds, groupId);
					this.greenGroupId2OnsetMap.put(groupId, timeSeconds);
				}
			}
		}
	}
	
	private void stopExtension(){
		this.extensionActive = false;
		this.currentExtensionPoint = null;
	}

	
	private boolean isExtensionTimeLeft(){
		if (this.sylviaConfig.isUseFixedTimeCycleAsMaximalExtension()){
			return this.extensionTime < this.activeSylviaPlan.getMaxExtensionTime();
		}
		return true;
	}
	
	private boolean isGreenTimeLeft(double timeSeconds, Id<SignalGroup> groupId, int maxGreenTime){
		int greenTime = (int) (timeSeconds - this.greenGroupId2OnsetMap.get(groupId));
		return greenTime < maxGreenTime;
	}
	
	private boolean checkForcedExtensionCondition(){
		if (this.isExtensionTimeLeft()) {
			return true;
		}
		return false;
	}
	
	private boolean checkExtensionCondition(double timeSeconds, DgExtensionPoint extensionPoint){
		if (this.isExtensionTimeLeft()) {
			//check if there is some green time left of one of the groups is over its maximal green time
			boolean greenTimeLeft = true;
			for (Id<SignalGroup> signalGroupId : extensionPoint.getSignalGroupIds()){
					if (! this.isGreenTimeLeft(timeSeconds, signalGroupId, extensionPoint.getMaxGreenTime(signalGroupId))){
						greenTimeLeft = false;
					}
			}
			//if there is green time left check traffic state
			if (greenTimeLeft){
				return this.checkTrafficConditions(timeSeconds, extensionPoint);
			}
		}
		return false;
	}
	

	private boolean checkTrafficConditions(double timeSeconds, DgExtensionPoint extensionPoint){
		int noCars = 0;
		for (SignalData signal : extensionPoint.getSignals()){
			if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
				noCars = this.sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), this.sylviaConfig.getSensorDistanceMeter(), timeSeconds);
				if (noCars > 0){
					return true;
				}
			}
			else {
				for (Id<Lane> laneId : signal.getLaneIds()){
					noCars = this.sensorManager.getNumberOfCarsOnLane(signal.getLinkId(), laneId);
					if (noCars > 0){
						return true;
					}
				}
			}
		}		
		return false;
	}
	
	
	@Override
	public void reset(Integer iterationNumber) {
		this.init();
	}
	
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		Tuple<SignalPlan, DgSylviaSignalPlan> plans = this.searchActivePlans();
		this.activeSylviaPlan = plans.getSecond();
		this.activeSylviaPlan.setFixedTimeCycle(plans.getFirst().getCycleTime());
		this.setMaxExtensionTimeInSylviaPlan(plans);
		this.calculateExtensionPoints(plans);
		if (sylviaPlanDumpCount < 1){
			this.dumpSylviaPlan();
			sylviaPlanDumpCount++;
		}
		this.initializeSensoring();
	}


	private void setMaxExtensionTimeInSylviaPlan(Tuple<SignalPlan, DgSylviaSignalPlan> plans) {
		int ext = plans.getFirst().getCycleTime() - plans.getSecond().getCycleTime();
		plans.getSecond().setMaxExtensionTime(ext);
	}


	private Tuple<SignalPlan,DgSylviaSignalPlan> searchActivePlans() {
		DgSylviaSignalPlan sylviaPlan = null;
		SignalPlan fixedTimePlan = null;
		for (Id<SignalPlan> planId : this.signalPlans.keySet()){
			if (planId.toString().startsWith(DgSylviaPreprocessData.SYLVIA_PREFIX)){
				sylviaPlan = (DgSylviaSignalPlan) this.signalPlans.get(planId);
			}
			if (planId.toString().startsWith(DgSylviaPreprocessData.FIXED_TIME_PREFIX)){
				fixedTimePlan = this.signalPlans.get(planId);
			}
		}
		if (sylviaPlan == null && fixedTimePlan == null){
			throw new IllegalStateException("No suitable plans found for controller of signal system: " + this.system.getId());
		}
		return new Tuple<SignalPlan, DgSylviaSignalPlan>(fixedTimePlan, sylviaPlan);
	}

	
	private void calculateExtensionPoints(Tuple<SignalPlan,DgSylviaSignalPlan> plans) {
		SignalPlan fixedTime = plans.getFirst();
		DgSylviaSignalPlan sylvia = plans.getSecond();
		int offset = 0;
		if (sylvia.getOffset() != null){
			offset = sylvia.getOffset();
		}
		for (SignalGroupSettingsData settings : sylvia.getPlanData().getSignalGroupSettingsDataByGroupId().values()){
			Integer dropping;
			if (settings.getDropping() == 0) {
				dropping = sylvia.getCycleTime() - 1; 
			}
			else {
				//set the extension point one second before the dropping
				dropping = settings.getDropping() - 1 + offset;
			}
			DgExtensionPoint extPoint = null;
			if (! this.extensionPointMap.containsKey(dropping)){
				extPoint = new DgExtensionPoint(dropping);
				this.extensionPointMap.put(dropping, extPoint);
				sylvia.addExtensionPoint(extPoint);
			}
			extPoint = this.extensionPointMap.get(dropping);
			extPoint.addSignalGroupId(settings.getSignalGroupId());

			//calculate max green time
			SignalGroupSettingsData fixedTimeSettings = ((DatabasedSignalPlan)fixedTime).getPlanData().getSignalGroupSettingsDataByGroupId().get(settings.getSignalGroupId());
			int fixedTimeGreen = DgSignalsUtils.calculateGreenTimeSeconds(fixedTimeSettings, fixedTime.getCycleTime());
			int maxGreen = (int) (fixedTimeGreen * this.sylviaConfig.getSignalGroupMaxGreenScale());
			if (maxGreen >= fixedTime.getCycleTime()){
				maxGreen = fixedTimeGreen;
			}
			extPoint.setMaxGreenTime(settings.getSignalGroupId(), maxGreen);
		}
	}

	private void dumpSylviaPlan() {
		log.debug("Signal System: "+ this.system.getId() + " Plan: " + this.activeSylviaPlan.getPlanData().getId());
		log.debug("  Maximal time for extension: " + this.activeSylviaPlan.getMaxExtensionTime());
		for (DgExtensionPoint p : this.extensionPointMap.values()){
			log.debug("  ExtensionPoint at: " + p.getSecondInPlan() + " groups: ");
			for (Id<SignalGroup> sgId : p.getSignalGroupIds()){
				log.debug("    SignalGroup: " + sgId + " maxGreen: "+ p.getMaxGreenTime(sgId));
			}
		}
	}

	
	private void initializeSensoring(){
		for (DgExtensionPoint extPoint : this.extensionPointMap.values()){
			Set<SignalData> extPointSignals = new HashSet<SignalData>();
			for (Id<SignalGroup> signalGroupId : extPoint.getSignalGroupIds()){
				SignalSystemData systemData = this.system.getSignalSystemsManager().getSignalsData().getSignalSystemsData().getSignalSystemData().get(this.system.getId());
				SignalGroupData signalGroup = this.system.getSignalSystemsManager().getSignalsData().getSignalGroupsData().getSignalGroupDataBySystemId(systemData.getId()).get(signalGroupId);
				Set<SignalData> signals = DgSignalsUtils.getSignalDataOfSignalGroup(systemData, signalGroup);
				extPointSignals.addAll(signals);
			}
			extPoint.addSignals(extPointSignals);
			for (SignalData signal : extPointSignals){
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
					this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), this.sylviaConfig.getSensorDistanceMeter());
				}
				else {
					for (Id<Lane> laneId : signal.getLaneIds()){
						this.sensorManager.registerNumberOfCarsMonitoringOnLane(signal.getLinkId(), laneId);
					}
				}
			}
		}
	}
	
	
}
