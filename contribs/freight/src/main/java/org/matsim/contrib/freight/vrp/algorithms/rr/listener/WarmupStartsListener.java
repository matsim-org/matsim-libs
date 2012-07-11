package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;

public interface WarmupStartsListener extends RuinAndRecreateControlerListener{

	void informWarmupStarts(RuinAndRecreateSolution currentSolution);

}
