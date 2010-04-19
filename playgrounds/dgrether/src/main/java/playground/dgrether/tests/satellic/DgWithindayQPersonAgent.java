/* *********************************************************************** *
 * project: org.matsim.*
 * DgWithindayQPersonAgent
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
package playground.dgrether.tests.satellic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.ptproject.qsim.QPersonAgent;
import org.matsim.ptproject.qsim.QSim;


/**
 * @author dgrether
 *
 */
public class DgWithindayQPersonAgent extends QPersonAgent {

	private static final Logger log = Logger.getLogger(DgWithindayQPersonAgent.class);
	private Random random;

	public DgWithindayQPersonAgent(Person p, QSim simulation, Random r) {
		super(p, simulation);
		random = r;
	}

	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	@Override
	public Id chooseNextLinkId() {
		Id currentLinkId  = this.getCurrentLinkId();
//		if (currentLinkId == null){
//			return super.chooseNextLinkId();
//		}
		Id destinationLinkId = this.getDestinationLinkId();
		QNetwork qnet = this.getQSimulation().getQNetwork();
		if (currentLinkId == destinationLinkId){
				return null;
		}
		QLink currentQLink = qnet.getQLink(currentLinkId);
		Map<Id, ? extends Link> outlinks = currentQLink.getLink().getToNode().getOutLinks();
		List<Link> outLinksList = new ArrayList<Link>(outlinks.values());
		int nextLinkNr = (int) (random.nextDouble() * outLinksList.size());
		Link nextLink = outLinksList.get(nextLinkNr);
		this.cachedNextLinkId = nextLink.getId();
		String nextLinkId = this.cachedNextLinkId.toString();
		return this.cachedNextLinkId;
	}
	
}
