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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.model.DatabasedSignalPlan;
import org.matsim.signalsystems.model.SignalController;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.sylvia.data.DgSylviaPreprocessData;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;


/**
 * @author dgrether
 *
 */
public class DgSylviaController implements SignalController {

	private static final Logger log = Logger.getLogger(DgSylviaController.class);
	
	public final static String CONTROLLER_IDENTIFIER = "SylviaSignalControl";

	private final static double SENSOR_DISTANCE = 30.0;
	
	private final static double GAP_SECONDS = 5.0;
	
	private SignalSystem system = null;
	private Map<Id, SignalPlan> signalPlans = new HashMap<Id, SignalPlan>();
	
	private DgSylviaSignalPlan activeSylviaPlan = null;
	private boolean extensionActive = false;
	private boolean forcedExtensionActive = false;
	private int secondInSylviaCycle = 0;
	private Map<Integer, DgExtensionPoint> extensionPointMap = null;
	private Map<Integer, DgExtensionPoint> forcedExtensionPointMap = null;
	private Map<Id, Double> greenGroupId2OnsetMap = null;
	private int extensionTime = 0;
	private int secondInCycle = 0;

	private DgExtensionPoint currentExtensionPoint = null;

	private DgSensorManager sensorManager = null;

	private Map<DgExtensionPoint, List<Id>> links4extensionPointMap = new HashMap<DgExtensionPoint, List<Id>>();
	
	private static final class SensorRecord {

		public Map<Id, Integer> linkIdNoCarsMap;
		
		public SensorRecord() {
			this.linkIdNoCarsMap = new HashMap<Id, Integer>();
		}
	}

	private Map<Double, SensorRecord> lastTimeStepSensorRecordsMap = null;
	
	public DgSylviaController(DgSensorManager sensorManager) {
		this.sensorManager = sensorManager;
		this.init();
	}
	
	private void init() {
		this.lastTimeStepSensorRecordsMap = new HashMap<Double, SensorRecord>();
		this.currentExtensionPoint = null;
		this.activeSylviaPlan = null;
		this.extensionActive = false;
		this.forcedExtensionActive = false;
		this.lastTimeStepSensorRecordsMap.clear();
		this.greenGroupId2OnsetMap = new HashMap<Id, Double>();
		this.links4extensionPointMap = new HashMap<DgExtensionPoint, List<Id>>();
		this.extensionPointMap = new HashMap<Integer, DgExtensionPoint>();
		this.forcedExtensionPointMap = new HashMap<Integer, DgExtensionPoint>();
		//TODO is there any datastructure to reset? 
		this.initCylce();
	}


	private void initCylce() {
		this.secondInSylviaCycle = -1; //as this is incremented before use
		this.extensionTime = 0;
		this.secondInCycle = 0;
	}
	
	
	@Override
	public void updateState(double timeSeconds) {
		this.secondInCycle++;
		//TODO check sylvia timer reset
//		log.info("time: " + timeSeconds + " sylvia timer: " + this.secondInSylviaCycle + " fixed-time timer: " + this.secondInCycle);
		int secondInFixedTimeCycle = (int) (timeSeconds % this.activeSylviaPlan.getFixedTimeCycle());
//		if (secondInFixedTimeCycle == 0){
		if (this.secondInSylviaCycle == this.activeSylviaPlan.getCycleTime()){
//			log.error("Reset cycle timers at " + timeSeconds);
//			log.error("  sylvia timer: " + this.secondInSylviaCycle + " sylvia cycle: " + this.activeSylviaPlan.getCycleTime());
//			log.error("  fixed-time timer: " + secondInFixedTimeCycle + " fixed-time cycle: " + this.activeSylviaPlan.getFixedTimeCycle());
//			log.error("  cylce length: " + this.secondInCycle);
			this.initCylce();
		}
		
		if (this.forcedExtensionActive){
			this.forcedExtensionActive = this.checkForcedExtensionCondition();
			return;
		}
		else if (this.extensionActive){
//			log.debug("time: " + timeSeconds + " extension active: " +  this.currentExtensionPoint.getSecondInPlan());
			if (! this.checkExtensionCondition(timeSeconds)) {
				this.stopExtension();
			}
			return;
		}
		else {
			this.secondInSylviaCycle++;
//			log.info("time: " + timeSeconds + " sylvia timer: " + this.secondInSylviaCycle + " fixed-time timer: " + this.secondInCycle);
//			log.info("sylvia timer: " + this.secondInSylviaCycle);
			//check for forced extension trigger
			if (this.forcedExtensionPointMap.containsKey(this.secondInSylviaCycle)){
				if (this.checkForcedExtensionCondition()){
					this.forcedExtensionActive = true;
					return;
				}
			}
			//check for extension trigger
			else if (this.extensionPointMap.containsKey(this.secondInSylviaCycle)){
//				log.debug("Found Extension point at " + this.secondInSylviaCycle);
				this.currentExtensionPoint = this.extensionPointMap.get(this.secondInSylviaCycle);
				if (this.checkExtensionCondition(timeSeconds)){
					this.extensionActive = true;
					return;
				}
				else { // disable all used state variables of a extension
					this.stopExtension();
				}
			}
			//else no extension...
			List<Id> droppings = this.activeSylviaPlan.getDroppings(this.secondInSylviaCycle);
			if (droppings != null){
				for (Id groupId : droppings){
					this.system.scheduleDropping(timeSeconds, groupId);
					this.greenGroupId2OnsetMap.remove(groupId);
				}
			}
			List<Id> onsets = this.activeSylviaPlan.getOnsets(this.secondInSylviaCycle);
			if (onsets != null){
				for (Id groupId : onsets){
					this.system.scheduleOnset(timeSeconds, groupId);
					this.greenGroupId2OnsetMap.put(groupId, timeSeconds);
//					log.error("Onset of group " + groupId + " at " +timeSeconds);
				}
			}
		}
	}
	
