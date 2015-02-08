package playground.pieter.distributed.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import playground.pieter.distributed.replanning.PlanCatcher;
import playground.pieter.distributed.replanning.modules.RegisterMutatedPlanForPSim;
import playground.singapore.transitLocationChoice.TransitLocationChoiceStrategy;

/**
 * Created by fouriep on 11/25/14.
 */
public class TransitLocationChoiceFactory implements PlanStrategyFactory {
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
    public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
        TransitLocationChoiceStrategy strategy = new TransitLocationChoiceStrategy(scenario);
        strategy.addStrategyModule(new RegisterMutatedPlanForPSim(slave, gene,trackGenome,controler));
        return strategy;
    }
}
