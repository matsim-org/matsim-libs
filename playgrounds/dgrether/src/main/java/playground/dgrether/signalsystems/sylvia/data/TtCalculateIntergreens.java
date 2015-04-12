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
package playground.dgrether.signalsystems.sylvia.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesDataImpl;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreenTimesWriter10;
import org.matsim.contrib.signals.data.intergreens.v10.IntergreensForSignalSystemDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.signals.data.intergreens.v10.IntergreenTimesData;
import org.matsim.signals.data.intergreens.v10.IntergreensForSignalSystemData;
import org.matsim.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signals.data.signalcontrol.v20.SignalSystemControllerData;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.utils.DgSignalGroupSettingsDataOnsetComparator;

/**
 * 
 * @author tthunig
 * 
 */
@Deprecated
public class TtCalculateIntergreens {

	private static final Logger log = Logger
			.getLogger(TtCalculateIntergreens.class);

	int signalSystemCycleTime = 0;
	List<SignalGroupSettingsData> groupSettingsList = new ArrayList<SignalGroupSettingsData>();
	Map<Id, DgPhase> signalGroupIdToPhaseMapping = new HashMap<Id, DgPhase>();
	Map<Integer, List<SignalGroupSettingsData>> onsetSettingsMap = new HashMap<Integer, List<SignalGroupSettingsData>>();

	public void calculateIntergreens(String signalControlInputFile,
			String intergreensOutputFile) {
		log.info("Calculate intergreens of the signal control from file " + signalControlInputFile);
		
		// read fixed time control
		SignalControlData signalControl = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(signalControl);
		reader.readFile(signalControlInputFile);

		// create a container for the intergreens of all signal systems
		IntergreenTimesData intergreens = new IntergreenTimesDataImpl();

		// loop through all signal systems; calculate their intergreens
		for (SignalSystemControllerData signalSystemData : signalControl
				.getSignalSystemControllerDataBySystemId().values()) {

			// create a container for the intergreens of this signal system
			IntergreensForSignalSystemData systemIntergreens = new IntergreensForSignalSystemDataImpl(
					signalSystemData.getSignalSystemId());

			this.preprocessSystemsSignalGroups(signalSystemData);

			// loop through the signal group settings
			// calculate intergreens to signals from the same and the next phase
			for (SignalGroupSettingsData selectedSignal : groupSettingsList) {
				int phaseEnd = this.handleSamePhase(systemIntergreens,
						selectedSignal);
				this.handleNextPhase(systemIntergreens, selectedSignal,
						phaseEnd);
			}

			intergreens.addIntergreensForSignalSystem(systemIntergreens);
			log.info("intergreen calculation for signal system " + signalSystemData.getSignalSystemId() + " done");
		}
		log.info("all intergreens of the signal control calculated");

		// write the intergreens to file
		IntergreenTimesWriter10 writer = new IntergreenTimesWriter10(
				intergreens);
		writer.write(intergreensOutputFile);
	}

	private void preprocessSystemsSignalGroups(
			SignalSystemControllerData signalSystemData) {
		log.info("preprocess signal groups data for intergreens calculation");
		
		// reset system properties
		this.groupSettingsList.clear();
		this.signalGroupIdToPhaseMapping.clear();
		this.onsetSettingsMap.clear();

		for (SignalPlanData signalPlan : signalSystemData.getSignalPlanData()
				.values()) {
			groupSettingsList.addAll(signalPlan
					.getSignalGroupSettingsDataByGroupId().values());
			// assumption: all signal plans of a signal system have the same
			// cycle time
			signalSystemCycleTime = signalPlan.getCycleTime();
		}

		// delete signal groups which are green all the time
		Set<SignalGroupSettingsData> allGreenSignals = this
				.removeAllGreenSignalGroupSettings(groupSettingsList,
						signalSystemCycleTime);

		// make a copy and sort by signal group onsets
		ArrayList<SignalGroupSettingsData> sortedGroupSettingsList = new ArrayList<SignalGroupSettingsData>();
		sortedGroupSettingsList.addAll(groupSettingsList);
		Collections.sort(sortedGroupSettingsList,
				new DgSignalGroupSettingsDataOnsetComparator());

		//create structures to simplify the intergreens calculation
		this.createSameOnsetMapping();
		Map<Id, Set<SignalGroupSettingsData>> overlappingSettingsMap = this.createOverlappingSettingsMap();
		this.createPhasesForOverlappingSignals(sortedGroupSettingsList, overlappingSettingsMap);
	}

