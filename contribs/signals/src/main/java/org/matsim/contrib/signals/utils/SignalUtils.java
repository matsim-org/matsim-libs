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
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
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
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.Lane;

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
	
	// TODO adapt this to more general use cases, e.g. four-arm intersection no groups etc
	public static void fillIntersectionDirectionsForSingleCrossingScenario(
			IntersectionDirections directionsForTheIntersection, Id<SignalSystem> signalSystemId,
			ConflictData conflictData) {
		// WE straight
		Direction dir24 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("2_3"), Id.createLinkId("3_4"), Id.create("2-4", Direction.class));
		directionsForTheIntersection.addDirection(dir24);
		// WE left
		Direction dir27 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("2_3"), Id.createLinkId("3_7"), Id.create("2-7", Direction.class));
		directionsForTheIntersection.addDirection(dir27);
		// WE right
		Direction dir28 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("2_3"), Id.createLinkId("3_8"), Id.create("2-8", Direction.class));
		directionsForTheIntersection.addDirection(dir28);
		// EW straight
		Direction dir42 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("4_3"), Id.createLinkId("3_2"), Id.create("4-2", Direction.class));
		directionsForTheIntersection.addDirection(dir42);
		// EW left
		Direction dir48 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("4_3"), Id.createLinkId("3_8"), Id.create("4-8", Direction.class));
		directionsForTheIntersection.addDirection(dir48);
		// EW right
		Direction dir47 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("4_3"), Id.createLinkId("3_7"), Id.create("4-7", Direction.class));
		directionsForTheIntersection.addDirection(dir47);
		// NS straight
		Direction dir78 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("7_3"), Id.createLinkId("3_8"), Id.create("7-8", Direction.class));
		directionsForTheIntersection.addDirection(dir78);
		// NS left
		Direction dir74 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("7_3"), Id.createLinkId("3_4"), Id.create("7-4", Direction.class));
		directionsForTheIntersection.addDirection(dir74);
		// NS right
		Direction dir72 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("7_3"), Id.createLinkId("3_2"), Id.create("7-2", Direction.class));
		directionsForTheIntersection.addDirection(dir72);
		// SN straight
		Direction dir87 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("8_3"), Id.createLinkId("3_7"), Id.create("8-7", Direction.class));
		directionsForTheIntersection.addDirection(dir87);
		// SN left
		Direction dir82 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("8_3"), Id.createLinkId("3_2"), Id.create("8-2", Direction.class));
		directionsForTheIntersection.addDirection(dir82);
		// SN right
		Direction dir84 = conflictData.getFactory().createDirection(signalSystemId, Id.createNodeId(3),
				Id.createLinkId("8_3"), Id.createLinkId("3_4"), Id.create("8-4", Direction.class));
		directionsForTheIntersection.addDirection(dir84);

		String[] dirNS = { "7-8", "7-4", "7-2", "8-7", "8-2", "8-4" };
		String[] dirWE = { "2-4", "2-7", "2-8", "4-2", "4-8", "4-7" };

		for (int i = 0; i < dirNS.length; i++) {
			dir24.addConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir27.addConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir28.addConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir42.addConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir47.addConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir48.addConflictingDirection(Id.create(dirNS[i], Direction.class));

			dir78.addNonConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir72.addNonConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir74.addNonConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir87.addNonConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir82.addNonConflictingDirection(Id.create(dirNS[i], Direction.class));
			dir84.addNonConflictingDirection(Id.create(dirNS[i], Direction.class));

			Direction dirItself = directionsForTheIntersection.getDirections()
					.get(Id.create(dirNS[i], Direction.class));
			dirItself.getNonConflictingDirections().remove(dirItself.getId());
		}
		for (int i = 0; i < dirWE.length; i++) {
			dir78.addConflictingDirection(Id.create(dirWE[i], Direction.class));
			dir72.addConflictingDirection(Id.create(dirWE[i], Direction.class));
			dir74.addConflictingDirection(Id.create(dirWE[i], Direction.class));
			dir87.addConflictingDirection(Id.create(dirWE[i], Direction.class));
			dir82.addConflictingDirection(Id.create(dirWE[i], Direction.class));
			dir84.addConflictingDirection(Id.create(dirWE[i], Direction.class));
		}

		dir24.addNonConflictingDirection(dir42.getId());
		dir24.addNonConflictingDirection(dir47.getId());
		dir24.addNonConflictingDirection(dir27.getId());
		dir24.addNonConflictingDirection(dir28.getId());
		dir24.addDirectionWhichMustYield(dir48.getId());

		dir27.addNonConflictingDirection(dir24.getId());
		dir27.addNonConflictingDirection(dir28.getId());
		dir27.addNonConflictingDirection(dir48.getId());
		dir27.addDirectionWithRightOfWay(dir42.getId());
		dir27.addDirectionWithRightOfWay(dir47.getId());

		dir28.addNonConflictingDirection(dir24.getId());
		dir28.addNonConflictingDirection(dir27.getId());
		dir28.addNonConflictingDirection(dir47.getId());
		dir28.addNonConflictingDirection(dir42.getId());
		dir28.addDirectionWhichMustYield(dir48.getId());

		dir42.addNonConflictingDirection(dir24.getId());
		dir42.addNonConflictingDirection(dir28.getId());
		dir42.addNonConflictingDirection(dir48.getId());
		dir42.addNonConflictingDirection(dir47.getId());
		dir42.addDirectionWhichMustYield(dir27.getId());

		dir48.addNonConflictingDirection(dir42.getId());
		dir48.addNonConflictingDirection(dir47.getId());
		dir48.addNonConflictingDirection(dir27.getId());
		dir48.addDirectionWithRightOfWay(dir24.getId());
		dir48.addDirectionWithRightOfWay(dir28.getId());

		dir47.addNonConflictingDirection(dir42.getId());
		dir47.addNonConflictingDirection(dir48.getId());
		dir47.addNonConflictingDirection(dir28.getId());
		dir47.addNonConflictingDirection(dir24.getId());
		dir47.addDirectionWhichMustYield(dir27.getId());
	}

}
