package org.matsim.freight.receiver.replanning;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.receiver.Receiver;
import org.matsim.freight.receiver.ReceiverPlan;
import org.matsim.freight.receiver.collaboration.OrderSizeMutator;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;

/**
 * This class implements a receiver reorder strategy that changes the number of
 * weekly deliveries preferred by receivers.
 *
 * @author wlbean, jwjoubert
 */
class OrderFrequencyStrategyManager implements Provider<ReceiverStrategyManager> {
	// never used.  kai, jan'19

	@Inject
	Scenario sc;

	OrderFrequencyStrategyManager(){
	}

	@Override
	public ReceiverStrategyManager get() {
		final ReceiverStrategyManager strategyManager = new ReceiverStrategyManagerImpl();
		strategyManager.setMaxPlansPerAgent(5);

		{
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger.Factory<ReceiverPlan, Receiver>().setBetaValue(10.0).build() );
			strategyManager.addStrategy(strategy, null, 0.7);
		}

		/*
		 * Increases the number of weekly deliveries with 1 at a time (and thereby decreasing order quantity).
		 */
		{
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger.Factory<ReceiverPlan, Receiver>().setBetaValue(10.0).build() );
			strategy.addStrategyModule(new OrderSizeMutator(true));
			strategyManager.addStrategy(strategy, null, 0.15);
			strategyManager.addChangeRequest((int) (sc.getConfig().controller().getLastIteration()*0.9), strategy, null, 0.0);
		}

		/*
		 * Decreases the number of weekly deliveries with 1 at time (and thereby increase order quantity).
		 */

		{
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger.Factory<ReceiverPlan, Receiver>().setBetaValue(10.0).build() );
			strategy.addStrategyModule(new OrderSizeMutator(false));
			strategyManager.addStrategy(strategy, null, 0.15);
			strategyManager.addChangeRequest((int) (sc.getConfig().controller().getLastIteration()*0.9), strategy, null, 0.0);
		}

	/* Replanning for grand coalition receivers. TODO Removed for now. */
//		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger.Factory<ReceiverPlan, Receiver>().setBetaValue(10.0).build() );
//			strategy.addStrategyModule(new CollaborationStatusMutator());
//			strategyManager.addStrategy(strategy, null, 0.2);
//			strategyManager.addChangeRequest((int) Math.round((sc.getConfig().controler().getLastIteration())*0.9), strategy, null, 0.0);
//		}

		return strategyManager;
	}

}
