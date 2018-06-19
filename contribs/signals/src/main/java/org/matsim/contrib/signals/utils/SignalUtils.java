/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.utils;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalGroupSettingsDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalSystemControllerDataImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.lanes.data.Lane;

/**
 * @author dgrether
 * 
 */
public class SignalUtils {

	/**
	 * Create an empty SignalsData object
	 * 
	 * @param signalSystemsConfigGroup
	 */
	public static SignalsData createSignalsData(SignalSystemsConfigGroup signalSystemsConfigGroup) {
		return new SignalsDataImpl(signalSystemsConfigGroup);
	}

	/**
	 * Creates a signal group for each single signal of the
	 * system and adds it to the given SignalGroupsData container. 
	 * Each created signal group will get the same ID as the signal itself.
	 * 
	 * @param groups
	 *            container to that the signal groups are added
	 * @param system
	 *            contains the signals for which signal groups are created
	 */
	public static void createAndAddSignalGroups4Signals(SignalGroupsData groups, SignalSystemData system) {
		for (SignalData signal : system.getSignalData().values()) {
			SignalGroupData group4signal = groups.getFactory().createSignalGroupData(system.getId(), 
					Id.create(signal.getId(), SignalGroup.class));
			group4signal.addSignalId(signal.getId());
			groups.addSignalGroupData(group4signal);
		}
	}

	/**
	 * Convenience method to create a signal with the given Id on the link with
	 * the given Id on the lanes with the given Ids. The signal is added to the
	 * SignalSystemData.
	 */
	public static void createAndAddSignal(SignalSystemData sys,
			SignalSystemsDataFactory factory, Id<Signal> signalId,
			Id<Link> linkId, List<Id<Lane>> laneIds) {
		
		SignalData signal = factory.createSignalData(signalId);
		sys.addSignalData(signal);
		signal.setLinkId(linkId);
		if (laneIds != null) {
			for (Id<Lane> laneId : laneIds) {
				signal.addLaneId(laneId);
			}
		}
	}
	
	/**
	 * Creates a signal plan with the given cycle time and offset.
	 * The plan gets the default signal plan id 1.
	 * 
	 * @param fac
	 * @param cycleTime
	 * @param offset
	 * @return the signal plan
	 */
	public static SignalPlanData createSignalPlan(SignalControlDataFactory fac, int cycleTime, int offset) {
		return createSignalPlan(fac, cycleTime, offset, Id.create(1,SignalPlan.class));
	}
	
	/**
	 * Creates a signal plan with the given cycle time, offset and id.
	 * 
	 * @param fac
	 * @param cycleTime
	 * @param offset
	 * @param signalPlanId
	 * @return the signal plan
	 */
	public static SignalPlanData createSignalPlan(SignalControlDataFactory fac, int cycleTime, int offset, Id<SignalPlan> signalPlanId) {
		
		SignalPlanData signalPlan = fac.createSignalPlanData(signalPlanId);
		signalPlan.setCycleTime(cycleTime);
		signalPlan.setOffset(offset);
		return signalPlan;
	}

	/**
	 * Creates and returns a signal group setting for the given signal group id with the
	 * given onset and dropping time.
	 * 
	 * @param fac
	 * @param signalGroupId
	 * @param onset
	 * @param dropping
	 * @return the signal group setting
	 */
	public static SignalGroupSettingsData createSetting4SignalGroup(
			SignalControlDataFactory fac, Id<SignalGroup> signalGroupId,
			int onset, int dropping) {

		SignalGroupSettingsData signalGroupSettings = fac
				.createSignalGroupSettingsData(signalGroupId);
		signalGroupSettings.setOnset(onset);
		signalGroupSettings.setDropping(dropping);
		return signalGroupSettings;
	}
	
	/**
	 * Clones the SignalControlData given as argument and returns a new instance of SignalControlData with the same content.
	 */
	public static SignalControlData copySignalControlData (SignalControlData oldSignalControlData) {
		SignalControlData newSignalControlData = new SignalControlDataImpl();
		for (SignalSystemControllerData oldSystemControl : oldSignalControlData.getSignalSystemControllerDataBySystemId().values()) {
			SignalSystemControllerData newSystemControl = new SignalSystemControllerDataImpl(oldSystemControl.getSignalSystemId());
			newSystemControl.setControllerIdentifier(oldSystemControl.getControllerIdentifier());
			for (SignalPlanData oldPlan : oldSystemControl.getSignalPlanData().values()) {
				SignalPlanData newPlan = new SignalPlanDataImpl(oldPlan.getId());
				newPlan.setStartTime(oldPlan.getStartTime());
				newPlan.setEndTime(oldPlan.getEndTime());
				newPlan.setCycleTime(oldPlan.getCycleTime());
				newPlan.setOffset(oldPlan.getOffset());
				for (SignalGroupSettingsData oldGroupSetting : oldPlan.getSignalGroupSettingsDataByGroupId().values()) {
					SignalGroupSettingsData newGroupSettings = new SignalGroupSettingsDataImpl(oldGroupSetting.getSignalGroupId());
					newGroupSettings.setOnset(oldGroupSetting.getOnset());
					newGroupSettings.setDropping(oldGroupSetting.getDropping());
					newPlan.addSignalGroupSettings(newGroupSettings);
				}
				newSystemControl.addSignalPlanData(newPlan);
			}
			newSignalControlData.addSignalSystemControllerData(newSystemControl);
		}
		return newSignalControlData;
	}

}
