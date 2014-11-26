package playground.pieter.distributed.replanning.modules;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.pieter.distributed.SlaveControler;

/**
 * Created by fouriep on 11/26/14.
 */
public class RegisterMutatedPlanForPSim implements PlanStrategyModule {
    public RegisterMutatedPlanForPSim(SlaveControler slave) {
        this.slave = slave;
    }

    private SlaveControler slave;

    @Override
    public void prepareReplanning(ReplanningContext replanningContext) {

    }

    @Override
    public void handlePlan(Plan plan) {
            slave.addPlansForPsim(plan);

    }

    @Override
    public void finishReplanning() {

    }



}

