package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;

public interface RuinStartsListener extends RuinAndRecreateListener{

	public void informRuinStarts(RuinAndRecreateSolution solution);

}
