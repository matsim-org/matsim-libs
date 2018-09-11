/* *********************************************************************** *
 * project: org.matsim.*
 * DgGenerateBasePlans
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
package org.matsim.contrib.signals.controller.sylvia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsDataImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsReader20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.xml.sax.SAXException;


/**
 * If I would have to write this again I would start with calculation of the IntergreenConstraints in this class and 
 * than follow a forward-propagation of constraints. but currently it is working fine!
 * @author dgrether
 *
 */
public final class SylviaPreprocessData {

	private static final Logger log = Logger.getLogger(SylviaPreprocessData.class);

	public static final String FIXED_TIME_PREFIX = "fixed_time_plan_";

	public static final String SYLVIA_PREFIX = "sylvia_plan_";

	private static final int MIN_GREEN_SECONDS = 5; //see RILSA pp. 28

	public static void simplifySignalGroupsAndConvertFixedTimePlansToSylviaBasePlans(String signalControlInputFile, String signalControlOutputFile, String signalGroupsFile, String signalGroupsOutFile)
			throws JAXBException, SAXException, ParserConfigurationException, IOException {
		SignalControlData signalControl = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(signalControl);
		reader.readFile(signalControlInputFile);

		SignalGroupsData signalGroupsData = new SignalGroupsDataImpl();
		SignalGroupsReader20 groupsReader = new SignalGroupsReader20(signalGroupsData);
		groupsReader.readFile(signalGroupsFile);

		convertSignalGroupsData(signalGroupsData, signalControl);

		SignalControlData sylviaSignalControl = new SignalControlDataImpl();
		convertSignalControlData(signalControl, sylviaSignalControl);

		SignalControlWriter20 writer = new SignalControlWriter20(sylviaSignalControl);
		writer.write(signalControlOutputFile);

		SignalGroupsWriter20 groupsWriter = new SignalGroupsWriter20(signalGroupsData);
		groupsWriter.write(signalGroupsOutFile);

	}
	
	/**
	 * Merge all signalGroups that have equal onsets and droppings to one SignalGroup, also change the SignalControlData accordingly
	 */
	private static SignalGroupsData convertSignalGroupsData(SignalGroupsData signalGroupsData, SignalControlData signalControl) {
		for (SignalSystemControllerData  controllerData : signalControl.getSignalSystemControllerDataBySystemId().values()){
			if (controllerData.getSignalPlanData().size() > 1){
				log.warn("More than one plan, check if this tool is doing the correct work!");
			}
			for (SignalPlanData signalPlan : controllerData.getSignalPlanData().values()){
				Map<Integer, List<SignalGroupSettingsData>> onsetGroupSettingsMap = new HashMap<>();
				for (SignalGroupSettingsData signalGroupSettings : signalPlan.getSignalGroupSettingsDataByGroupId().values()){
					if (!onsetGroupSettingsMap.containsKey(signalGroupSettings.getOnset())){
						onsetGroupSettingsMap.put(signalGroupSettings.getOnset(), new ArrayList<>());
					}
					onsetGroupSettingsMap.get(signalGroupSettings.getOnset()).add(signalGroupSettings);
				}
			}
		}
		return null;
	}
	
	
	private static void convertFixedTimePlansToSylviaBasePlans(String signalControlInputFile, String signalControlOutputFile)
			throws JAXBException, SAXException, ParserConfigurationException, IOException{
		SignalControlData signalControl = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(signalControl);
		reader.readFile(signalControlInputFile);

		SignalControlDataImpl sylviaSignalControl = new SignalControlDataImpl();
		convertSignalControlData(signalControl, sylviaSignalControl);

		SignalControlWriter20 writer = new SignalControlWriter20(sylviaSignalControl);
		writer.write(signalControlOutputFile);
	}

	/**
	 * Convert old, fixed time signal control data into new sylvia signal control data.
	 * The sylvia signal control data will then contain the old fixed time signal control plan and the new shortened sylvia signal control plan.
	 * 
	 * @param controlData old, fixed-time signal control
	 * @param sylviaSignalControlData signal control object, where the sylvia signal control should be stored in
	 */
	public static void convertSignalControlData(final SignalControlData controlData, SignalControlData sylviaSignalControlData){
		for (SignalSystemControllerData  controllerData: controlData.getSignalSystemControllerDataBySystemId().values()){
			SignalSystemControllerData newControllerData = sylviaSignalControlData.getFactory().createSignalSystemControllerData(controllerData.getSignalSystemId());
			sylviaSignalControlData.addSignalSystemControllerData(newControllerData);
			newControllerData.setControllerIdentifier(SylviaSignalController.IDENTIFIER);
			for (SignalPlanData signalPlan : controllerData.getSignalPlanData().values()) {
				// add a copy of the old plan
				newControllerData.addSignalPlanData(SignalUtils.copySignalPlanData(signalPlan, 
						Id.create(FIXED_TIME_PREFIX + signalPlan.getId().toString(), SignalPlan.class)));
				newControllerData.addSignalPlanData(convertSignalPlanData(signalPlan));
			}
		}
	}	
	
