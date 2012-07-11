package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;
import org.matsim.contrib.freight.vrp.basics.Job;

public interface RecreationStartsListener extends RuinAndRecreateControlerListener{

	void informRecreationStarts(RuinAndRecreateSolution solution, Collection<Job> unassignedJobs);

}
