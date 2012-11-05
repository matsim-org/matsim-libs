package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgent;
import org.matsim.contrib.freight.vrp.basics.Job;

public interface RecreationStartsListener extends RuinAndRecreateListener {

	void informRecreationStarts(Collection<RouteAgent> tourAgents,
			Collection<Job> unassignedJobs);

}
