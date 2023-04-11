package org.matsim.contrib.freightReceiver.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freightReceiver.Receiver;
import org.matsim.contrib.freightReceiver.ReceiverPlan;
import org.matsim.contrib.freightReceiver.collaboration.OrderSizeMutator;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.GenericStrategyManagerImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;

import javax.inject.Inject;

/**
 * This class implements a receiver reorder strategy that changes the number of weekly deliveries preferred by receivers.
 *
 * @author wlbean
 *
 */
public class NumDelReceiverOrderStrategyManagerImpl implements ReceiverOrderStrategyManagerFactory{
	// never used.  kai, jan'19

	@Inject Scenario sc;

	NumDelReceiverOrderStrategyManagerImpl(){
	}

	@Override
	public GenericStrategyManager<ReceiverPlan, Receiver> createReceiverStrategyManager() {
		final GenericStrategyManager<ReceiverPlan, Receiver> stratMan = new GenericStrategyManagerImpl<>();
		stratMan.setMaxPlansPerAgent(5);

		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new BestPlanSelector<ReceiverPlan, Receiver>());
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger<>( 10.) );
//			strategy.addStrategyModule(new CollaborationStatusChanger());
			stratMan.addStrategy(strategy, null, 0.7);
//			stratMan.addChangeRequest((int) (sc.getConfig().controler().getLastIteration()*0.9), strategy, null, 0.0);

		}

		/*
		 * Increases the number of weekly deliveries with 1 at a time (and thereby decreasing order quantity).
		 */

		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger<>( 10.) );
			strategy.addStrategyModule(new OrderSizeMutator(true));
			stratMan.addStrategy(strategy, null, 0.15);
			stratMan.addChangeRequest((int) (sc.getConfig().controler().getLastIteration()*0.9), strategy, null, 0.0);
		}

		/*
		 * Decreases the number of weekly deliveries with 1 at time (and thereby increase order quantity).
		 */

		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger<>( 10.) );
			strategy.addStrategyModule(new OrderSizeMutator(false));
			stratMan.addStrategy(strategy, null, 0.15);
			stratMan.addChangeRequest((int) (sc.getConfig().controler().getLastIteration()*0.9), strategy, null, 0.0);
		}

		/* Replanning for grand coalition receivers.*/

		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger<>( 10.) );
			strategy.addStrategyModule(new CollaborationStatusMutator());
			stratMan.addStrategy(strategy, null, 0.2);
			stratMan.addChangeRequest((int) Math.round((sc.getConfig().controler().getLastIteration())*0.9), strategy, null, 0.0);
		}

		return stratMan;
	}

}
