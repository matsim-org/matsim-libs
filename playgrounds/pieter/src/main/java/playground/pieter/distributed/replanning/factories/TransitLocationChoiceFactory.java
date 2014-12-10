package playground.pieter.distributed.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import playground.pieter.distributed.replanning.PlanCatcher;
import playground.pieter.distributed.replanning.modules.RegisterMutatedPlanForPSim;
import playground.singapore.transitLocationChoice.TransitLocationChoiceStrategy;

/**
 * Created by fouriep on 11/25/14.
 */
public class TransitLocationChoiceFactory implements PlanStrategyFactory {
    PlanCatcher slave;

    public TransitLocationChoiceFactory(PlanCatcher slave) {
        this.slave = slave;
    }

    @Override
    public PlanStrategy createPlanStrategy( Scenario scenario, EventsManager eventsManager) {
        TransitLocationChoiceStrategy strategy = new TransitLocationChoiceStrategy(scenario);
        strategy.addStrategyModule(new RegisterMutatedPlanForPSim(slave));
        return strategy;
    }
}
