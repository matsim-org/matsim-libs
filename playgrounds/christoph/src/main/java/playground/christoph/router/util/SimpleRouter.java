/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleRouter.java
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

package playground.christoph.router.util;

import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PersonalizableTravelCost;

/*
 * This abstract Class contains some Objects that are used by Simple
 * Routers like the Random Router.
 * 
 * By implementing the PersonalizableTravelCost interface the PlansCalcRoute
 * class hands over the currently handled person by using the setPerson method. 
 */
public abstract class SimpleRouter implements LeastCostPathCalculator, PersonalizableTravelCost {
	
	protected KnowledgeTools knowledgeTools;
	protected Network network;
	protected Random random;
	protected LoopRemover loopRemover;
	protected TabuSelector tabuSelector;
	protected Person person;
	
	public SimpleRouter(Network network) {
		this.network = network;
		this.knowledgeTools = new KnowledgeTools();
		this.random = MatsimRandom.getLocalInstance();
		this.loopRemover = new LoopRemover();
		this.tabuSelector = new TabuSelector();
	}
	
	public void setPerson(Person person) {
		this.person = person;
	}

	/*
	 * A typical simple Router like a Random Router ignores
	 * the link travel costs so by default we set them to 0.
	 */
	public double getLinkGeneralizedTravelCost(Link link, double time) {
		return 0;
	}
}
