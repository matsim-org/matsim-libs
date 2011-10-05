/* *********************************************************************** *
 * project: org.matsim.*
 * EvaluationService.java
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
package playground.johannes.coopsim.services;

import java.util.Collection;

import playground.johannes.coopsim.eval.Evaluator;
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class EvaluationService implements SimService<Object> {

	private final SimService<Collection<Trajectory>> mobsimService;
	
	private final Evaluator evaluator;
	
	public EvaluationService(SimService<Collection<Trajectory>> mobsimService, Evaluator evaluator) {
		this.mobsimService = mobsimService;
		this.evaluator = evaluator;
	}
	
	@Override
	public void init() {
	}

	@Override
	public void run() {
		Collection<Trajectory> trajectories = mobsimService.get();
		for(Trajectory t : trajectories) {
			double score = evaluator.evaluate(t);
			t.getPerson().getSelectedPlan().setScore(score);
		}
	}

	@Override
	public Object get() {
		return null;
	}

	@Override
	public void terminate() {
	}

}
