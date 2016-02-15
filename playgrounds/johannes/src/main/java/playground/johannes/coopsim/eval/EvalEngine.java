/* *********************************************************************** *
 * project: org.matsim.*
 * EvalEngine.java
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
package playground.johannes.coopsim.eval;

import org.matsim.api.core.v01.Id;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.HashSet;
import java.util.Set;


/**
 * @author illenberger
 * 
 */
public class EvalEngine {
	
	private final Evaluator evaluator;
	
	public EvalEngine(Evaluator evalutor) {
		this.evaluator = evalutor;
	}
	

	public void evaluate(Set<Trajectory> trajectories) {
		Set<Id> persons = new HashSet<Id>();
		for(Trajectory t : trajectories) {
			double score = evaluator.evaluate(t);
			
			t.getPerson().getSelectedPlan().setScore(score);
			persons.add(t.getPerson().getId());
		}
	}
}
