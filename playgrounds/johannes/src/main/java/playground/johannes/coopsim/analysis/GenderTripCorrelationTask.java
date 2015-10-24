/* *********************************************************************** *
 * project: org.matsim.*
 * GenderTripCorrelationTask.java
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

import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.analysis.Gender;
import playground.johannes.coopsim.pysical.VisitorTracker;

import java.util.Map;

/**
 * @author illenberger
 * 
 */
public class GenderTripCorrelationTask extends SocialTripCorrelationTask {

	private final Map<SocialVertex, String> values;

	public GenderTripCorrelationTask(SocialGraph graph, VisitorTracker tracker) {
		super(graph, tracker, "gender");

		values = Gender.getInstance().values(graph.getVertices());
	}

	@Override
	protected double getValue(SocialVertex v) {
		if (values.get(v).equals(Gender.FEMALE))
			return 1;
		else
			return 0;
	}

}