	private Set<SignalGroupSettingsData> removeAllGreenSignalGroupSettings(
			List<SignalGroupSettingsData> groupSettingsList, Integer cycleTime) {
		Set<SignalGroupSettingsData> allGreenSettings = new HashSet<SignalGroupSettingsData>();
		ListIterator<SignalGroupSettingsData> it = groupSettingsList
				.listIterator();
		while (it.hasNext()) {
			SignalGroupSettingsData settings = it.next();
			if (settings.getOnset() == 0 && settings.getDropping() == cycleTime) {
				allGreenSettings.add(settings);
				it.remove();
			}
		}
		return allGreenSettings;
	}
	
	private void createSameOnsetMapping() {
		for (SignalGroupSettingsData settings : groupSettingsList) {
			if (!onsetSettingsMap.containsKey(settings.getOnset())) {
				onsetSettingsMap.put(settings.getOnset(),
						new ArrayList<SignalGroupSettingsData>());
			}
			onsetSettingsMap.get(settings.getOnset()).add(settings);
		}
	}

	private Map<Id, Set<SignalGroupSettingsData>> createOverlappingSettingsMap() {
		Map<Id, Set<SignalGroupSettingsData>> overlappingSettingsMap = new HashMap<Id, Set<SignalGroupSettingsData>>();
		for (SignalGroupSettingsData selectedSignal : groupSettingsList) {
			overlappingSettingsMap.put(selectedSignal.getSignalGroupId(),
					new HashSet<SignalGroupSettingsData>());
			for (SignalGroupSettingsData comparativeSignal : groupSettingsList) {
				// remark: there are no overlapping signals crossing the cycle
				// time
				if (comparativeSignal.getOnset() < selectedSignal.getDropping()
						&& comparativeSignal.getDropping() > selectedSignal
								.getOnset()) {
					overlappingSettingsMap.get(
							selectedSignal.getSignalGroupId()).add(
							comparativeSignal);
				}
			}
		}
		return overlappingSettingsMap;
	}

	// fills signalGroupIdToPhaseMapping
	private void createPhasesForOverlappingSignals(ArrayList<SignalGroupSettingsData> sortedGroupSettingsList, Map<Id, Set<SignalGroupSettingsData>> overlappingSettingsMap) {
		// List<DgPhase> phases = new ArrayList<DgPhase>();
		Set<SignalGroupSettingsData> handledSignals = new HashSet<SignalGroupSettingsData>();
		// loop through the signal settings sorted by onset
		for (SignalGroupSettingsData selectedSignal : sortedGroupSettingsList) {
			if (handledSignals.contains(selectedSignal)) {
				continue;
			}
			int phaseOn = selectedSignal.getOnset();
			int phaseDrop = selectedSignal.getDropping();
			Map<Id, SignalGroupSettingsData> phaseSignals = new HashMap<Id, SignalGroupSettingsData>();
			phaseSignals.put(selectedSignal.getSignalGroupId(), selectedSignal);
			handledSignals.add(selectedSignal);
			SignalGroupSettingsData lastDroppingSignal = selectedSignal;
			// recursively add all overlapping signals to the phase
			do { // while phase dropping changes
				phaseDrop = lastDroppingSignal.getDropping();
				lastDroppingSignal = extendPhase(lastDroppingSignal,
						overlappingSettingsMap, phaseSignals, handledSignals);
			} while (phaseDrop != lastDroppingSignal.getDropping());
			// phase completed
			DgPhase phase = new DgPhase(phaseOn, phaseDrop, phaseSignals);
			// phases.add(phase);
			for (Id signalId : phaseSignals.keySet()) {
				signalGroupIdToPhaseMapping.put(signalId, phase);
			}
		}
	}

