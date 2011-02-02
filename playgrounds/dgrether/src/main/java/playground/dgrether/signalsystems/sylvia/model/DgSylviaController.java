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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.model.DatabasedSignalPlan;
import org.matsim.signalsystems.model.SignalController;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.sylvia.data.DgSylviaPreprocessData;


/**
 * @author dgrether
 *
 */
public class DgSylviaController implements SignalController {

	private static final Logger log = Logger.getLogger(DgSylviaController.class);
	
	public final static String CONTROLLER_IDENTIFIER = "SylviaSignalControl";

	private final static double SENSOR_DISTANCE = 30.0;
	
	private final static double GAP_SECONDS = 5.0;
	
	private SignalSystem system;
	private Map<Id, SignalPlan> signalPlans = new HashMap<Id, SignalPlan>();
	
	private DgSylviaSignalPlan activeSylviaPlan = null;
	private boolean extensionActive = false;
	private boolean forcedExtensionActive = false;
	private int secondInFixedTimeCycle = 0;
	private int secondInSylviaCycle = 0;
	private Map<Integer, DgExtensionPoint> extensionPointMap;
	private Map<Integer, Object> forcedExtensionPointMap;
	private Map<Id, Double> greenGroupId2OnsetMap = new HashMap<Id, Double>();
	private int extensionTime = 0;

	private DgExtensionPoint currentExtensionPoint;

	private DgSensorManager sensorManager;

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
	}

	private void initCylce(){
		this.secondInSylviaCycle = 0; //TODO check if 0 or -1
		this.extensionTime = 0;
	}
	
	@Override
	public void updateState(double timeSeconds) {
		this.secondInFixedTimeCycle = (int) (timeSeconds % this.activeSylviaPlan.getFixedTimeCycle());
		if (this.secondInFixedTimeCycle == 0){
			this.initCylce();
		}
		if (this.forcedExtensionActive){
			this.forcedExtensionActive = this.checkForcedExtensionCondition();
			return;
		}
		else if (this.extensionActive){
			this.extensionActive = this.checkExtensionCondition(timeSeconds);
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
				if (this.checkExtensionCondition(timeSeconds)){
					this.extensionActive = true;
					this.currentExtensionPoint = this.extensionPointMap.get(this.secondInSylviaCycle);
					return;
				}
			}
			//else no extension...
			List<Id> droppings = this.activeSylviaPlan.getDroppings(this.secondInSylviaCycle);
			for (Id groupId : droppings){
				this.system.scheduleDropping(timeSeconds, groupId);
				this.greenGroupId2OnsetMap.remove(groupId);
			}
			List<Id> onsets = this.activeSylviaPlan.getOnsets(secondInSylviaCycle);
			for (Id groupId : onsets){
				this.system.scheduleOnset(timeSeconds, groupId);
				this.greenGroupId2OnsetMap.put(groupId, timeSeconds);
			}
		}
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
			for (Id signalGroupId : this.currentExtensionPoint.getSignalGroupIds()){
				if (! this.isGreenTimeLeft(timeSeconds, signalGroupId, this.currentExtensionPoint.getMaxGreenTime(signalGroupId))){
					greenTimeLeft = false;
				}
			}
			//if there is green time left check traffic state
			if (greenTimeLeft){
				this.monitorTrafficConditions(timeSeconds);
				return this.checkTrafficConditions(timeSeconds);
			}
		}
		this.lastTimeStepSensorRecordsMap = null;
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
	
	//TODO check arithmetik concerning number of mearures and GAP_SECONDS
	private boolean checkTrafficConditions(double timeSeconds){
		//we have not enough measures to check for the specified gap
		if (this.lastTimeStepSensorRecordsMap.size() < GAP_SECONDS) {
			return true;
		}
		//there are GAP_SECONDS many measures
		for (double t = timeSeconds - GAP_SECONDS; t < timeSeconds; t++){
			SensorRecord gapSensorRecord = this.lastTimeStepSensorRecordsMap.get(t);
			for (Id linkId : this.links4extensionPointMap.get(this.currentExtensionPoint)){
				int noCars = gapSensorRecord.linkIdNoCarsMap.get(linkId);
				if (noCars > 0){
					return true;
				}
			}
		}
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
		this.lastTimeStepSensorRecordsMap = null;
		//TODO is there any datastructure to reset? is the sensor manager reset?
	}

	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		Tuple<SignalPlan, DgSylviaSignalPlan> plans = this.searchActivePlans();
		this.setFixedTimeCycleInSylviaPlan(plans);
		this.setMaxExtensionTimeInSylviaPlan(plans);
		this.calculateExtensionPoints(plans);
		this.activeSylviaPlan = plans.getSecond();
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
		for (SignalGroupSettingsData settings : sylvia.getPlanData().getSignalGroupSettingsDataByGroupId().values()){
			SignalGroupSettingsData fixedTimeSettings = ((DatabasedSignalPlan)fixedTime).getPlanData().getSignalGroupSettingsDataByGroupId().get(settings.getSignalGroupId());
			int on = fixedTimeSettings.getOnset();
			int off = fixedTimeSettings.getDropping();
			int green = 0;
			if (on < off){
				green = off - on;
			}
			else {
				green = off + fixedTime.getCycleTime() - on;
			}
			//we check one second before the dropping
			DgExtensionPoint extPoint = new DgExtensionPoint(settings.getDropping() - 1);
			sylvia.addExtensionPoint(extPoint);
			this.extensionPointMap.put(extPoint.getSecondInPlan(), extPoint);
			//max green time
			int maxGreen = (int) (green * 1.5);
			if (maxGreen >= fixedTime.getCycleTime()){
				maxGreen = green;
			}
			extPoint.setMaxGreenTime(settings.getSignalGroupId(), maxGreen);
			extPoint.getSignalGroupIds().add(settings.getSignalGroupId());
			//TODO what's about the phasing??? can get confused
			//TODO here the sensors should be initialized and links4extensionPointMap too
			
			
		}
	}

	
}
