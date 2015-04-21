package org.matsim.contrib.pseudosimulation.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.distributed.plans.PlanGenome;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by fouriep on 12/10/14.
 */
public class PlanCatcher {

    private Collection<Plan> plansForPSim;



    public Collection<Plan> getPlansForPSim() {
        return plansForPSim;
    }

    public void addPlansForPsim(Plan plan) {
        if (plan instanceof PlanGenome)
            ((PlanGenome) plan).resetScoreComponents();
        if (plansForPSim == null)
            plansForPSim = new ArrayList<>();
        plansForPSim.add(plan);
    }

    public void init() {
        plansForPSim = new ArrayList<>();
    }
}
