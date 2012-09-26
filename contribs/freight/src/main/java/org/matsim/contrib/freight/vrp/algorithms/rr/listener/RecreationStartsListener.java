package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;
import org.matsim.contrib.freight.vrp.basics.Job;

public interface RecreationStartsListener extends RuinAndRecreateListener {

	void informRecreationStarts(RuinAndRecreateSolution solution,
			Collection<Job> unassignedJobs);

}