	private static SignalPlanData convertSignalPlanData(final SignalPlanData fixedTimePlan) {
		SignalPlanData newPlan = SignalUtils.copySignalPlanData(fixedTimePlan, Id.create(SYLVIA_PREFIX + fixedTimePlan.getId().toString(), SignalPlan.class));
		// shift plan such that longest green time starts at second zero
		shiftPlan(newPlan);
		/* create sorted list by onset and dropping (decreasing) */
		List<SignalGroupSettingsData> settingsSortedByDropping = new ArrayList<>();
		settingsSortedByDropping.addAll(newPlan.getSignalGroupSettingsDataByGroupId().values());
		Collections.sort(settingsSortedByDropping, new Comparator<SignalGroupSettingsData>() {

			@Override
			public int compare(SignalGroupSettingsData setting1, SignalGroupSettingsData setting2) {
				// shift 1 and 2 to get reverse order
				return Integer.compare(setting2.getDropping(), setting1.getDropping());
			}
		});
		List<SignalGroupSettingsData> settingsSortedByOnset = new ArrayList<>();
		settingsSortedByOnset.addAll(newPlan.getSignalGroupSettingsDataByGroupId().values());
		Collections.sort(settingsSortedByOnset, new Comparator<SignalGroupSettingsData>() {

			@Override
			public int compare(SignalGroupSettingsData setting1, SignalGroupSettingsData setting2) {
				// shift 1 and 2 to get reverse order
				return Integer.compare(setting2.getOnset(), setting1.getOnset());
			}
		});
		/* scan cycle time for time periods before droppings, where no signal setting is changed and shrink them to MIN_GREEN_SECONDS seconds */
		Iterator<SignalGroupSettingsData> droppingIterator = settingsSortedByDropping.iterator();
		Iterator<SignalGroupSettingsData> onsetIterator = settingsSortedByOnset.iterator();
		SignalGroupSettingsData thisDropping = droppingIterator.next();
		SignalGroupSettingsData nextDropping = droppingIterator.hasNext()? droppingIterator.next() : null;
		while (nextDropping != null) {
			// look for the next dropping pair with a minimum of MIN_GREEN_SECONDS seconds inbetween
			while (nextDropping != null && thisDropping.getDropping() - nextDropping.getDropping() <= MIN_GREEN_SECONDS) {
				// between these two droppings we can not shrink the signal plan. check the next pair
				thisDropping = nextDropping;
				nextDropping = droppingIterator.hasNext()? droppingIterator.next() : null;
			}
			// look for the next onset that is before the current dropping (note: there always is one, because no dropping is scheduled at second 0 but at least one offset is scheduled for second 0)
			SignalGroupSettingsData nextOnset = onsetIterator.next();
			while (nextOnset.getOnset() >= thisDropping.getDropping()) {
				nextOnset = onsetIterator.next();
			}
			if (thisDropping.getDropping() - nextOnset.getOnset() <= MIN_GREEN_SECONDS) {
				// we can not shrink the signal plan before the current dropping
				thisDropping = nextDropping;
				continue;
			}
			// we can shrink the signal plan
			int shrinkStart = Math.max(nextOnset.getOnset(), nextDropping==null? 0 : nextDropping.getDropping()) + MIN_GREEN_SECONDS;
			int shrinkEnd = thisDropping.getDropping();
			// shift all settings behind shrinkStart by shrinkEnd - shrinkStart seconds to
			// the left
			log.info("Shrink plan " + newPlan.getId() + " by " + (shrinkEnd - shrinkStart)
					+ " seconds at dropping of signal group " + thisDropping.getSignalGroupId()
					+ ". Shift all settings behind " + shrinkStart + " accordingly. The next dropping is signal group "
					+ (nextDropping == null ? "null" : nextDropping.getSignalGroupId() )
					+ ". The next onset is signal group " + nextOnset.getSignalGroupId());
			shrinkSignalPlan(newPlan, shrinkStart, shrinkEnd - shrinkStart);
			thisDropping = nextDropping;
		}
		return newPlan;
		
		/* alt:
		List<SignalGroupSettingsData> groupSettingsList = new ArrayList<>();
		groupSettingsList.addAll(newPlan.getSignalGroupSettingsDataByGroupId().values());
		//filter allGreenSettings
		Set<SignalGroupSettingsData> allGreenSettings = removeAllGreenSignalGroupSettings(groupSettingsList, fixedTimePlan.getCycleTime());
		List<FixedTimeSignalPhase> phases = calculateSortedPhases(groupSettingsList);
		List<FixedTimeSignalPhase> sylviaPhases = new ArrayList<>();
		
		int phaseStart  = 0;
		int lastPhaseOff = 0;
		for (int i = 0; i < phases.size(); i++){
			System.out.println();
			log.info("Processing phase: " + (i+1) + " of " + phases.size());
			final FixedTimeSignalPhase phase = phases.get(i); // this phase is not modified
			FixedTimeSignalPhase sylviaPhase = createSylviaPhase(phase, factory);
			if (i == 0){ //this should be the first phase of the cylce
				phaseStart = phase.getPhaseStartSecond();
			}
			else {
				FixedTimeSignalPhase lastPhase = phases.get(i - 1);
				FixedTimeSignalPhase lastSylviaPhase = sylviaPhases.get(i - 1);
				log.info("last phase end: " + lastSylviaPhase.getPhaseEndSecond());
				int intergreen = phase.getPhaseStartSecond() - lastPhase.getPhaseEndSecond();
				log.info("intergreen: " + intergreen + " due to phase start at " + phase.getPhaseStartSecond() + " last phase end: " + lastPhase.getPhaseEndSecond());
				if (intergreen >= 0){ //phases are not overlapping
					log.info("phase not overlapping...");
					phaseStart = lastSylviaPhase.getPhaseEndSecond() + intergreen;
					int shift = sylviaPhase.getPhaseStartSecond() - phaseStart;
					log.info("shift of not overlapping phase : " + shift);
					sylviaPhase.setPhaseStartSecond(sylviaPhase.getPhaseStartSecond() - shift);
					sylviaPhase.setPhaseEndSecond(sylviaPhase.getPhaseEndSecond() - shift);
					for (SignalGroupSettingsData settings : sylviaPhase.getSignalGroupSettingsByGroupId().values()){
						settings.setOnset(settings.getOnset() - shift);
						settings.setDropping(settings.getDropping() - shift);
						log.info("  ...modified settings of phase: " + settings.getSignalGroupId() + " on:  " + settings.getOnset() + " off: " + settings.getDropping());
					}
				}
				else { //handle overlapping phases -> shift phase 
					log.info("phases overlap...");
					Collection<IntergreenConstraint> intergreenConstraints = calculateIntergreenConstraints(lastPhase, phase);
					log.info("intergreen constraints: " + intergreenConstraints);
					if (! intergreenConstraints.isEmpty()){
						int phaseOn = sylviaPhase.getPhaseEndSecond(); //initialize with maximal value
						int phaseOff = 0;
						for (IntergreenConstraint ic : intergreenConstraints){
							SignalGroupSettingsData sylviaSettings = sylviaPhase.getSignalGroupSettingsByGroupId().get(ic.onSettingsId);
							SignalGroupSettingsData lastSylviaSettings = lastSylviaPhase.getSignalGroupSettingsByGroupId().get(ic.droppingSettingsId);
							int greenTime = sylviaSettings.getDropping() - sylviaSettings.getOnset();
							int on = lastSylviaSettings.getDropping() + ic.intergreen;
							int off = on + greenTime;
							log.info("settings " + sylviaSettings.getSignalGroupId() + " green time : " + greenTime + " intergreen constraint: " + ic.intergreen + " to group id " + ic.droppingSettingsId + " on " + on + " off " + off);
							sylviaSettings.setOnset(on);
							sylviaSettings.setDropping(off);
							if (on < phaseOn)
								phaseOn = on;
							if (off > phaseOff)
								phaseOff = off;
						}
						log.info("shiftet phase to " + phaseOn + " - " + phaseOff);
						sylviaPhase.setPhaseStartSecond(phaseOn);
						sylviaPhase.setPhaseEndSecond(phaseOff);
						for (SignalGroupSettingsData settings : sylviaPhase.getSignalGroupSettingsByGroupId().values()){
							settings.setDropping(sylviaPhase.getPhaseEndSecond());
						}
					}
					else { //phases overlap but there are no positive intergreen constaints -> all groups start meanwhile last phase is active and end afterwards
						//do nothing
					}
				}
			}
			lastPhaseOff = phase.getPhaseEndSecond();
			sylviaPhases.add(sylviaPhase);
		}

		int lastIntergreen = fixedTimePlan.getCycleTime() - lastPhaseOff;
		int lastSylviaPhaseOff = 0;
		newPlan.getSignalGroupSettingsDataByGroupId().clear();
		for (FixedTimeSignalPhase p : sylviaPhases){
			addPhaseToPlan(p, newPlan);
			lastSylviaPhaseOff = p.getPhaseEndSecond();
		}
		int sylviaCycle = lastSylviaPhaseOff + lastIntergreen;
		newPlan.setCycleTime(sylviaCycle);

		for (SignalGroupSettingsData settings : allGreenSettings){
			settings.setOnset(0);
			settings.setDropping(sylviaCycle);
			newPlan.addSignalGroupSettings(settings);
		}
		return newPlan; */
	}
	
