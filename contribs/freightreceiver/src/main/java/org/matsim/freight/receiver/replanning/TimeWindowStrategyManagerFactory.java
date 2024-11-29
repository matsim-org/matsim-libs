/**
 *
 */
package org.matsim.freight.receiver.replanning;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.receiver.Receiver;
import org.matsim.freight.receiver.ReceiverPlan;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.utils.misc.Time;

/**
 * This class implements a receiver reorder strategy that changes the delivery time window of its orders.
 *
 * @author wlbean
 */
class TimeWindowStrategyManagerFactory implements Provider<ReceiverStrategyManager> {

	@Inject
	Scenario sc;

	TimeWindowStrategyManagerFactory() {
	}

	@Override
	public ReceiverStrategyManager get() {
		final ReceiverStrategyManager strategyManager = new ReceiverStrategyManagerImpl();
		strategyManager.setMaxPlansPerAgent(5);

		{
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>((new ExpBetaPlanChanger.Factory<ReceiverPlan, Receiver>()).setBetaValue(10.0).build());
			strategyManager.addStrategy(strategy, null, 0.7);
		}

		/*
		 * Increases or decreases the time window upper bound by steps of 1hr.
		 */
		{
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger.Factory<ReceiverPlan, Receiver>().setBetaValue(10.).build());
			strategy.addStrategyModule(new TimeWindowUpperBoundMutator(Time.parseTime("01:00:00")));
			strategyManager.addStrategy(strategy, null, 0.3);
			strategyManager.addChangeRequest((int) (sc.getConfig().controller().getLastIteration() * 0.9), strategy, null, 0.0);
		}

		/* Replanning for grand coalition receivers. TODO Ignored for now */
		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger<>(10.));
//			strategy.addStrategyModule(new CollaborationStatusMutator());
//			strategyManager.addStrategy(strategy, null, 0.2);
//			strategyManager.addChangeRequest((int) Math.round((sc.getConfig().controler().getLastIteration()) * 0.9), strategy, null, 0.0);
		}


		return strategyManager;
	}

}
