package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;

public interface AlgorithmEndsListener extends RuinAndRecreateListener {

	public void informAlgorithmEnds(RuinAndRecreateSolution currentSolution);

}
