/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityScoringFunctionFactory.java
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class JointActivityScoringFunctionFactory extends CharyparNagelScoringFunctionFactory {

	private VisitorTracker tracker;
	
	private Map<Person, SocialVertex> vertexMapping;
	
	/**
	 * @param config
	 */
	public JointActivityScoringFunctionFactory(VisitorTracker tracker, SocialGraph graph) {
		super(new CharyparNagelScoringConfigGroup());
		
		vertexMapping = new HashMap<Person, SocialVertex>();
		for(SocialVertex vertex : graph.getVertices()) {
			vertexMapping.put(vertex.getPerson().getPerson(), vertex);
		}
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator sf = (ScoringFunctionAccumulator) super.createNewScoringFunction(plan);
		sf.addScoringFunction(new JointActivityScorer(plan.getPerson(), tracker, vertexMapping));
		return sf;
	}

}
