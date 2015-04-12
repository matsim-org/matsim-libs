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
 * Class to calculate the intergreen times from a (fixed) signal control plan
 * and write them into a xml file.
 * 
 * Intergreens are necessary to create and check (adaptive) signal control
 * plans.
 * 
 * @author tthunig
 * 
 */
public class TtCalculateSimplifiedIntergreens {

	private static final Logger log = Logger
			.getLogger(TtCalculateSimplifiedIntergreens.class);

	int signalSystemCycleTime = 0;
	List<SignalGroupSettingsData> groupSettingsList = new ArrayList<SignalGroupSettingsData>();
	Map<Id, DgPhase> signalGroupIdToPhaseMapping = new HashMap<Id, DgPhase>();
	List<DgPhase> sortedPhasesByOnset = new ArrayList<DgPhase>();
	Map<Integer, List<SignalGroupSettingsData>> onsetSettingsMap = new HashMap<Integer, List<SignalGroupSettingsData>>();

	public void calculateIntergreens(String signalControlInputFile,
			String intergreensOutputFile, boolean simplifyPhases) {
		log.info("Calculate intergreens of the signal control from file "
				+ signalControlInputFile);
		if (simplifyPhases)
			log.info("Simplify signal phases: a phase ends with the last setting starting at the phase starting time, "
					+ "i.e. these kind of phases may overlap. "
					+ "Intergreens of overlapping signals from different phases are defined as zero.");

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

			this.preprocessSystemsSignalGroups(signalSystemData, simplifyPhases);

			// loop through the signal group settings
			// calculate intergreens to signals from the same and the next phase
			for (SignalGroupSettingsData selectedSignal : groupSettingsList) {
				this.handleSamePhase(systemIntergreens, selectedSignal);
				this.handleNextPhase(systemIntergreens, selectedSignal);
			}

			intergreens.addIntergreensForSignalSystem(systemIntergreens);
			log.info("intergreen calculation for signal system "
					+ signalSystemData.getSignalSystemId() + " done");
		}

