/* *********************************************************************** *
 * project: org.matsim.*
 * AgeTripCorrelationTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.analysis;

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.sna.graph.social.analysis.Age;
import playground.johannes.coopsim.pysical.VisitorTracker;

/**
 * @author illenberger
 *
 */
public class AgeTripCorrelationTask extends SocialTripCorrelationTask {

	private final TObjectDoubleHashMap<Vertex> values;
	
	public AgeTripCorrelationTask(SocialGraph graph, VisitorTracker tracker) {
		super(graph, tracker, "age");
		values = Age.getInstance().values(graph.getVertices());
	}

	@Override
	protected double getValue(SocialVertex v) {
		return values.get(v);
	}

}
