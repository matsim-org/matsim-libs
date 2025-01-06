package org.matsim.freight.receiver.replanning;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.receiver.Receiver;
import org.matsim.freight.receiver.ReceiverPlan;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.utils.misc.Time;

/**
 * This class implements a receiver reorder strategy that changes the delivery service time of its orders.
 *
 * @author wlbean
 */
final class ServiceTimeStrategyManagerProvider implements Provider<ReceiverStrategyManager> {

	@Inject
	Scenario sc;

	ServiceTimeStrategyManagerProvider() {
	}

	@Override
	public ReceiverStrategyManager get() {
		ReceiverStrategyManager strategyManager = new ReceiverStrategyManagerImpl();
		strategyManager.setMaxPlansPerAgent(5);

		{ /* Just choose a plan from memory. The better the past score, the higher the probability of being chosen. */
			GenericPlanStrategy<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger.Factory<ReceiverPlan, Receiver>().build());
			strategyManager.addStrategy(strategy, null, 0.70);
		}

		{
			/* For half of the time, increase the service time by 15min at a time. Here we should end up with a service time <= 4hrs.
			 * The strategy is witched off after 90% of the number of iterations. */
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> increaseStrategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger.Factory<ReceiverPlan, Receiver>().build());
			increaseStrategy.addStrategyModule(new ServiceTimeMutator(Time.parseTime("0:15:00"), Time.parseTime("04:00:00"), true));
			strategyManager.addStrategy(increaseStrategy, null, 0.15);
			strategyManager.addChangeRequest((int) (sc.getConfig().controller().getLastIteration() * 0.9), increaseStrategy, null, 0.0);
		}

		{
			/* For half of the time, decrease the service time by 15min at a time. Here we should end up with a service time >= 1hrs.
			 * The strategy is witched off after 90% of the number of iterations. */
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> decreaseStrategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger.Factory<ReceiverPlan, Receiver>().build());
			decreaseStrategy.addStrategyModule(new ServiceTimeMutator(Time.parseTime("00:15:00"), Time.parseTime("01:00:00"), false));
			strategyManager.addStrategy(decreaseStrategy, null, 0.15);
			strategyManager.addChangeRequest((int) (sc.getConfig().controller().getLastIteration() * 0.9), decreaseStrategy, null, 0.0);
		}

		/* Replanning for grand coalition receivers.*/
//		{
////			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanSelector<>( 10. ) );
//			strategy.addStrategyModule(new CollaborationStatusMutator());
//			strategyManager.addStrategy(strategy, null, 0.2);
//			strategyManager.addChangeRequest((int) Math.round((sc.getConfig().controler().getLastIteration())*0.9), strategy, null, 0.0);
//		}
//		log.error("yyyyyy the above needs to be restored again.") ;
		// yyyyyy I have switched off the coalition mutator!

		return strategyManager;
	}

}
