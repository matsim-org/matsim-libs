/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToDropOffIdentifierFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.filter.EarliestLinkExitTimeFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.trafficmonitoring.LinkEnteredProvider;

import playground.christoph.evacuation.withinday.replanning.identifiers.filters.AffectedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.InformedAgentsFilterFactory;

public class AgentsToDropOffIdentifierFactory extends DuringLegIdentifierFactory {

	private final MobsimDataProvider mobsimDataProvider;
	private final LinkEnteredProvider linkEnteredProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final JointDepartureCoordinator jointDepartureCoordinator;
	
	public AgentsToDropOffIdentifierFactory(MobsimDataProvider mobsimDataProvider, LinkEnteredProvider linkEnteredProvider, 
			JointDepartureOrganizer jointDepartureOrganizer, JointDepartureCoordinator jointDepartureCoordinator, 
			AffectedAgentsFilterFactory affectedAgentsFilterFactory, TransportModeFilterFactory transportModeFilterFactory, 
			InformedAgentsFilterFactory informedAgentsFilterFactory, EarliestLinkExitTimeFilterFactory earliestLinkExitTimeFilterFactory) {
		this.mobsimDataProvider = mobsimDataProvider;
		this.linkEnteredProvider = linkEnteredProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
		this.jointDepartureCoordinator = jointDepartureCoordinator;
		
		// remove agents which are not informed yet
		this.addAgentFilterFactory(informedAgentsFilterFactory);
		
		// remove agents which are not traveling by car
		this.addAgentFilterFactory(transportModeFilterFactory);
		
		// remove agents which are not traveling inside the affected area
		this.addAgentFilterFactory(affectedAgentsFilterFactory);
		
		/*
		 *  Remove agents that cannot stop at their current link anymore. This can occur
		 *  on very short links (min travel time < 1 second). Then, the vehicle might
		 *  already be in the outgoing buffer.
		 */
		this.addAgentFilterFactory(earliestLinkExitTimeFilterFactory);
	}
	
	@Override
	public DuringLegIdentifier createIdentifier() {
		
		DuringLegIdentifier identifier = new AgentsToDropOffIdentifier(this.mobsimDataProvider, this.linkEnteredProvider, 
				this.jointDepartureOrganizer, this.jointDepartureCoordinator);
		identifier.setAgentSelectorFactory(this);
		this.addAgentFiltersToIdentifier(identifier);
		return identifier;
	}

}
