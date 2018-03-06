package org.matsim.contrib.pseudosimulation.replanning;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.distributed.plans.PlanGenome;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by fouriep on 12/10/14.
 */
@Singleton
public class PlanCatcher {
    @Inject
	public PlanCatcher(){}

    private Collection<Plan> plansForPSim;

    public Collection<Plan> getPlansForPSim() {
        return plansForPSim;
    }

    public void addPlansForPsim(Plan plan) {
        if (plansForPSim == null)
            plansForPSim = new ArrayList<>();
        plansForPSim.add(plan);
    }

    public void init() {
        plansForPSim = new ArrayList<>();
    }
}
