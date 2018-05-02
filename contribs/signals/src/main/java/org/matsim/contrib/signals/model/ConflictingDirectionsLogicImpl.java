/* *********************************************************************** *
 * project: org.matsim.*
 * IntergreensLogicImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.ActionOnSignalSpecsViolation;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.conflicts.ConflictingDirections;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;


/**
 * @author tthunig
 *
 */
public class ConflictingDirectionsLogicImpl implements ConflictingDirectionsLogic {

private static final Logger log = Logger.getLogger(ConflictingDirectionsLogicImpl.class);

	private ActionOnSignalSpecsViolation actionOnConflictingDirectionsViolation;
	private Map<Id<Signal>, Set<Tuple<Id<Link>, Id<Link>>>> setOfLinkTuplesPerSignal = new HashMap<>();
	private Map<Id<SignalGroup>, Set<Direction>> setOfDirectionsPerGroup = new HashMap<>();
	
	private Map<Id<SignalSystem>, List<Id<SignalGroup>>> greenSignalsPerSystem = new HashMap<>();

	public ConflictingDirectionsLogicImpl(Lanes lanes, SignalsData signalsData, ActionOnSignalSpecsViolation actionOnConflictingDirectionsViolation) {
		this.actionOnConflictingDirectionsViolation = actionOnConflictingDirectionsViolation;
		
		for (SignalSystemData signalSystem : signalsData.getSignalSystemsData().getSignalSystemData().values()) {
			// remember relation of signals to directions
			for (SignalData signal : signalSystem.getSignalData().values()) {
				setOfLinkTuplesPerSignal.put(signal.getId(), new HashSet<>());
				for (Id<Lane> signalizedLaneId : signal.getLaneIds()) {
					Lane signalizedLane = lanes.getLanesToLinkAssignments().get(signal.getLinkId()).getLanes().get(signalizedLaneId);
					for (Id<Link> toLinkId : signalizedLane.getToLinkIds()) {
						setOfLinkTuplesPerSignal.get(signal.getId()).add(new Tuple<Id<Link>, Id<Link>>(signal.getLinkId(), toLinkId));
					}
				}
			}
			
			// group directions to signal groups
			ConflictingDirections conflictsOfThisSystem = signalsData.getConflictingDirectionsData().getConflictsPerSignalSystem().get(signalSystem.getId());
			for (SignalGroupData group : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
				setOfDirectionsPerGroup.put(group.getId(), new HashSet<>());
				for (Id<Signal> signalIdOfThisGroup : signalsData.getSignalGroupsData().getSignalGroupDataBySystemId(signalSystem.getId()).get(group.getId()).getSignalIds()) {
					for (Tuple<Id<Link>, Id<Link>> from2toLink : setOfLinkTuplesPerSignal.get(signalIdOfThisGroup)) {
						setOfDirectionsPerGroup.get(group.getId()).add(conflictsOfThisSystem.getDirection(from2toLink.getFirst(), from2toLink.getSecond()));
					}
				}
				log.info("Group " + group.getId() + " corresponds to " + setOfDirectionsPerGroup.get(group.getId()).size() + " directions.");
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.greenSignalsPerSystem.clear();
	}

	@Override
	public void handleEvent(SignalGroupStateChangedEvent event) {
		if (SignalGroupState.GREEN.equals(event.getNewState())){
			// register system if necessary
			if (!greenSignalsPerSystem.containsKey(event.getSignalSystemId())) {
				greenSignalsPerSystem.put(event.getSignalSystemId(), new LinkedList<>());
			}
			
			// check whether a conflicting direction already shows green
			this.checkAndHandleGreenAllowed(event);
			
			// add signal as green
			greenSignalsPerSystem.get(event.getSignalSystemId()).add(event.getSignalGroupId());
		} else if (SignalGroupState.RED.equals(event.getNewState())) {
			// remove signal from the set of green signal groups
			if (greenSignalsPerSystem.containsKey(event.getSignalSystemId())) {
				greenSignalsPerSystem.get(event.getSignalSystemId()).remove(event.getSignalGroupId());
			}
		}
	}
	
	private void checkAndHandleGreenAllowed(SignalGroupStateChangedEvent event) {
		// check whether one direction that has green at the moment is conflicting with at least one direction of the group that is switched to green
		for (Id<SignalGroup> greenGroupId : greenSignalsPerSystem.get(event.getSignalSystemId())) {
			for (Direction greenDirection : setOfDirectionsPerGroup.get(greenGroupId)) {
				for (Direction directionToSwitchGreen : setOfDirectionsPerGroup.get(event.getSignalGroupId())) {
					if (greenDirection.getConflictingDirections().contains(directionToSwitchGreen.getId()) || directionToSwitchGreen.getConflictingDirections().contains(greenDirection.getId())) {
						String conflictingDirectionViolation = "Signal Group " + event.getSignalGroupId() + " is switched to green although " + greenGroupId 
								+ " already shows green. This is not allowed due to the conflicting directions data defined in the scenario because direction " 
								+ greenDirection.getId() + " from link " + greenDirection.getFromLink() + " to link " + greenDirection.getToLink() 
								+ " is conflicting to direction " + directionToSwitchGreen.getId() + " from link " + directionToSwitchGreen.getFromLink()
								+ " to link " + directionToSwitchGreen.getToLink() + ".";
						if (this.actionOnConflictingDirectionsViolation.equals(SignalSystemsConfigGroup.ActionOnSignalSpecsViolation.WARN)) {
							log.warn(conflictingDirectionViolation);
						} else if (this.actionOnConflictingDirectionsViolation.equals(SignalSystemsConfigGroup.ActionOnSignalSpecsViolation.EXCEPTION)) {
							log.warn(conflictingDirectionViolation);
							throw new RuntimeException(conflictingDirectionViolation);
						}
					}
				}
			}
		}	
	}
}