	/**
	 * shift plan such that longest green time starts at second zero. adapt offset accordingly. check that no dropping happens at second 0 but cycleTime
	 */
	private static SignalPlanData shiftPlan(SignalPlanData signalPlan) {
		// identify longest green time
		int longestGreenTime = 0;
		Id<SignalGroup> longestGreenGroup = null;
		for (SignalGroupSettingsData setting : signalPlan.getSignalGroupSettingsDataByGroupId().values()) {
			int greenTime = setting.getDropping() - setting.getOnset();
			if (greenTime <= 0) greenTime += signalPlan.getCycleTime();
			
			if (greenTime > longestGreenTime) {
				longestGreenTime = greenTime;
				longestGreenGroup = setting.getSignalGroupId();
			}
		}
		// shift all settings and offset accordingly
		int shiftBy = signalPlan.getSignalGroupSettingsDataByGroupId().get(longestGreenGroup).getOnset();
		log.info("The setting with the longest green time is signal group " + longestGreenGroup + " with a green time of " + longestGreenTime + " seconds. Shift start to second " + shiftBy);
		if (shiftBy == 0) {
			return signalPlan;
		}
		signalPlan.setOffset((signalPlan.getOffset() - shiftBy + signalPlan.getCycleTime()) % signalPlan.getCycleTime());
		for (SignalGroupSettingsData setting : signalPlan.getSignalGroupSettingsDataByGroupId().values()) {
			// set new onset in interval [0,cycleTime-1]
			int shiftedOnset = setting.getOnset() - shiftBy;
			if (shiftedOnset < 0) shiftedOnset += signalPlan.getCycleTime();
			setting.setOnset(shiftedOnset);
			// set new dropping in interval [1,cycleTime]
			int shiftedDropping = setting.getDropping() - shiftBy;
			if (shiftedDropping <= 0) shiftedDropping += signalPlan.getCycleTime();
			setting.setDropping(shiftedDropping);
		}
		return signalPlan;
	}

