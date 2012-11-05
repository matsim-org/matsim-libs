package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgent;

public interface RecreationEndsListener extends RuinAndRecreateListener {

	public void informRecreationEnds(Collection<RouteAgent> tourAgents);

}
