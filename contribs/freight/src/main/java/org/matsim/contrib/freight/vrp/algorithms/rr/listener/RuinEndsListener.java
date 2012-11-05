package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgent;
import org.matsim.core.controler.listener.ControlerListener;

public interface RuinEndsListener extends ControlerListener {

	public void informRuinEnds(Collection<RouteAgent> tourAgents);

}
