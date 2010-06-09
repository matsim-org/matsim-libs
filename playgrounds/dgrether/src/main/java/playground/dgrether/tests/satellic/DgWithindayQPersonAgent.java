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
import org.matsim.ptproject.qsim.QNetworkI;
import org.matsim.ptproject.qsim.QPersonAgent;
import org.matsim.ptproject.qsim.QSimI;


/**
 * @author dgrether
 *
 */
public class DgWithindayQPersonAgent extends QPersonAgent {

	private static final Logger log = Logger.getLogger(DgWithindayQPersonAgent.class);
	private Random random;

	public DgWithindayQPersonAgent(Person p, QSimI simulation, Random r) {
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
		QNetworkI qnet = this.getQSimulation().getQNetwork();
		if (currentLinkId == destinationLinkId){
				return null;
		}
		QLink currentQLink = qnet.getLinks().get(currentLinkId);
		Map<Id, ? extends Link> outlinks = currentQLink.getLink().getToNode().getOutLinks();
		List<Link> outLinksList = new ArrayList<Link>();
//		log.error("outlinks.size " + outlinks.size());

		double outLinksCapacitySum = 0.0;
		for (Link outLink : outlinks.values()){
			if (!outLink.getToNode().getId().equals(currentQLink.getLink().getFromNode().getId())){
				outLinksList.add(outLink);
				outLinksCapacitySum += outLink.getCapacity(this.getQSimulation().getSimTimer().getTimeOfDay());
			}
		}
		double randomNumber = random.nextDouble() * outLinksCapacitySum;
		double selectedCapacity = 0.0;
//		log.error("outlinkslist.size " + outLinksList.size());
		for (Link outLink : outLinksList){
			selectedCapacity += outLink.getCapacity(this.getQSimulation().getSimTimer().getTimeOfDay());
//			log.error("selectedCap: " + selectedCapacity + " randomNumber: " + randomNumber);
			if (selectedCapacity >= randomNumber){
				this.cachedNextLinkId = outLink.getId();
				return this.cachedNextLinkId;
			}
		}
		throw new IllegalStateException();
//		
//		
//		int nextLinkNr = (int) (random.nextDouble() * outLinksList.size());
//		Link nextLink = outLinksList.get(nextLinkNr);
//		this.cachedNextLinkId = nextLink.getId();
//		return this.cachedNextLinkId;
	}
	
}