	private void stopExtension(){
		this.extensionActive = false;
		this.currentExtensionPoint = null;
		this.lastTimeStepSensorRecordsMap = null;
	}

	
	private boolean isExtensionTimeLeft(){
		return this.extensionTime < this.activeSylviaPlan.getMaxExtensionTime();
	}
	
	private boolean isGreenTimeLeft(double timeSeconds, Id groupId, int maxGreenTime){
		int greenTime = (int) (timeSeconds - this.greenGroupId2OnsetMap.get(groupId));
		return greenTime < maxGreenTime;
	}
	
	private boolean checkForcedExtensionCondition(){
		if (this.isExtensionTimeLeft()) {
			return true;
		}
		return false;
	}
	
	private boolean checkExtensionCondition(double timeSeconds){
		if (this.isExtensionTimeLeft()) {
			//check if there is some green time left of one of the groups is over its maximal green time
			boolean greenTimeLeft = true;
//			log.error("green groups: " + this.greenGroupId2OnsetMap.size());
			for (Id signalGroupId : this.currentExtensionPoint.getSignalGroupIds()){
//				if (this.greenGroupId2OnsetMap.containsKey(signalGroupId)){ // as the group may be not green yet 
					if (! this.isGreenTimeLeft(timeSeconds, signalGroupId, this.currentExtensionPoint.getMaxGreenTime(signalGroupId))){
						greenTimeLeft = false;
					}
//				}
			}
			//if there is green time left check traffic state
			if (greenTimeLeft){
				this.monitorTrafficConditions(timeSeconds);
				return this.checkTrafficConditions(timeSeconds);
			}
		}
		return false;
	}
	
	

	
	private void monitorTrafficConditions(double timeSeconds){
		//we measure the first time in this extension
		if (this.lastTimeStepSensorRecordsMap == null) {
			this.lastTimeStepSensorRecordsMap = new HashMap<Double, SensorRecord>();
		}
		else {
			this.lastTimeStepSensorRecordsMap.remove(timeSeconds - GAP_SECONDS);
		}
		SensorRecord sensorRecord = new SensorRecord();
		this.lastTimeStepSensorRecordsMap.put(timeSeconds, sensorRecord);
		for (Id linkId : this.links4extensionPointMap.get(this.currentExtensionPoint)){
			Integer noCars = this.sensorManager.getNumberOfCarsAtDistancePerSecond(linkId, SENSOR_DISTANCE, timeSeconds);
			sensorRecord.linkIdNoCarsMap.put(linkId, noCars);
		}
		
	}
	
	private boolean checkTrafficConditions(double timeSeconds){
		//we have not enough measures to check for the specified gap
		if (this.lastTimeStepSensorRecordsMap.size() < GAP_SECONDS) {
			return true;
		}
//		log.error("extension check for traffic conditions...");
		//there are GAP_SECONDS many measures
		for (double t = timeSeconds - GAP_SECONDS + 1; t <= timeSeconds; t++){
			SensorRecord gapSensorRecord = this.lastTimeStepSensorRecordsMap.get(t);
			for (Id linkId : this.links4extensionPointMap.get(this.currentExtensionPoint)){
//				log.error("time: " + timeSeconds + " linkId: " + linkId + " gapSensorRecord: " + gapSensorRecord);
				int noCars = gapSensorRecord.linkIdNoCarsMap.get(linkId);
				if (noCars > 0){
					log.debug("  Zeitlücke unterschritten!");
					return true;
				}
			}
		}
//		log.error("Zeitlücke überschritten!");
		return false;
	}
	
