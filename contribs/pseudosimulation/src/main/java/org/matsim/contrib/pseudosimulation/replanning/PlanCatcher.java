package org.matsim.contrib.pseudosimulation.replanning;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.distributed.plans.PlanGenome;

import java.util.*;

/**
 * Created by fouriep on 12/10/14.
 */
@Singleton
public class PlanCatcher {
    @Inject
	public PlanCatcher(){}

    private Map<Id,Plan> plansForPSim;

    public Collection<Plan> getPlansForPSim() {
        return plansForPSim.values();
    }

    public void addPlansForPsim(Plan plan) {
        if (plansForPSim == null)
            plansForPSim = new HashMap<>();
        plansForPSim.put(plan.getPerson().getId(),plan);
    }

    public void init() {
        plansForPSim = new HashMap<>();
    }


	public void removeExistingPlanOrAddNewPlan(Plan plan) {
		if (plansForPSim == null)
			plansForPSim = new HashMap<>();
		if (plansForPSim.get(plan.getPerson().getId()) == plan)
			plansForPSim.remove(plan.getPerson().getId());
		else
			plansForPSim.put(plan.getPerson().getId(),plan);
	}
}