	/**
	 * move all droppings and onsets behind shrinkStart by secondsToBeRemoved
	 * seconds to the left, i.e. shrink the whole signal plan by secondsToBeRemoved
	 * seconds beginning at second shrinkStart.
	 */
	private static void shrinkSignalPlan(SignalPlanData signalPlan, int shrinkStart, int secondsToBeRemoved) {
		signalPlan.setCycleTime(signalPlan.getCycleTime() - secondsToBeRemoved);
		for (SignalGroupSettingsData setting : signalPlan.getSignalGroupSettingsDataByGroupId().values()) {
			if (setting.getOnset() > shrinkStart) {
				setting.setOnset(setting.getOnset() - secondsToBeRemoved);
			}
			if (setting.getDropping() > shrinkStart) {
				setting.setDropping(setting.getDropping() - secondsToBeRemoved);
			}
		}
	}
	
	
	public static void main(String[] args) throws JAXBException, SAXException, ParserConfigurationException, IOException {
		String signalControlFile = args[0];
		String signalControlOutFile = args[1];
		SylviaPreprocessData.convertFixedTimePlansToSylviaBasePlans(signalControlFile, signalControlOutFile);
	}
}

class IntergreenConstraint {
	Id<SignalGroup> droppingSettingsId;
	Id<SignalGroup> onSettingsId;
	Integer intergreen;
}

class SignalGroupSettingsOnsetComparator implements Comparator<SignalGroupSettingsData> {
	@Override
	public int compare(SignalGroupSettingsData o1, SignalGroupSettingsData o2) {
		return Integer.valueOf(o1.getOnset()).compareTo(Integer.valueOf(o2.getOnset()));
	}
}