	@Override
	public void addPlan(SignalPlan plan) {
		this.signalPlans .put(plan.getId(), plan);
	}

	@Override
	public void setSignalSystem(SignalSystem system) {
		this.system = system;
	}

	@Override
	public void reset(Integer iterationNumber) {
		this.init();
	}
	
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		Tuple<SignalPlan, DgSylviaSignalPlan> plans = this.searchActivePlans();
		this.activeSylviaPlan = plans.getSecond();
		this.setFixedTimeCycleInSylviaPlan(plans);
		this.setMaxExtensionTimeInSylviaPlan(plans);
		this.calculateExtensionPoints(plans);
		this.dumpExtensionPoints();
		this.initializeSensoring();
	}


	private void setMaxExtensionTimeInSylviaPlan(Tuple<SignalPlan, DgSylviaSignalPlan> plans) {
		int ext = plans.getFirst().getCycleTime() - plans.getSecond().getCycleTime();
		plans.getSecond().setMaxExtensionTime(ext);
	}

	private void setFixedTimeCycleInSylviaPlan(Tuple<SignalPlan, DgSylviaSignalPlan> plans) {
		plans.getSecond().setFixedTimeCycle(plans.getFirst().getCycleTime());
	}

	private Tuple<SignalPlan,DgSylviaSignalPlan> searchActivePlans() {
		DgSylviaSignalPlan sylviaPlan = null;
		SignalPlan fixedTimePlan = null;
		for (Id planId : this.signalPlans.keySet()){
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
			//set the extension point one second before the dropping
			Integer time = settings.getDropping() - 1 + offset;
			DgExtensionPoint extPoint = null;
			if (! this.extensionPointMap.containsKey(time)){
				extPoint = new DgExtensionPoint(time);
				this.extensionPointMap.put(time, extPoint);
				sylvia.addExtensionPoint(extPoint);
			}
			extPoint = this.extensionPointMap.get(time);
			extPoint.addSignalGroupId(settings.getSignalGroupId());

			//calculate max green time
			SignalGroupSettingsData fixedTimeSettings = ((DatabasedSignalPlan)fixedTime).getPlanData().getSignalGroupSettingsDataByGroupId().get(settings.getSignalGroupId());
			int fixedTimeGreen = DgSignalsUtils.calculateGreenTimeSeconds(fixedTimeSettings, fixedTime.getCycleTime());
			int maxGreen = (int) (fixedTimeGreen * 1.5);
			if (maxGreen >= fixedTime.getCycleTime()){
				maxGreen = fixedTimeGreen;
			}
			extPoint.setMaxGreenTime(settings.getSignalGroupId(), maxGreen);
		}
		//TODO add last extension point in cylce as a forced extension point
	}

	private void dumpExtensionPoints() {
		log.debug("Signal System: "+ this.system.getId() + " Plan: " + this.activeSylviaPlan.getPlanData().getId());
		for (DgExtensionPoint p : this.extensionPointMap.values()){
			log.debug("  ExtensionPoint at: " + p.getSecondInPlan() + " groups: ");
			for (Id sgId : p.getSignalGroupIds()){
				log.debug("    SignalGroup: " + sgId + " maxGreen: "+ p.getMaxGreenTime(sgId));
			}
		}
	}

	
	private void initializeSensoring(){
		for (DgExtensionPoint extPoint : this.extensionPointMap.values()){
			List<Id> linkIdList = new ArrayList<Id>();
			for (Id signalGroupId : extPoint.getSignalGroupIds()){
				SignalSystemData systemData = this.system.getSignalSystemsManager().getSignalsData().getSignalSystemsData().getSignalSystemData().get(this.system.getId());
				SignalGroupData signalGroup = this.system.getSignalSystemsManager().getSignalsData().getSignalGroupsData().getSignalGroupDataBySystemId(systemData.getId()).get(signalGroupId);
				Set<Id> linkIds = DgSignalsUtils.calculateSignalizedLinks4SignalGroup(systemData, signalGroup);
				linkIdList.addAll(linkIds);
			}
			for (Id linkId : linkIdList){
				this.sensorManager.registerCarsAtDistancePerSecondMonitoring(linkId, SENSOR_DISTANCE);
			}
			this.links4extensionPointMap.put(extPoint, linkIdList);
		}
	}
	
	
}
