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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsDataImpl;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsReader20;
import org.xml.sax.SAXException;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.sylvia.model.DgSylviaController;
import playground.dgrether.signalsystems.utils.DgSignalGroupSettingsDataOnsetComparator;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;


/**
 * @author dgrether
 *
 */
public class DgSylviaPreprocessData {

	private static final Logger log = Logger.getLogger(DgSylviaPreprocessData.class);

	public static final String FIXED_TIME_PREFIX = "fixed_time_plan_";

	public static final String SYLVIA_PREFIX = "sylvia_plan_";

	private int minGreenSeconds = 5; //see RILSA pp. 28

	private Set<Id> signalSystemIds = new HashSet<Id>();
	
	public DgSylviaPreprocessData(){
//			this.signalSystemIds.add(new IdImpl(1));
			this.signalSystemIds.add(new IdImpl(17));
//			this.signalSystemIds.add(new IdImpl(18));
	}


	public void convertFixedTimePlansToSylviaBasePlans(String signalControlInputFile, String singalControlOutputFile, String signalGroupsFile, String signalGroupsOutFile) throws JAXBException, SAXException, ParserConfigurationException, IOException{
		SignalControlData signalControl = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(signalControl);
		reader.readFile(signalControlInputFile);

		SignalGroupsData signalGroupsData = new SignalGroupsDataImpl();
		SignalGroupsReader20 groupsReader = new SignalGroupsReader20(signalGroupsData);
		groupsReader.readFile(signalGroupsFile);

//		this.convertSignalGroupsData(signalGroupsData, signalControl);

		SignalControlData newSignalControl = this.convertSignalControlData(signalControl);

		SignalControlWriter20 writer = new SignalControlWriter20(newSignalControl);
		writer.write(singalControlOutputFile);

//		SignalGroupsWriter20 groupsWriter = new SignalGroupsWriter20(signalGroupsData);
//		groupsWriter.write(signalGroupsOutFile);

	}


	/**
	 * Merge all signalGroups that have equal onsets and droppings to one SignalGroup, also change the SignalControlData accordingly
	 */
	private SignalGroupsData convertSignalGroupsData(SignalGroupsData signalGroupsData, SignalControlData signalControl) {
		for (SignalSystemControllerData  controllerData : signalControl.getSignalSystemControllerDataBySystemId().values()){
			if (controllerData.getSignalPlanData().size() > 1){
				log.warn("More than one plan, check if this tool is doing the correct work!");
			}
			for (SignalPlanData signalPlan : controllerData.getSignalPlanData().values()){
				Map<Integer, List<SignalGroupSettingsData>> onsetGroupSettingsMap = new HashMap<Integer, List<SignalGroupSettingsData>>();
				for (SignalGroupSettingsData signalGroupSettings : signalPlan.getSignalGroupSettingsDataByGroupId().values()){
					if (!onsetGroupSettingsMap.containsKey(signalGroupSettings.getOnset())){
						onsetGroupSettingsMap.put(signalGroupSettings.getOnset(), new ArrayList<SignalGroupSettingsData>());
					}
					onsetGroupSettingsMap.get(signalGroupSettings.getOnset()).add(signalGroupSettings);
				}
			}

		}
		return null;
	}


	private SignalControlData convertSignalControlData(final SignalControlData controlData){
		SignalControlData cd = new SignalControlDataImpl();
		for (SignalSystemControllerData  controllerData: controlData.getSignalSystemControllerDataBySystemId().values()){
			if (!this.signalSystemIds.contains(controllerData.getSignalSystemId())){
				cd.addSignalSystemControllerData(controllerData);
			}
			else {
				SignalSystemControllerData  newControllerData = cd.getFactory().createSignalSystemControllerData(controllerData.getSignalSystemId());
				cd.addSignalSystemControllerData(newControllerData);
				newControllerData.setControllerIdentifier(DgSylviaController.CONTROLLER_IDENTIFIER);
				log.debug("");
				log.debug("system: " + controllerData.getSignalSystemId());
				for (SignalPlanData signalPlan : controllerData.getSignalPlanData().values()){
					//add a copy of the old plan
					newControllerData.addSignalPlanData(DgSignalsUtils.copySignalPlanData(signalPlan, new IdImpl(FIXED_TIME_PREFIX + signalPlan.getId().toString()), cd.getFactory()));
					newControllerData.addSignalPlanData(this.convertSignalPlanData(signalPlan, cd.getFactory()));
				}
			}
		}
		return cd;
	}

