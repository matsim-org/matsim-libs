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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
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

	private final static double SENSOR_DISTANCE = 10.0;
	
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
		this.currentExtensionPoint = null;
		this.activeSylviaPlan = null;
		this.extensionActive = false;
		this.forcedExtensionActive = false;
		this.lastTimeStepSensorRecordsMap = null;
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
		log.info("time: " + timeSeconds + " sylvia timer: " + this.secondInSylviaCycle + " fixed-time timer: " + this.secondInCycle + " ext time: " + this.extensionTime + " of " + this.activeSylviaPlan.getMaxExtensionTime());
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
			this.extensionTime++;
//			log.debug("time: " + timeSeconds + " extension active: " +  this.currentExtensionPoint.getSecondInPlan());
			if (! this.checkExtensionCondition(timeSeconds, this.currentExtensionPoint)) {
				this.stopExtension();
			}
			return;
		}
		else {
			this.secondInSylviaCycle++;
//			log.info("time: " + timeSeconds + " sylvia timer: " + this.secondInSylviaCycle + " fixed-time timer: " + this.secondInCycle);
			log.info("sylvia timer: " + this.secondInSylviaCycle);
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
				if (this.checkExtensionCondition(timeSeconds, this.currentExtensionPoint)){
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
	
	private boolean checkExtensionCondition(double timeSeconds, DgExtensionPoint extensionPoint){
		if (this.isExtensionTimeLeft()) {
			//check if there is some green time left of one of the groups is over its maximal green time
			boolean greenTimeLeft = true;
//			log.error("green groups: " + this.greenGroupId2OnsetMap.size());
			for (Id signalGroupId : extensionPoint.getSignalGroupIds()){
//				if (this.greenGroupId2OnsetMap.containsKey(signalGroupId)){ // as the group may be not green yet 
					if (! this.isGreenTimeLeft(timeSeconds, signalGroupId, extensionPoint.getMaxGreenTime(signalGroupId))){
						greenTimeLeft = false;
					}
//				}
			}
			//if there is green time left check traffic state
			if (greenTimeLeft){
//				this.monitorTrafficConditions(timeSeconds, extensionPoint);
				return this.checkTrafficConditions(timeSeconds, extensionPoint);
			}
		}
		return false;
	}
	
	

	
	private void monitorTrafficConditions(double timeSeconds, DgExtensionPoint extensionPoint){
		//we measure the first time in this extension
		if (this.lastTimeStepSensorRecordsMap == null) {
			this.lastTimeStepSensorRecordsMap = new HashMap<Double, SensorRecord>();
		}
		else {
			this.lastTimeStepSensorRecordsMap.remove(timeSeconds - GAP_SECONDS);
		}
		SensorRecord sensorRecord = new SensorRecord();
		this.lastTimeStepSensorRecordsMap.put(timeSeconds, sensorRecord);
		for (Id linkId : this.links4extensionPointMap.get(extensionPoint)){
			Integer noCars = this.sensorManager.getNumberOfCarsAtDistancePerSecond(linkId, SENSOR_DISTANCE, timeSeconds);
			log.error("Link " + linkId + " noCarsAtSecond " + noCars + " noCarsOnLink: " + this.sensorManager.getNumberOfCarsOnLink(linkId));
			sensorRecord.linkIdNoCarsMap.put(linkId, noCars);
		}
		
	}

	private boolean checkTrafficConditions(double timeSeconds, DgExtensionPoint extensionPoint){
		log.debug("check traffic conditions:  ");
		int noCars = 0;
		for (SignalData signal : extensionPoint.getSignals()){
			if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
				noCars = this.sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), SENSOR_DISTANCE, timeSeconds);
				if (noCars > 0){
					log.debug(" Dehnung Aktiv!!");
					return true;
				}
			}
			else {
				for (Id laneId : signal.getLaneIds()){
					noCars = this.sensorManager.getNumberOfCarsOnLane(signal.getLinkId(), laneId);
					if (noCars > 0){
						log.debug(" Dehnung Aktiv!!");
						return true;
					}
				}
			}
		}		
		return false;
	}
	
	private boolean checkTrafficConditionsOld(double timeSeconds, DgExtensionPoint extensionPoint){
		//we have not enough measures to check for the specified gap
		if (this.lastTimeStepSensorRecordsMap.size() < GAP_SECONDS) {
			return true;
		}
//		log.error("extension check for traffic conditions...");
		//there are GAP_SECONDS many measures
		for (double t = timeSeconds - GAP_SECONDS + 1; t <= timeSeconds; t++){
			SensorRecord gapSensorRecord = this.lastTimeStepSensorRecordsMap.get(t);
			for (Id linkId : this.links4extensionPointMap.get(extensionPoint)){
				int noCars = gapSensorRecord.linkIdNoCarsMap.get(linkId);
				log.error("time: " + t + " linkId: " + linkId + " gapSensorRecord: " + noCars);
				if (noCars > 0){
					log.debug("  Zeitlücke unterschritten, Dehnung Aktiv!!");
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
			Set<SignalData> extPointSignals = new HashSet<SignalData>();
			for (Id signalGroupId : extPoint.getSignalGroupIds()){
				SignalSystemData systemData = this.system.getSignalSystemsManager().getSignalsData().getSignalSystemsData().getSignalSystemData().get(this.system.getId());
				SignalGroupData signalGroup = this.system.getSignalSystemsManager().getSignalsData().getSignalGroupsData().getSignalGroupDataBySystemId(systemData.getId()).get(signalGroupId);
				Set<SignalData> signals = DgSignalsUtils.getSignalDataOfSignalGroup(systemData, signalGroup);
				extPointSignals.addAll(signals);
			}
			extPoint.addSignals(extPointSignals);
			for (SignalData signal : extPointSignals){
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
					this.sensorManager.registerCarsAtDistancePerSecondMonitoring(signal.getLinkId(), SENSOR_DISTANCE);
				}
				else {
					for (Id laneId : signal.getLaneIds()){
						//TODO check this part again
						this.sensorManager.registerNumberOfCarsMonitoringOnLane(signal.getLinkId(), laneId);
					}
				}
			}
			//TODO change this
//			this.links4extensionPointMap.put(extPoint, linkIdList);
		}
	}
	
	/**
	 * Calculates link ids and lane ids that are grouped to the signal group given as argument.
	 * If a signal is directly on a link 
	 */
	private Map<Id, Set<Id>> calculateSignalizedLinkAndLaneIds4SignalGroup(SignalSystemData signalSystem, SignalGroupData signalGroup){
		Map<Id, Set<Id>> linkId2LaneIdMap = new HashMap<Id, Set<Id>>();
		if (!signalSystem.getId().equals(signalGroup.getSignalSystemId())){
			throw new IllegalArgumentException("System Id: " + signalSystem.getId() + " is not equal to signal group Id: " + signalGroup.getId());
		}
		for (Id signalId : signalGroup.getSignalIds()){
			SignalData signal = signalSystem.getSignalData().get(signalId);
			if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
					linkId2LaneIdMap.put(signal.getLinkId(), null);
			}
			else {
				linkId2LaneIdMap.put(signal.getLinkId(), signal.getLaneIds());
			}
		}
		return linkId2LaneIdMap;
	}
	
}
