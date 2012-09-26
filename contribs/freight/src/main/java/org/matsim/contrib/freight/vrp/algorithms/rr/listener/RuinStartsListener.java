package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinStrategy;

public interface RuinStartsListener extends RuinAndRecreateListener {

	public void informRuinStarts(RuinStrategy ruinStrategy,
			RuinAndRecreateSolution solution);

}
