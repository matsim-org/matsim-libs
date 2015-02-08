package playground.pieter.distributed.replanning.modules;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.pieter.distributed.plans.PlanGenome;
import playground.pieter.distributed.replanning.PlanCatcher;

/**
 * Created by fouriep on 11/26/14.
 */
public class RegisterMutatedPlanForPSim implements PlanStrategyModule, IterationEndsListener {
    private final char gene;
    private final boolean trackGenome;
    private int iterationNumber;

    public RegisterMutatedPlanForPSim(PlanCatcher slave, char gene, boolean trackGenome, Controler controler) {
        this.slave = slave;
        this.gene=gene;
        this.trackGenome = trackGenome;
        controler.addControlerListener(this);
    }

    private PlanCatcher slave;

    @Override
    public void prepareReplanning(ReplanningContext replanningContext) {

    }

    @Override
    public void handlePlan(Plan plan) {
        if(slave != null)
            slave.addPlansForPsim(plan);
        if(trackGenome)
            ((PlanGenome) plan).appendStrategyToGenome(String.format("%s%04d",gene, iterationNumber));
    }

    @Override
    public void finishReplanning() {

    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        iterationNumber=event.getIteration();
    }



}

