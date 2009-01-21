/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnet;

import java.util.Iterator;

import org.matsim.basic.v01.BasicActivity;
import org.matsim.basic.v01.BasicKnowledge;
import org.matsim.basic.v01.BasicPerson;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicPopulation;

import playground.johannes.graph.generators.GraphFactory;

/**
 * @author illenberger
 *
 */
public class SocialNetworkFactory<P extends BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> implements GraphFactory<SocialNetwork<P>, Ego<P>, SocialTie> {

	private BasicPopulation<P> population;
	
	private Iterator<P> popIterator;
	
	public SocialNetworkFactory(BasicPopulation<P> population) {
		this.population = population;
	}
	
	public SocialTie addEdge(SocialNetwork<P> g, Ego<P> v1, Ego<P> v2) {
		return g.addEdge(v1, v2);
	}

	public Ego<P> addVertex(SocialNetwork<P> g) {
		if(popIterator.hasNext())
			return g.addEgo(popIterator.next());
		else
			return null;
	}

	public SocialNetwork<P> createGraph() {
		popIterator = population.getPersons().values().iterator();
		return new SocialNetwork<P>();
	}

}