	private SignalPlanData convertSignalPlanData(final SignalPlanData fixedTimePlan, SignalControlDataFactory factory) {
		SignalPlanData newPlan = DgSignalsUtils.copySignalPlanData(fixedTimePlan, new IdImpl(SYLVIA_PREFIX + fixedTimePlan.getId().toString()), factory);
		List<SignalGroupSettingsData> groupSettingsList = new ArrayList<SignalGroupSettingsData>();
		groupSettingsList.addAll(newPlan.getSignalGroupSettingsDataByGroupId().values());

		//filter allGreenSettings
		Set<SignalGroupSettingsData> allGreenSettings = this.removeAllGreenSignalGroupSettings(groupSettingsList, fixedTimePlan.getCycleTime());

		List<Phase> phases = this.calculateSortedPhases(groupSettingsList);
		
		Collections.sort(groupSettingsList, new DgSignalGroupSettingsDataOnsetComparator());
		int onset = -1;
		int dropping = 0;
		int fixedTimeDropping = 0;
		for (int i = 0; i < groupSettingsList.size(); i++) {
			SignalGroupSettingsData settings = groupSettingsList.get(i);
			SignalGroupSettingsData fixedTimeSettings = fixedTimePlan.getSignalGroupSettingsDataByGroupId().get(settings.getSignalGroupId());
			fixedTimeDropping = fixedTimeSettings.getDropping();
			if (i == 0) {
				onset = settings.getOnset();
				log.debug("first onset: " + onset);
				dropping = onset + minGreenSeconds;
			}
			else { // i > 0
				SignalGroupSettingsData lastSettings = groupSettingsList.get(i - 1);
				SignalGroupSettingsData lastFixedTimeSettings = fixedTimePlan.getSignalGroupSettingsDataByGroupId().get(lastSettings.getSignalGroupId());
				if (lastFixedTimeSettings.getOnset() != settings.getOnset()){ // increment onset and dropping
					int intergreen = settings.getOnset() - lastFixedTimeSettings.getDropping();
					onset = dropping + intergreen;
					dropping = onset + minGreenSeconds;
				}
			}
			//set the values
			log.debug("sg " + settings.getSignalGroupId() + " fixedonset: " + fixedTimeSettings.getOnset() + " fixeddrop: " + fixedTimeDropping);
			log.debug("sg " + settings.getSignalGroupId() + " onset: " + onset + " drop: " + dropping);
			settings.setOnset(onset);
			settings.setDropping(dropping);
		}
		int lastIntergreen = fixedTimePlan.getCycleTime() - fixedTimeDropping;
		int sylviaCycle = dropping + lastIntergreen;
		newPlan.setCycleTime(sylviaCycle);
		for (SignalGroupSettingsData settings : allGreenSettings){
			settings.setOnset(0);
			settings.setDropping(sylviaCycle);
		}
		return newPlan;
	}

