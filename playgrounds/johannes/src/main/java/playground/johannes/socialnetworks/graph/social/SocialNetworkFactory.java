/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkFactory2.java
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
package playground.johannes.socialnetworks.graph.social;

import org.matsim.contrib.sna.graph.GraphFactory;

/**
 * @author illenberger
 *
 */
public class SocialNetworkFactory implements GraphFactory<SocialNetwork, Ego, SocialTie>{

	public SocialTie createEdge() {
		return new SocialTie(0);
	}
	
	public SocialTie createEdge(int created) {
		return new SocialTie(created);
	}

	public SocialNetwork createGraph() {
		return new SocialNetwork();
	}

	public Ego createVertex() {
		throw new UnsupportedOperationException();
	}
	
	public Ego createVertex(SocialPerson person) {
		return new Ego(person);
	}

}
