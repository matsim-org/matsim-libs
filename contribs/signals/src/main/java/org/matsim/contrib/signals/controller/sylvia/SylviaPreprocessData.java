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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
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

	public static void simplifySignalGroupsAndConvertFixedTimePlansToSylviaBasePlans(String signalControlInputFile, String singalControlOutputFile, String signalGroupsFile, String signalGroupsOutFile)
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
		writer.write(singalControlOutputFile);

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
//			log.debug("");
//			log.debug("system: " + controllerData.getSignalSystemId());
			for (SignalPlanData signalPlan : controllerData.getSignalPlanData().values()) {
				// add a copy of the old plan
				newControllerData.addSignalPlanData(SignalUtils.copySignalPlanData(signalPlan, 
						Id.create(FIXED_TIME_PREFIX + signalPlan.getId().toString(), SignalPlan.class)));
				newControllerData.addSignalPlanData(convertSignalPlanData(signalPlan, sylviaSignalControlData.getFactory()));
			}
		}
	}
	
	
	private static FixedTimeSignalPhase createSylviaPhase(final FixedTimeSignalPhase phase, final SignalControlDataFactory factory){
		log.info("creating sylvia phase...");
		final int on = phase.getPhaseStartSecond();
		List<SignalGroupSettingsData> alltimeGreenSettings = new ArrayList<>();
		List<SignalGroupSettingsData> shorterSettingsSortedByOnset  = new ArrayList<>();
		List<SignalGroupSettingsData> newSettings = new ArrayList<>();
		//get all group settings that are shorter than the phase
		for (SignalGroupSettingsData settings : phase.getSignalGroupSettingsByGroupId().values()){
			if (settings.getOnset() == phase.getPhaseStartSecond() && settings.getDropping() == phase.getPhaseEndSecond()){
				alltimeGreenSettings.add(settings);
			}
			else {
				shorterSettingsSortedByOnset.add(settings);
			}
		}
		
		//shorten the settings that are shorter than the phase
		Collections.sort(shorterSettingsSortedByOnset, new SignalGroupSettingsOnsetComparator());
		int currentShortOn = on;
		int currentOn = on;
//		Integer currentOff = null;
		int currentShortOff = on + MIN_GREEN_SECONDS;
		for (SignalGroupSettingsData settings : shorterSettingsSortedByOnset) {
			SignalGroupSettingsData shortSettings = SignalUtils.copySignalGroupSettingsData(settings, factory);
			if (settings.getOnset() != currentOn){
				currentOn = settings.getOnset();
				currentShortOn = currentShortOn + MIN_GREEN_SECONDS;
				currentShortOff = currentShortOn + MIN_GREEN_SECONDS;
			}
			shortSettings.setOnset(currentShortOn);
			shortSettings.setDropping(currentShortOff);
			newSettings.add(shortSettings);
		}

		//now there is a phase length, i.e.:
		int off;
		//only short settings at the beginning
		if (currentShortOn == on && shorterSettingsSortedByOnset.size() > 0) {
			off = currentShortOff + MIN_GREEN_SECONDS;
		}
		else {
			off = currentShortOff;
		}
		FixedTimeSignalPhase newPhase = new FixedTimeSignalPhase(on, off);
		//create all time green settings
		for (SignalGroupSettingsData settings : alltimeGreenSettings){
			SignalGroupSettingsData shortSettings = SignalUtils.copySignalGroupSettingsData(settings, factory);
			shortSettings.setOnset(on);
			shortSettings.setDropping(off);
			newSettings.add(shortSettings);
		}
		//add the new settings to the new phase
		for (SignalGroupSettingsData settings : newSettings){
			newPhase.addSignalGroupSettingsData(settings);
		}
		return newPhase;
	}
	
	private static SignalPlanData convertSignalPlanData(final SignalPlanData fixedTimePlan, SignalControlDataFactory factory) {
		SignalPlanData newPlan = SignalUtils.copySignalPlanData(fixedTimePlan, Id.create(SYLVIA_PREFIX + fixedTimePlan.getId().toString(), SignalPlan.class));
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
		return newPlan;
	}
	
	/**
	 * calculates the time that should be between two phases that overlap in time
	 * @return 
	 */
	private static Collection<IntergreenConstraint> calculateIntergreenConstraints(FixedTimeSignalPhase lastPhase, FixedTimeSignalPhase phase) {
		Map<SignalGroupSettingsData, IntergreenConstraint> map = new HashMap<>();
		for (SignalGroupSettingsData settings : phase.getSignalGroupSettingsByGroupId().values()){
			for (SignalGroupSettingsData lastSettings : lastPhase.getSignalGroupSettingsByGroupId().values()){
				int intergreen = settings.getOnset() - lastSettings.getDropping();
				log.info("intergreen: " + intergreen);
				if (intergreen >= 0){
					if ((! map.containsKey(settings)) || map.get(settings).intergreen > intergreen){
						IntergreenConstraint ic = new IntergreenConstraint();
						ic.onSettingsId = settings.getSignalGroupId();
						ic.droppingSettingsId = lastSettings.getSignalGroupId();
						ic.intergreen = intergreen;
						map.put(settings, ic);
					}
				}
			}
		}
		return map.values();
	}
		
	

	private static List<SignalGroupSettingsData> calculateSettingsShorterThanPhase(FixedTimeSignalPhase phase){
		List<SignalGroupSettingsData> settingsList  = new ArrayList<>();
		//get all group settings that are shorter than the phase
		for (SignalGroupSettingsData settings : phase.getSignalGroupSettingsByGroupId().values()){
			if (settings.getOnset() == phase.getPhaseStartSecond() && settings.getDropping() == phase.getPhaseEndSecond()){
				continue;
			}
			settingsList.add(settings);
		}
		return settingsList;
	}
	

	private static void addPhaseToPlan(FixedTimeSignalPhase p, SignalPlanData newPlan){
		for (SignalGroupSettingsData settings : p.getSignalGroupSettingsByGroupId().values()){
			newPlan.addSignalGroupSettings(settings);
		}
	}

	/**
	 * Calculates phases sorted by start time
	 * A signal group is considered to be member of a phase if it
	 *   - starts together with others at a time t
	 *   - starts after t but ends at the same time as the groups starting at t
	 */
	private static List<FixedTimeSignalPhase> calculateSortedPhases(final List<SignalGroupSettingsData> groupSettingsList) {
		List<FixedTimeSignalPhase> phases = new ArrayList<>();
		//make a copy
		ArrayList<SignalGroupSettingsData> settingsList = new ArrayList<>();
		settingsList.addAll(groupSettingsList);
		//sort the copy
		Collections.sort(settingsList, new SignalGroupSettingsOnsetComparator());
		//preprocess
		Map<Integer, Set<SignalGroupSettingsData>> onsetSettingsMap = new HashMap<>();
		Map<Integer, Set<SignalGroupSettingsData>> droppingSettingsMap = new HashMap<>();
		for (SignalGroupSettingsData settings : groupSettingsList){
			if (!onsetSettingsMap.containsKey(settings.getOnset())){
				onsetSettingsMap.put(settings.getOnset(), new HashSet<>());
			}
			onsetSettingsMap.get(settings.getOnset()).add(settings);
			if (!droppingSettingsMap.containsKey(settings.getDropping())){
				droppingSettingsMap.put(settings.getDropping(), new HashSet<>());
			}
			droppingSettingsMap.get(settings.getDropping()).add(settings);
		}
		
		//create the phases
		Set<SignalGroupSettingsData> handledSettings = new HashSet<>();
		//loop through the settings sorted by onset
		for (SignalGroupSettingsData settings : settingsList){
			if (handledSettings.contains(settings)){
				continue;
			}
			Set<SignalGroupSettingsData> sameOnsetSettings = onsetSettingsMap.get(settings.getOnset());
			SignalGroupSettingsData lastDropSettings = getLastDroppingSettings(sameOnsetSettings); 
			int phaseOn = lastDropSettings.getOnset();
			int phaseDrop = lastDropSettings.getDropping();
			FixedTimeSignalPhase phase = new FixedTimeSignalPhase(phaseOn, phaseDrop);
			phases.add(phase);
			Set<SignalGroupSettingsData> sameDroppingSettings = droppingSettingsMap.get(phaseDrop);
			//check semantics and add to phase
			for (SignalGroupSettingsData sameOnsetSetting : sameOnsetSettings){
				if (sameOnsetSetting.getDropping() <= phaseDrop){
					handledSettings.add(sameOnsetSetting);
					phase.addSignalGroupSettingsData(sameOnsetSetting);
				}
				else {
					log.error("should not happen");
				}
			}
			for (SignalGroupSettingsData sameDropSetting : sameDroppingSettings){
				if (handledSettings.contains(sameDropSetting)){
					continue;
				}
				if (sameDropSetting.getOnset() >= phaseOn){
					handledSettings.add(sameDropSetting);
					phase.addSignalGroupSettingsData(sameDropSetting);
				}
				else {
					log.error("should not happen");
				}
			}
		}
		return phases;
	}
	


	private static SignalGroupSettingsData getLastDroppingSettings(Set<SignalGroupSettingsData> sameOnsetSettings) {
		SignalGroupSettingsData lastDropSettings = null;
		for (SignalGroupSettingsData settings : sameOnsetSettings){
			if (settings.getOnset() > settings.getDropping()){
				throw new IllegalStateException("onset > dropping not implemented yet!");
			}
			if (lastDropSettings == null || settings.getDropping() >= lastDropSettings.getDropping()){
				lastDropSettings = settings;
			}
		}
		return lastDropSettings;
	}



	private static Set<SignalGroupSettingsData> removeAllGreenSignalGroupSettings(List<SignalGroupSettingsData> groupSettingsList, Integer cycleTime){
		Set<SignalGroupSettingsData> allGreenSettings = new HashSet<>();
		ListIterator<SignalGroupSettingsData> it = groupSettingsList.listIterator();
		while (it.hasNext()){
			SignalGroupSettingsData settings = it.next();
			if (settings.getOnset() == 0 && settings.getDropping() == cycleTime){
				allGreenSettings.add(settings);
				it.remove();
			}
		}
		return allGreenSettings;
	}
	
	public static void main(String[] args) throws JAXBException, SAXException, ParserConfigurationException, IOException {
		String signalControlFile = "../../../shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_control_no_13_random_offsets.xml";
		String signalControlOutFile = "../../../shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_control_sylvia_no_13_random_offsets.xml";
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