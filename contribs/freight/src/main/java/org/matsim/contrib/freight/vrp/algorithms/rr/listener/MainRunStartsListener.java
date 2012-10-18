package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;

public interface MainRunStartsListener extends RuinAndRecreateListener {

	public void informMainRunStarts(RuinAndRecreateSolution iniSolution);

}
