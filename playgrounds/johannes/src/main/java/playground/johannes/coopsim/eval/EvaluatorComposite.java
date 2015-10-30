/* *********************************************************************** *
 * project: org.matsim.*
 * EvaluatorComposite.java
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

import org.apache.log4j.Logger;
import org.matsim.contrib.common.collections.Composite;
import playground.johannes.coopsim.Profiler;
import playground.johannes.coopsim.pysical.Trajectory;

/**
 * @author illenberger
 *
 */
public class EvaluatorComposite extends Composite<Evaluator> implements Evaluator {

	private static final Logger logger = Logger.getLogger(EvaluatorComposite.class);
	
	@Override
	public double evaluate(Trajectory trajectory) {
		double score = 0;
		
		for(int i = 0; i < components.size(); i++) {
			Evaluator e = components.get(i);
			Profiler.resume(e.getClass().getName());
			score += e.evaluate(trajectory);
			Profiler.pause(e.getClass().getName());
		}
		
		if(Double.isInfinite(score))
			logger.warn("*** Infinite score. ***");
		else if (Double.isNaN(score)) {
			logger.warn("*** Score is NaN! Treating like negative infinity. ***");
			return Double.NEGATIVE_INFINITY;
		}

		return score;
	}

}