		// write the intergreens to file
		IntergreenTimesWriter10 writer = new IntergreenTimesWriter10(
				intergreens);
		writer.write(intergreensOutputFile);
	}

	private void preprocessSystemsSignalGroups(
			SignalSystemControllerData signalSystemData, boolean simplifyPhases) {
		log.info("preprocess signal groups data for intergreens calculation");

		// reset system properties
		this.groupSettingsList.clear();
		this.signalGroupIdToPhaseMapping.clear();
		this.sortedPhasesByOnset.clear();
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

		// create structures to simplify the intergreens calculation
		this.createSameOnsetMapping();
		if (simplifyPhases) {
			Map<Integer, List<SignalGroupSettingsData>> droppingSettingsMap = this
					.createSameDroppingMapping();
			this.createSimplifiedPhases(sortedGroupSettingsList,
					droppingSettingsMap);
		} else {
			Map<Id, Set<SignalGroupSettingsData>> overlappingSettingsMap = this
					.createOverlappingSettingsMap();
			this.createPhases(sortedGroupSettingsList, overlappingSettingsMap);
		}
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

	private Map<Integer, List<SignalGroupSettingsData>> createSameDroppingMapping() {
		Map<Integer, List<SignalGroupSettingsData>> droppingSettingsMap = new HashMap<Integer, List<SignalGroupSettingsData>>();
		for (SignalGroupSettingsData settings : groupSettingsList) {
			if (!droppingSettingsMap.containsKey(settings.getDropping())) {
				droppingSettingsMap.put(settings.getDropping(),
						new ArrayList<SignalGroupSettingsData>());
			}
			droppingSettingsMap.get(settings.getDropping()).add(settings);
		}
		return droppingSettingsMap;
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

	// fills signalGroupIdToPhaseMapping with simplified phases
	private void createSimplifiedPhases(
			ArrayList<SignalGroupSettingsData> sortedGroupSettingsList,
			Map<Integer, List<SignalGroupSettingsData>> droppingSettingsMap) {
		Set<SignalGroupSettingsData> handledSignals = new HashSet<SignalGroupSettingsData>();
		// loop through the signal settings sorted by onset
		for (SignalGroupSettingsData selectedSignal : sortedGroupSettingsList) {
			if (handledSignals.contains(selectedSignal)) {
				continue;
			}
			int phaseOn = selectedSignal.getOnset();
			int phaseDrop = selectedSignal.getDropping();
			Map<Id, SignalGroupSettingsData> phaseSignals = new HashMap<Id, SignalGroupSettingsData>();
			// add all signals with the same onset to the phase
			for (SignalGroupSettingsData sameOnsetSignal : onsetSettingsMap
					.get(phaseOn)) {
				if (!handledSignals.contains(sameOnsetSignal)) {
					phaseSignals.put(sameOnsetSignal.getSignalGroupId(),
							sameOnsetSignal);
					handledSignals.add(sameOnsetSignal);
					// phase ends with the last of this signals
					if (sameOnsetSignal.getDropping() > phaseDrop) {
						phaseDrop = sameOnsetSignal.getDropping();
					}
				}
			}
			// add all signals dropping at this end time to the phase
			for (SignalGroupSettingsData sameDroppingSignal : droppingSettingsMap
					.get(phaseDrop)) {
				if (!handledSignals.contains(sameDroppingSignal)) {
					phaseSignals.put(sameDroppingSignal.getSignalGroupId(),
							sameDroppingSignal);
					handledSignals.add(sameDroppingSignal);
				}
			}
			// phase completed
			DgPhase phase = new DgPhase(phaseOn, phaseDrop, phaseSignals);
			this.sortedPhasesByOnset.add(phase);

			// fill signalGroupIdToPhaseMapping with simplified phases
			for (Id signalId : phaseSignals.keySet()) {
				signalGroupIdToPhaseMapping.put(signalId, phase);
			}
		}
	}

	// fills signalGroupIdToPhaseMapping with unsimplified phases
	private void createPhases(
			ArrayList<SignalGroupSettingsData> sortedGroupSettingsList,
			Map<Id, Set<SignalGroupSettingsData>> overlappingSettingsMap) {
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
			this.sortedPhasesByOnset.add(phase);

			// fill signalGroupIdToPhaseMapping with unsimplified phases
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

	private void handleSamePhase(
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
	}

	private void handleNextPhase(
			IntergreensForSignalSystemData systemIntergreens,
			SignalGroupSettingsData selectedSignal) {

		// get the next phase
		boolean cycleTimeCrossed = false;
		DgPhase selectedPhase = this.signalGroupIdToPhaseMapping
				.get(selectedSignal.getSignalGroupId());
		int nextPhaseIndex = this.sortedPhasesByOnset.indexOf(selectedPhase) + 1;
		if (nextPhaseIndex >= this.sortedPhasesByOnset.size()) {
			nextPhaseIndex = 0;
			cycleTimeCrossed = true;
		}
		DgPhase nextPhase = this.sortedPhasesByOnset.get(nextPhaseIndex);

		// calculate intergreens to all signals in the next phase
		for (SignalGroupSettingsData nextPhaseSignals : nextPhase
				.getSignalGroupSettingsByGroupId().values()) {
			int intergreen = nextPhaseSignals.getOnset()
					- selectedSignal.getDropping();
			if (cycleTimeCrossed) {
				intergreen += signalSystemCycleTime;
			}
			// intergreens between overlapping signals from different phases
			// should be zero
			if (intergreen < 0) {
				intergreen = 0;
			}
			systemIntergreens.setIntergreenTime(intergreen,
					selectedSignal.getSignalGroupId(),
					nextPhaseSignals.getSignalGroupId());
		}
	}

	/**
	 * Please choose the boolean "simplifyPhases" here in the code.
	 * 
	 * Intergreen times are only calculated to signals in the next phase. So
	 * different definitions of phases treat in different numbers of calculated
	 * intergreens.
	 * 
	 * A phase is a time period of a (fixed time) signal control, where the end
	 * is defined by the dropping of the largest setting starting at the phase
	 * starting time. So different such phases may overlap each other. If you
	 * choose "simplifyPhases" as true, overlapping phases will be simplified in
	 * the sense that they get zero intergreen times.
	 * 
	 * If you choose "simplifyPhases" as false, all these simplified phases will
	 * be handled as one phase. They will get no intergreen times because they
	 * are allowed to show green at the same time. In this case you will get
	 * more intergreen times, because all intergreens to signals in the next
	 * (now bigger) phase will be calculated.
	 * 
	 */
	public static void main(String[] args) {

		// please choose this flag (see javadoc description above)
		boolean simplifyPhases = true;

		String signalControlFile = DgPaths.REPOS
				+ "shared-svn/studies/projects/cottbus/cottbus_scenario/signal_control.xml";
		String intergreensOutFile = DgPaths.REPOS
				+ "shared-svn/studies/projects/cottbus/cottbus_scenario/";
		if (simplifyPhases)
			intergreensOutFile += "signal_control_simplifiedIntergreens.xml";
		else
			intergreensOutFile += "signal_control_unsimplifiedIntergreens.xml";

		new TtCalculateSimplifiedIntergreens().calculateIntergreens(
				signalControlFile, intergreensOutFile, simplifyPhases);
	}

}