	private List<Phase> calculateSortedPhases(final List<SignalGroupSettingsData> groupSettingsList) {
		List<Phase> phases = new ArrayList<Phase>();
		//make a copy
		ArrayList<SignalGroupSettingsData> settingsList = new ArrayList<SignalGroupSettingsData>();
		settingsList.addAll(groupSettingsList);
		//sort the copy
		Collections.sort(settingsList, new DgSignalGroupSettingsDataOnsetComparator());
		//preprocess
		Map<Integer, Set<SignalGroupSettingsData>> onsetSettingsMap = new HashMap<Integer, Set<SignalGroupSettingsData>>();
		Map<Integer, Set<SignalGroupSettingsData>> droppingSettingsMap = new HashMap<Integer, Set<SignalGroupSettingsData>>();
		for (SignalGroupSettingsData settings : groupSettingsList){
			if (!onsetSettingsMap.containsKey(settings.getOnset())){
				onsetSettingsMap.put(settings.getOnset(), new HashSet<SignalGroupSettingsData>());
			}
			onsetSettingsMap.get(settings.getOnset()).add(settings);
			if (!droppingSettingsMap.containsKey(settings.getDropping())){
				droppingSettingsMap.put(settings.getDropping(), new HashSet<SignalGroupSettingsData>());
			}
			droppingSettingsMap.get(settings.getDropping()).add(settings);
		}
		
		//create the phases
		Set<SignalGroupSettingsData> handledSettings = new HashSet<SignalGroupSettingsData>();
		//loop through the settings sorted by onset
		for (SignalGroupSettingsData settings : settingsList){
			if (handledSettings.contains(settings)){
				continue;
			}
			Set<SignalGroupSettingsData> sameOnsetSettings = onsetSettingsMap.get(settings.getOnset());
			SignalGroupSettingsData lastDropSettings = this.getLastDroppingSettings(sameOnsetSettings); 
			int phaseOn = lastDropSettings.getOnset();
			int phaseDrop = lastDropSettings.getDropping();
			Phase phase = new Phase(phaseOn, phaseDrop);
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
	


	private SignalGroupSettingsData getLastDroppingSettings(Set<SignalGroupSettingsData> sameOnsetSettings) {
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



	private static final class Phase {
		
		private static final Logger log = Logger.getLogger(DgSylviaPreprocessData.Phase.class);
		private Integer on = null;
		private Integer off = null;
		private Map<Id, SignalGroupSettingsData> signalGroupSettingsByGroupId = new HashMap<Id, SignalGroupSettingsData>();
		
		public Phase(Integer phaseOn, Integer phaseDrop) {
			log.debug("created phase from " + phaseOn + " to " + phaseDrop);
			this.on = phaseOn;
			this.off = phaseDrop;
		}

		public Integer getPhaseStartSecond(){
			return this.on;
		}
		
		public Integer getPhaseEndSecond(){
			return this.off;
		}
		
		public void addSignalGroupSettingsData(SignalGroupSettingsData settings) {
			log.debug("  adding settings to phase: " + settings.getSignalGroupId() + " on: " + settings.getOnset() + " drop " + settings.getDropping());
			this.signalGroupSettingsByGroupId.put(settings.getSignalGroupId(), settings);
		}
		
		public Map<Id, SignalGroupSettingsData> getSignalGroupSettingsByGroupId(){
			return this.signalGroupSettingsByGroupId;
		}
		
	}

	
	private Set<SignalGroupSettingsData> removeAllGreenSignalGroupSettings(List<SignalGroupSettingsData> groupSettingsList, Integer cycleTime){
		Set<SignalGroupSettingsData> allGreenSettings = new HashSet<SignalGroupSettingsData>();
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
	
	
	
	

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws JAXBException 
	 */
	public static void main(String[] args) throws JAXBException, SAXException, ParserConfigurationException, IOException {
		String signalControlFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-lsa/signalControlCottbusT90_v2.0_jb_ba_removed.xml";
		String signalControlOutFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/sylvia/signal_control_sylvia.xml";
		String signalGroupsFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/signalGroupsCottbusByNodes_v2.0.xml";
		String signalGroupsOutFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/sylvia/signal_groups_sylvia.xml";
		new DgSylviaPreprocessData().convertFixedTimePlansToSylviaBasePlans(signalControlFile, signalControlOutFile, signalGroupsFile, signalGroupsOutFile);
	}

}
