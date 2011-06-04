/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.taxicab;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.mobsim.framework.PlanDriverAgent;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;

/**
 * @author nagel
 *
 */
public class TaxicabAgent extends ExperimentalBasicWithindayAgent implements PlanDriverAgent, AgentDepartureEventHandler {
	
	Netsim netsim ;
	Scenario sc ;

	/**
	 * @param p
	 * @param simulation
	 */
	public TaxicabAgent(Person p, Netsim simulation) {
		super(p, simulation);
		// TODO Auto-generated constructor stub
		netsim = simulation ;
		sc = netsim.getScenario();
	}
	
	// figure out how this is inserted in matsim.  Look at "createAdditionaAgents"
	
	@Override
	public Id chooseNextLinkId() {
		// where are we?
		Link currentLink = sc.getNetwork().getLinks().get( this.getCurrentLinkId() ) ;
		Node node = currentLink.getToNode() ;
		
		int nOutLinks = node.getOutLinks().size() ;
		int idx = (int) ( Math.random() * nOutLinks ) ;
		
		Id nextLinkId = null ;
		int ii = 0 ;
		for ( Link outLink : node.getOutLinks().values() ) {
			if ( ii == idx ) {
				nextLinkId = outLink.getId() ;
				break ;
			}
			ii++ ;
		}
		
		
		// lskdjf
		return nextLinkId ;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		// should be a DispatherTaxiRequestEvent
		
		// yyyy Auto-generated method stub
//		throw new UnsupportedOperationException() ;
	}

	@Override
	public void reset(int iteration) {
		// yyyy Auto-generated method stub
//		throw new UnsupportedOperationException() ;
	}

}
