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
import org.matsim.lanes.data.Lane;

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
		this.initCycle();
	}


	private void initCycle() {
		this.secondInSylviaCycle = -1; //as this is incremented before use
		// TODO ist es nicht verstaendlicher das bei 0 starten zu lassen und am ende statt am anfang hochzaehlen zu lassen?
		this.secondInCycle = -1;
		this.extensionTime = 0;
	}
	
	// TODO comment this method
	@Override
	public void updateState(double currentTime) {
//		int secondInFixedTimeCycle = (int) (timeSeconds % this.activeSylviaPlan.getFixedTimeCycle());
		if (this.secondInSylviaCycle == this.activeSylviaPlan.getCycleTime() - 1) { 
			// the base plan cycle including all extensions is processed. init data structure for the next cycle.
			this.initCycle();
			// TODO muss das nicht eher am ende der methode passieren, nachdem dropping/onset etc abgearbeitet?
		}
		this.secondInCycle++;
		
		if (this.forcedExtensionActive){
			// forced means last phase of the plan, i.e. has to be extended until the end of the cycle
			this.forcedExtensionActive = this.checkForcedExtensionCondition();
			return;
		}
		else if (this.extensionActive){
			this.extensionTime++;
			// check whether the extension should hold on
			if (! this.checkExtensionCondition(currentTime, this.currentExtensionPoint)) {
				this.stopExtension();
			}
			return;
		}
		else { // no extension is active
			// increment the number of seconds that the basic plan is processed
			this.secondInSylviaCycle++;
			//check for forced extension trigger
			if (this.forcedExtensionPointMap.containsKey(this.secondInSylviaCycle)){ // TODO this is never filled! fill it with last signal group
				if (this.checkForcedExtensionCondition()){
					this.forcedExtensionActive = true;
					return;
				}
			}
			//check for extension trigger
			else if (this.extensionPointMap.containsKey(this.secondInSylviaCycle)){
				this.currentExtensionPoint = this.extensionPointMap.get(this.secondInSylviaCycle);
				if (this.checkExtensionCondition(currentTime, this.currentExtensionPoint)){
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
					this.system.scheduleDropping(currentTime, groupId);
					this.greenGroupId2OnsetMap.remove(groupId);
				}
			}
			List<Id<SignalGroup>> onsets = this.activeSylviaPlan.getOnsets(this.secondInSylviaCycle);
			if (onsets != null){
				for (Id<SignalGroup> groupId : onsets){
					this.system.scheduleOnset(currentTime, groupId);
					this.greenGroupId2OnsetMap.put(groupId, currentTime);
				}
			}
		}
	}
	
	private void stopExtension(){
		this.extensionActive = false;
		this.currentExtensionPoint = null;
	}

	/**
	 * Check whether there is time left to extend phases.
	 * If the fixed time cycle time is used as maximal extension time, this method checks whether it is already reached.
	 * If not, extension is always allowed.
	 */
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
	
	// TODO comment
	private boolean checkForcedExtensionCondition(){
		return isExtensionTimeLeft();
	}
	
	/**
	 * Checks 
	 * 1. whether there is green time left to extend the signal groups green phase and, if yes,
	 * 2. whether there are cars arriving, i.e. there is need to extend the signal groups green time.
	 * 
	 * @param currentTime
	 * @param extensionPoint
	 * @return true, if the signal should be extended. false, if not, i.e. if there is no time left to extend the signal or if there is no need to extend the signal.
	 */
	private boolean checkExtensionCondition(double currentTime, DgExtensionPoint extensionPoint){
		if (this.isExtensionTimeLeft()) {
			//check if there is some green time left or one of the groups is over its maximal green time
			boolean greenTimeLeft = true;
			for (Id<SignalGroup> signalGroupId : extensionPoint.getSignalGroupIds()){
					if (! this.isGreenTimeLeft(currentTime, signalGroupId, extensionPoint.getMaxGreenTime(signalGroupId))){
						greenTimeLeft = false;
					}
			}
			//if there is green time left check traffic state
			if (greenTimeLeft){
				return this.checkTrafficConditions(currentTime, extensionPoint);
			}
		}
		return false;
	}
	

	/**
	 * Checks whether there are cars arriving, i.e. the signal groups green time should be extended.
	 * 
	 * @param currentTime
	 * @param extensionPoint
	 * @return true, if there is a car at an incoming lane 
	 * or, if no lanes are used, when there is a car on an incoming link within some distance (specified in sylviaConfig - default is 10)
	 */
	private boolean checkTrafficConditions(double currentTime, DgExtensionPoint extensionPoint){
		int noCars = 0;
		for (SignalData signal : extensionPoint.getSignals()){
			if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
				noCars = this.sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), this.sylviaConfig.getSensorDistanceMeter(), currentTime);
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
			Integer extensionMoment;
			if (settings.getDropping() == 0) {
				extensionMoment = sylvia.getCycleTime() - 1; // TODO + offset is missing
			}
			else {
				//set the extension point one second before the dropping
				extensionMoment = settings.getDropping() - 1 + offset;
			}
			// TODO use this instead of the above:
//			// set the extension moment one second before the dropping, but inside the cycle time
//			extensionMoment = (settings.getDropping() - 1 + offset) % sylvia.getCycleTime();
			
			// TODO warum -1? ist es nicht besser in dropping sec ueber verlaengern nachzudenken?
			
			// put all extension points in a map ordered by time
			DgExtensionPoint extPoint = null;
			if (! this.extensionPointMap.containsKey(extensionMoment)){
				extPoint = new DgExtensionPoint(extensionMoment);
				this.extensionPointMap.put(extensionMoment, extPoint);
				sylvia.addExtensionPoint(extPoint); // TODO why is this needed?
			}
			extPoint = this.extensionPointMap.get(extensionMoment);
			extPoint.addSignalGroupId(settings.getSignalGroupId());

			//calculate max green time
			SignalGroupSettingsData fixedTimeSettings = ((DatabasedSignalPlan)fixedTime).getPlanData().getSignalGroupSettingsDataByGroupId().get(settings.getSignalGroupId());
			int fixedTimeGreen = DgSignalsUtils.calculateGreenTimeSeconds(fixedTimeSettings, fixedTime.getCycleTime());
			int maxGreen = (int) (fixedTimeGreen * this.sylviaConfig.getSignalGroupMaxGreenScale());
			if (maxGreen >= fixedTime.getCycleTime()){
				maxGreen = fixedTimeGreen; // TODO warum nicht fixedTime.getCycleTime() oder cycle time - sylvia time der remaining groups... !?
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
