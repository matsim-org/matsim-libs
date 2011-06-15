/* *********************************************************************** *
 * project: org.matsim.*
 * InitialIdentifierImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.identifiers;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;

public class InitialIdentifierImpl extends InitialIdentifier {

	protected QSim qsim;

	// use the Factory!
	/*package*/ InitialIdentifierImpl(QSim qsim) {
		this.qsim = qsim;
	}
		
	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		Collection<MobsimAgent> mobsimAgents = this.qsim.getAgents();
		Collection<PlanBasedWithinDayAgent> handledAgents = this.getHandledAgents();
		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		if (this.handleAllAgents()) {
			for (MobsimAgent agent : mobsimAgents) {
				agentsToReplan.add((PlanBasedWithinDayAgent)agent);
			}
			return agentsToReplan;
		}
		
		if (mobsimAgents.size() > handledAgents.size()) {
			for (PlanBasedWithinDayAgent agent : handledAgents) {
				if (mobsimAgents.contains(agent)) {
					agentsToReplan.add(agent);
				}
			}
		} else {
			for (MobsimAgent agent : mobsimAgents) {
				if (handledAgents.contains(agent)) {
					agentsToReplan.add((PlanBasedWithinDayAgent)agent);
				}
			}
		}
	
		return agentsToReplan;
	}

}
