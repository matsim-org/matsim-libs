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

package playground.christoph.withinday.replanning.identifiers;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PersonAgent;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.mobsim.WithinDayQSim;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifier;

public class InitialIdentifierImpl extends InitialIdentifier {

	protected WithinDayQSim simulation;
		
	public InitialIdentifierImpl(WithinDayQSim simulation) {
		this.simulation = simulation;
	}
		
	public List<PersonAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner) {
		List<PersonAgent> agentsToReplan = new ArrayList<PersonAgent>();
		
		for (MobsimAgent mobsimAgent : this.simulation.getAgents()) {
			if (mobsimAgent instanceof WithinDayPersonAgent) {
				WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) mobsimAgent;
				
				if (withinDayPersonAgent.getWithinDayReplanners().contains(withinDayReplanner)) {
					agentsToReplan.add(withinDayPersonAgent);
				}
			}
		}
		
		return agentsToReplan;
	}

	public InitialIdentifierImpl clone() {
		InitialIdentifierImpl clone = new InitialIdentifierImpl(this.simulation);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
}
