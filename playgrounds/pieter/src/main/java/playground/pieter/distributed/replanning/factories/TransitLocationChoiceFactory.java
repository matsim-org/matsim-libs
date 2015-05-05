package playground.pieter.distributed.replanning.factories;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import playground.pieter.distributed.replanning.PlanCatcher;
import playground.pieter.distributed.replanning.modules.RegisterMutatedPlanForPSim;
import playground.singapore.transitLocationChoice.TransitLocationChoiceStrategy;

import javax.inject.Provider;

/**
 * Created by fouriep on 11/25/14.
 */
public class TransitLocationChoiceFactory implements Provider<PlanStrategy> {
    private final boolean trackGenome;
    private final Controler controler;
    PlanCatcher slave;
    char gene;

    public TransitLocationChoiceFactory(PlanCatcher slave, char gene, boolean trackGenome, Controler controler) {
        this.gene = gene;
        this.slave = slave;
        this.trackGenome = trackGenome;
        this.controler=controler;
    }

    @Override
    public PlanStrategy get() {
        TransitLocationChoiceStrategy strategy = new TransitLocationChoiceStrategy(controler.getScenario());
        strategy.addStrategyModule(new RegisterMutatedPlanForPSim(slave, gene,trackGenome,controler));
        return strategy;
    }
}
