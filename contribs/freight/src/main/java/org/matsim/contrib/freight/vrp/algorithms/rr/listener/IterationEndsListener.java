package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;

public interface IterationEndsListener extends RuinAndRecreateListener {

	void informIterationEnds(int currentIteration,
			RuinAndRecreateSolution currentSolution,
			RuinAndRecreateSolution rejectedSolution);

}
