/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningController.java
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

package playground.christoph.withinday.replanning.replanners.tools;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;

import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayReplanner;

// yyyy Could this be extended to a ControllerListener? cdobler, Oct'10
public class ReplanningController {
	
	private Scenario scenario; 
	private int idCounter = 0;
	
	private Set<WithinDayReplanner<InitialIdentifier>> intialReplanners;
	private Set<WithinDayReplanner<DuringLegIdentifier>> duringLegReplanners;
	private Set<WithinDayReplanner<DuringActivityIdentifier>> duringActivityReplanners;

	public ReplanningController() {
		this.scenario = new ScenarioImpl();
		init();
	}

	public ReplanningController(Scenario scenario) {
		this.scenario = scenario;
		init();
	}
	
	private void init() {
		intialReplanners = new HashSet<WithinDayReplanner<InitialIdentifier>>();
		duringLegReplanners = new HashSet<WithinDayReplanner<DuringLegIdentifier>>();
		duringActivityReplanners = new HashSet<WithinDayReplanner<DuringActivityIdentifier>>();		
	}
	
	public final synchronized Id getNextId() {
		Id id = scenario.createId("WithinDayReplanner" + idCounter);
		idCounter++;
		
		return id;
	}
	
	public final boolean addInitialReplanner(WithinDayReplanner<InitialIdentifier> replanner) {
		return this.intialReplanners.add(replanner);
	}
	
	public final boolean addDuringLegReplanner(WithinDayReplanner<DuringLegIdentifier> replanner) {
		return this.duringLegReplanners.add(replanner);
	}
	
	public final boolean addDuringActivityReplanner(WithinDayReplanner<DuringActivityIdentifier> replanner) {
		return this.duringActivityReplanners.add(replanner);
	}
	
	public final boolean removeInitialReplanner(WithinDayReplanner<InitialIdentifier> replanner) {
		return this.intialReplanners.remove(replanner);
	}
	
	public final boolean removeDuringLegReplanner(WithinDayReplanner<DuringLegIdentifier> replanner) {
		return this.duringLegReplanners.remove(replanner);
	}
	
	public final boolean removeDuringActivityReplanner(WithinDayReplanner<DuringActivityIdentifier> replanner) {
		return this.duringActivityReplanners.remove(replanner);
	}
	
	public final Set<WithinDayReplanner<InitialIdentifier>> getInitialReplanners() {
		return Collections.unmodifiableSet(intialReplanners);
	}
	
	public final Set<WithinDayReplanner<DuringLegIdentifier>> getDuringLegReplanners() {
		return Collections.unmodifiableSet(duringLegReplanners);
	}
	
	public final Set<WithinDayReplanner<DuringActivityIdentifier>> getDuringActivityReplanners() {
		return Collections.unmodifiableSet(duringActivityReplanners);
	}

	public final Set<WithinDayReplanner<? extends AgentsToReplanIdentifier>> getReplanners() {
		Set<WithinDayReplanner<? extends AgentsToReplanIdentifier>> replanners = new HashSet<WithinDayReplanner<? extends AgentsToReplanIdentifier>>();
		replanners.addAll(intialReplanners);
		replanners.addAll(duringLegReplanners);
		replanners.addAll(duringActivityReplanners);
		return replanners;
		
	}
}