	private SignalGroupSettingsData extendPhase(
			SignalGroupSettingsData lastDroppingSignal,
			Map<Id, Set<SignalGroupSettingsData>> overlappingSettingsMap,
			Map<Id, SignalGroupSettingsData> phaseSignals,
			Set<SignalGroupSettingsData> handledSignals) {

		int phaseDrop = lastDroppingSignal.getDropping();
		SignalGroupSettingsData newLastDroppingSignal = lastDroppingSignal;

		for (SignalGroupSettingsData overlappingSignal : overlappingSettingsMap
				.get(lastDroppingSignal.getSignalGroupId())) {
			// add overlapping signal to the phase
			if (!handledSignals.contains(overlappingSignal)) {
				phaseSignals.put(overlappingSignal.getSignalGroupId(),
						overlappingSignal);
				handledSignals.add(overlappingSignal);
			}
			// look for the new last dropping signal
			if (overlappingSignal.getDropping() > phaseDrop) {
				newLastDroppingSignal = overlappingSignal;
				phaseDrop = overlappingSignal.getDropping();
			}
		}

		lastDroppingSignal = newLastDroppingSignal;
		return lastDroppingSignal;
	}

	private int handleSamePhase(
			IntergreensForSignalSystemData systemIntergreens,
			SignalGroupSettingsData selectedSignal) {

		// calculate intergreens to later signals in the same phase
		DgPhase signalPhase = signalGroupIdToPhaseMapping.get(selectedSignal
				.getSignalGroupId());
		for (SignalGroupSettingsData samePhaseSignals : signalPhase
				.getSignalGroupSettingsByGroupId().values()) {
			if (samePhaseSignals.getOnset() >= selectedSignal.getDropping()) {
				systemIntergreens.setIntergreenTime(samePhaseSignals.getOnset()
						- selectedSignal.getDropping(),
						selectedSignal.getSignalGroupId(),
						samePhaseSignals.getSignalGroupId());
			}
		}

		return signalPhase.getPhaseEndSecond();
	}

	private void handleNextPhase(
			IntergreensForSignalSystemData systemIntergreens,
			SignalGroupSettingsData selectedSignal, int phaseEnd) {

		// search for the next phase
		boolean cycleTimeCrossed = false;
		int nextPhaseOn = phaseEnd;
		while (!onsetSettingsMap.containsKey(nextPhaseOn)) {
			nextPhaseOn++;
			// cycle time is reached
			if (nextPhaseOn >= signalSystemCycleTime) {
				nextPhaseOn = 0;
				cycleTimeCrossed = true;
			}
		}

		// get an arbitrary (the first of the list) signal of the
		// signals starting at time nextPhaseOn
		SignalGroupSettingsData nextSignal = onsetSettingsMap.get(nextPhaseOn)
				.get(0);
		DgPhase nextPhase = signalGroupIdToPhaseMapping.get(nextSignal
				.getSignalGroupId());

		// calculate intergreens to all signals in the next phase
		for (SignalGroupSettingsData nextPhaseSignals : nextPhase
				.getSignalGroupSettingsByGroupId().values()) {
			int intergreen = nextPhaseSignals.getOnset()
					- selectedSignal.getDropping();
			if (cycleTimeCrossed) {
				intergreen += signalSystemCycleTime;
			}
			systemIntergreens.setIntergreenTime(intergreen,
					selectedSignal.getSignalGroupId(),
					nextPhaseSignals.getSignalGroupId());
		}
	}
	
	
	public static void main(String[] args){
		 String signalControlFile = DgPaths.REPOS +
		 "shared-svn/studies/projects/cottbus/cottbus_scenario/signal_control.xml";
		 String intergreensOutFile = DgPaths.REPOS +
		 "shared-svn/studies/projects/cottbus/cottbus_scenario/signal_control_intergreens.xml";

		new TtCalculateIntergreens().calculateIntergreens(signalControlFile,
				intergreensOutFile);
	}

}
