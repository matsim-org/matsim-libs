/**
 *
 */
package org.matsim.contrib.freightReceiver.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freightReceiver.Receiver;
import org.matsim.contrib.freightReceiver.ReceiverPlan;
import org.matsim.contrib.freightReceiver.collaboration.TimeWindowMutatorV2;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.GenericStrategyManagerImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.utils.misc.Time;

import javax.inject.Inject;

/**
 * This class implements a receiver reorder strategy that changes the delivery time window of its orders.
 *
 * @author wlbean
 *
 */
public class TimeWindowReceiverOrderStrategyManagerImpl implements ReceiverOrderStrategyManagerFactory{

	@Inject Scenario sc;

	TimeWindowReceiverOrderStrategyManagerImpl(){
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

//		/*
//		 * Increases or decreases the time window start or time window end times.
//		 */
//
//		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> timeStrategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
//			timeStrategy.addStrategyModule(new TimeWindowMutator(Time.parseTime("01:00:00")));
//			stratMan.addStrategy(timeStrategy, null, 0.3);
//			stratMan.addChangeRequest((int) (sc.getConfig().controler().getLastIteration()*0.9), timeStrategy, null, 0.0);
//		}

		/*
		 * Increases or decreases the time window end times.
		 */

		{
//			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<ReceiverPlan, Receiver>());
			GenericPlanStrategyImpl<ReceiverPlan, Receiver> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger<>( 10.) );
			strategy.addStrategyModule(new TimeWindowMutatorV2(Time.parseTime("02:00:00")));
			stratMan.addStrategy(strategy, null, 0.3);
			stratMan.addChangeRequest((int) (sc.getConfig().controler().getLastIteration()*0.9), strategy, null, 0.0);
		}

		/* Replanning for grand coalition receivers.*/
//
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
